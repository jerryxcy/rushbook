package dev.rushbook.booking.registration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.rushbook.booking.BookingServiceTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BookingServiceTestConfiguration.class)
class HoldApiIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void attendeeCanHoldSpotUntilDatabaseTimeExpiration() throws Exception {
        UUID eventId = createEvent(10);

        HttpResponse<String> response = postHold(eventId, "attendee-001");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains(
                        "\"outcome\":\"HELD\"",
                        "\"attendeeId\":\"attendee-001\"",
                        "\"eventId\":\"" + eventId + "\"",
                        "\"status\":\"HELD\"",
                        "\"expiresAt\":");
    }

    @Test
    void exhaustedCapacityReturnsStableRejectedResult() throws Exception {
        UUID eventId = createEvent(1);
        assertThat(postHold(eventId, "winner").statusCode()).isEqualTo(201);

        HttpResponse<String> firstRejection = postHold(eventId, "rejected-attendee");
        HttpResponse<String> repeatedRejection = postHold(eventId, "rejected-attendee");

        assertThat(firstRejection.statusCode()).isEqualTo(409);
        assertThat(repeatedRejection.statusCode()).isEqualTo(409);
        assertThat(firstRejection.body())
                .contains(
                        "\"outcome\":\"REJECTED\"",
                        "\"reason\":\"CAPACITY_EXHAUSTED\"",
                        "\"eventId\":\"" + eventId + "\"",
                        "\"attendeeId\":\"rejected-attendee\"");
        assertThat(repeatedRejection.body()).isEqualTo(firstRejection.body());
    }

    @Test
    void repeatedHoldReturnsExistingActiveRegistration() throws Exception {
        UUID eventId = createEvent(2);
        HttpResponse<String> firstHold = postHold(eventId, "same-attendee");

        HttpResponse<String> duplicate = postHold(eventId, "same-attendee");

        assertThat(firstHold.statusCode()).isEqualTo(201);
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(duplicate.body()).isEqualTo(firstHold.body());
    }

    @RepeatedTest(3)
    void oneHundredAttendeesCompetingForTenSpotsCreateExactlyTenHolds() throws Exception {
        UUID eventId = createEvent(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int attendeeNumber = 0; attendeeNumber < 100; attendeeNumber++) {
                String attendeeId = "contender-" + attendeeNumber;
                futures.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    return postHold(eventId, attendeeId);
                                }));
            }

            start.countDown();
            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(responses).filteredOn(response -> response.statusCode() == 201).hasSize(10);
            assertThat(responses).filteredOn(response -> response.statusCode() == 409).hasSize(90);
            assertThat(responses)
                    .filteredOn(response -> response.statusCode() == 409)
                    .allSatisfy(
                            response ->
                                    assertThat(response.body())
                                            .contains("\"reason\":\"CAPACITY_EXHAUSTED\""));
        }
    }

    @Test
    void blankAttendeeIdIsRejected() throws Exception {
        UUID eventId = createEvent(1);

        HttpResponse<String> response = postHold(eventId, " ");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void missingEventReturnsStableNotFoundResult() throws Exception {
        UUID eventId = UUID.randomUUID();

        HttpResponse<String> response = postHold(eventId, "attendee-404");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains(
                        "\"outcome\":\"REJECTED\"",
                        "\"reason\":\"EVENT_NOT_FOUND\"",
                        "\"eventId\":\"" + eventId + "\"");
    }

    private UUID createEvent(int capacity) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                                        {
                                          "name": "Concurrency Lab",
                                          "capacity": %d
                                        }
                                        """
                                                .formatted(capacity)))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        String location = response.headers().firstValue("Location").orElseThrow();
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private HttpResponse<String> postHold(UUID eventId, String attendeeId) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events/" + eventId + "/holds"))
                        .timeout(Duration.ofSeconds(20))
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

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }
}
