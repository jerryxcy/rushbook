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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BookingServiceTestConfiguration.class)
class ConcurrencyEvidenceApiIntegrationTest {

    private static final int REPETITIONS = 5;
    private static final int REQUEST_TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @RepeatedTest(REPETITIONS)
    void uniqueAttendeesNeverCreateMoreActiveRegistrationsThanCapacity() throws Exception {
        int capacity = 8;
        int attendees = 40;
        UUID eventId = createEvent(capacity);

        List<ObservedResponse> evidence =
                race(
                        attendees,
                        requestNumber -> {
                            String attendeeId = "capacity-contender-" + requestNumber;
                            return observe(
                                    requestNumber,
                                    attendeeId,
                                    postHold(eventId, attendeeId));
                        });

        List<ObservedResponse> created = withStatus(evidence, 201);
        List<ObservedResponse> rejected = withStatus(evidence, 409);
        Set<String> registrationIds =
                created.stream()
                        .map(response -> jsonString(response.body(), "registrationId"))
                        .collect(Collectors.toSet());

        assertThat(created)
                .as("capacity=%s; all responses:%n%s", capacity, describe(evidence))
                .hasSize(capacity);
        assertThat(rejected)
                .as("every non-winner must be rejected; all responses:%n%s", describe(evidence))
                .hasSize(attendees - capacity)
                .allSatisfy(
                        response ->
                                assertThat(response.body())
                                        .contains("\"reason\":\"CAPACITY_EXHAUSTED\""));
        assertThat(registrationIds)
                .as("each occupied Spot needs one Registration; winners:%n%s", describe(created))
                .hasSize(capacity);
        assertThat(evidence)
                .as(
                        "unexpected response means the invariant was not observed; all responses:%n%s",
                        describe(evidence))
                .allSatisfy(
                        response -> assertThat(response.statusCode()).isIn(201, 409));
    }

    @RepeatedTest(REPETITIONS)
    void duplicateConcurrentHoldsReturnOneActiveRegistration() throws Exception {
        int requests = 20;
        UUID eventId = createEvent(2);

        List<ObservedResponse> evidence =
                race(
                        requests,
                        requestNumber ->
                                observe(
                                        requestNumber,
                                        "duplicate-attendee",
                                        postHold(eventId, "duplicate-attendee")));
        Set<String> bodies =
                evidence.stream().map(ObservedResponse::body).collect(Collectors.toSet());

        assertThat(withStatus(evidence, 201))
                .as(
                        "exactly one request creates the Registration; all responses:%n%s",
                        describe(evidence))
                .hasSize(1);
        assertThat(withStatus(evidence, 200))
                .as("all retries return the existing Registration; all responses:%n%s", describe(evidence))
                .hasSize(requests - 1);
        assertThat(bodies)
                .as(
                        "duplicate Holds must expose one domain result; all responses:%n%s",
                        describe(evidence))
                .hasSize(1);
    }

    @RepeatedTest(REPETITIONS)
    void concurrentConfirmsReturnOneBooking() throws Exception {
        int requests = 20;
        UUID eventId = createEvent(1);
        HttpResponse<String> holdResponse = postHold(eventId, "confirm-attendee");
        UUID registrationId =
                UUID.fromString(jsonString(holdResponse.body(), "registrationId"));

        List<ObservedResponse> evidence =
                race(
                        requests,
                        requestNumber ->
                                observe(
                                        requestNumber,
                                        "confirm-attendee",
                                        postConfirm(registrationId)));
        Set<String> bookingIds =
                evidence.stream()
                        .map(response -> jsonString(response.body(), "bookingId"))
                        .collect(Collectors.toSet());
        Set<String> bodies =
                evidence.stream().map(ObservedResponse::body).collect(Collectors.toSet());

        assertThat(evidence)
                .as(
                        "every Confirm must observe the terminal Booking; all responses:%n%s",
                        describe(evidence))
                .allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));
        assertThat(bookingIds)
                .as(
                        "concurrent Confirms must not create a second Booking; all responses:%n%s",
                        describe(evidence))
                .hasSize(1);
        assertThat(bodies)
                .as("all callers must receive the same Booking; all responses:%n%s", describe(evidence))
                .hasSize(1);
    }

    private List<ObservedResponse> race(int requestCount, IntFunction<ObservedResponse> request)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ObservedResponse>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int requestNumber = 0; requestNumber < requestCount; requestNumber++) {
                int index = requestNumber;
                futures.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    start.await();
                                    return request.apply(index);
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("all requests must be ready before releasing the start gate")
                    .isTrue();
            start.countDown();

            List<ObservedResponse> responses = new ArrayList<>();
            for (Future<ObservedResponse> future : futures) {
                responses.add(future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return responses;
        }
    }

    private ObservedResponse observe(
            int requestNumber, String attendeeId, HttpResponse<String> response) {
        return new ObservedResponse(
                requestNumber, attendeeId, response.statusCode(), response.body());
    }

    private List<ObservedResponse> withStatus(
            List<ObservedResponse> evidence, int statusCode) {
        return evidence.stream().filter(response -> response.statusCode() == statusCode).toList();
    }

    private String describe(List<ObservedResponse> evidence) {
        return evidence.stream()
                .map(
                        response ->
                                "#%d attendee=%s status=%d body=%s"
                                        .formatted(
                                                response.requestNumber(),
                                                response.attendeeId(),
                                                response.statusCode(),
                                                response.body()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private UUID createEvent(int capacity) {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                                        {
                                          "name": "Repeatable Concurrency Evidence",
                                          "capacity": %d,
                                          "holdPeriodSeconds": 60
                                        }
                                        """
                                                .formatted(capacity)))
                        .build();

        HttpResponse<String> response = send(request);
        assertThat(response.statusCode())
                .as("create Event response: %s", response.body())
                .isEqualTo(201);
        return UUID.fromString(jsonString(response.body(), "eventId"));
    }

    private HttpResponse<String> postHold(UUID eventId, String attendeeId) {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl() + "/api/events/" + eventId + "/holds"))
                        .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        """
                                        {"attendeeId":"%s"}
                                        """
                                                .formatted(attendeeId)))
                        .build();
        return send(request);
    }

    private HttpResponse<String> postConfirm(UUID registrationId) {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        baseUrl()
                                                + "/api/registrations/"
                                                + registrationId
                                                + "/confirm"))
                        .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        return send(request);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new IllegalStateException("HTTP request failed: " + request.uri(), exception);
        }
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

    private record ObservedResponse(
            int requestNumber, String attendeeId, int statusCode, String body) {}
}
