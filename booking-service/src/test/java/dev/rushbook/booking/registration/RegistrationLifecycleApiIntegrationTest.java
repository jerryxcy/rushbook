package dev.rushbook.booking.registration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.rushbook.booking.BookingServiceTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BookingServiceTestConfiguration.class)
class RegistrationLifecycleApiIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private int port;

    @Test
    void attendeeCanConfirmHeldRegistrationBeforeExpiration() throws Exception {
        UUID eventId = createEvent(1, 30);
        HttpResponse<String> holdResponse = postHold(eventId, "attendee-confirm");
        UUID registrationId =
                UUID.fromString(jsonString(holdResponse.body(), "registrationId"));

        HttpResponse<String> confirmResponse = postConfirm(registrationId);

        assertThat(confirmResponse.statusCode()).isEqualTo(200);
        assertThat(confirmResponse.body())
                .contains(
                        "\"outcome\":\"BOOKED\"",
                        "\"registrationId\":\"" + registrationId + "\"",
                        "\"eventId\":\"" + eventId + "\"",
                        "\"attendeeId\":\"attendee-confirm\"",
                        "\"status\":\"BOOKED\"",
                        "\"bookingId\":",
                        "\"confirmedAt\":");
        OffsetDateTime confirmedAt =
                OffsetDateTime.parse(jsonString(confirmResponse.body(), "confirmedAt"));
        OffsetDateTime expiresAt =
                OffsetDateTime.parse(jsonString(holdResponse.body(), "expiresAt"));
        assertThat(confirmedAt).isBefore(expiresAt);
    }

    @Test
    void repeatedConfirmReturnsTheSameBooking() throws Exception {
        UUID eventId = createEvent(1, 30);
        UUID registrationId =
                UUID.fromString(
                        jsonString(
                                postHold(eventId, "attendee-retry").body(),
                                "registrationId"));

        HttpResponse<String> firstConfirm = postConfirm(registrationId);
        HttpResponse<String> repeatedConfirm = postConfirm(registrationId);

        assertThat(firstConfirm.statusCode()).isEqualTo(200);
        assertThat(repeatedConfirm.statusCode()).isEqualTo(200);
        assertThat(repeatedConfirm.body()).isEqualTo(firstConfirm.body());
        assertThat(jsonString(repeatedConfirm.body(), "bookingId"))
                .isEqualTo(jsonString(firstConfirm.body(), "bookingId"));
    }

    @Test
    void confirmAfterDatabaseTimeExpirationIsRejected() throws Exception {
        UUID eventId = createEvent(1, 5);
        UUID registrationId =
                UUID.fromString(
                        jsonString(
                                postHold(eventId, "attendee-expired").body(),
                                "registrationId"));
        Thread.sleep(Duration.ofMillis(5_200));

        HttpResponse<String> confirmResponse = postConfirm(registrationId);

        assertThat(confirmResponse.statusCode()).isEqualTo(409);
        assertThat(confirmResponse.body())
                .contains(
                        "\"outcome\":\"REJECTED\"",
                        "\"reason\":\"HOLD_EXPIRED\"",
                        "\"registrationId\":\"" + registrationId + "\"");
    }

    @Test
    void attendeeCanRequestNewHoldAfterPreviousHoldExpires() throws Exception {
        UUID eventId = createEvent(1, 5);
        HttpResponse<String> firstHold = postHold(eventId, "attendee-again");
        UUID firstRegistrationId =
                UUID.fromString(jsonString(firstHold.body(), "registrationId"));
        Thread.sleep(Duration.ofMillis(5_200));

        HttpResponse<String> secondHold = postHold(eventId, "attendee-again");

        assertThat(secondHold.statusCode()).isEqualTo(201);
        assertThat(secondHold.body())
                .contains(
                        "\"outcome\":\"HELD\"",
                        "\"attendeeId\":\"attendee-again\"");
        assertThat(jsonString(secondHold.body(), "registrationId"))
                .isNotEqualTo(firstRegistrationId.toString());
    }

    @Test
    void duplicateConcurrentHoldRequestsReturnOneRegistration() throws Exception {
        UUID eventId = createEvent(2, 30);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int requestNumber = 0; requestNumber < 20; requestNumber++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    return postHold(eventId, "same-attendee");
                                }));
            }

            start.countDown();
            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(responses)
                    .filteredOn(response -> response.statusCode() == 201)
                    .hasSize(1);
            assertThat(responses)
                    .filteredOn(response -> response.statusCode() == 200)
                    .hasSize(19);
            assertThat(responses)
                    .extracting(HttpResponse::body)
                    .containsOnly(responses.getFirst().body());
        }
    }

    @Test
    void confirmThatExpiresWhileWaitingForRowLockIsRejected() throws Exception {
        UUID eventId = createEvent(1, 5);
        UUID registrationId =
                UUID.fromString(
                        jsonString(
                                postHold(eventId, "attendee-lock-wait").body(),
                                "registrationId"));
        CompletableFuture<HttpResponse<String>> pendingConfirm;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement =
                            connection.prepareStatement(
                                    """
                                    SELECT registration_id
                                    FROM booking.registrations
                                    WHERE registration_id = ?
                                    FOR UPDATE
                                    """)) {
                statement.setObject(1, registrationId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                }
            }

            pendingConfirm =
                    CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    return postConfirm(registrationId);
                                } catch (Exception exception) {
                                    throw new CompletionException(exception);
                                }
                            });
            Thread.sleep(Duration.ofMillis(5_200));
            connection.commit();
        }

        HttpResponse<String> confirmResponse =
                pendingConfirm.get(10, TimeUnit.SECONDS);
        assertThat(confirmResponse.statusCode()).isEqualTo(409);
        assertThat(confirmResponse.body())
                .contains(
                        "\"reason\":\"HOLD_EXPIRED\"",
                        "\"registrationId\":\"" + registrationId + "\"");
    }

    private UUID createEvent(int capacity, int holdPeriodSeconds) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                                        {
                                          "name": "Lifecycle Lab",
                                          "capacity": %d,
                                          "holdPeriodSeconds": %d
                                        }
                                        """
                                                .formatted(capacity, holdPeriodSeconds)))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(jsonString(response.body(), "eventId"));
    }

    private HttpResponse<String> postHold(UUID eventId, String attendeeId) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events/" + eventId + "/holds"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                                        {"attendeeId":"%s"}
                                        """
                                                .formatted(attendeeId)))
                        .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postConfirm(UUID registrationId) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        baseUrl()
                                                + "/api/registrations/"
                                                + registrationId
                                                + "/confirm"))
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String jsonString(String json, String fieldName) {
        Matcher matcher =
                Pattern.compile("\"" + Pattern.quote(fieldName) + "\":\"([^\"]+)\"")
                        .matcher(json);
        assertThat(matcher.find()).as("JSON field %s in %s", fieldName, json).isTrue();
        return matcher.group(1);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
