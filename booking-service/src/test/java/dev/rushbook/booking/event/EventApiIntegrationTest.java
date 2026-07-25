package dev.rushbook.booking.event;

import static org.assertj.core.api.Assertions.assertThat;

import dev.rushbook.booking.BookingServiceTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BookingServiceTestConfiguration.class)
class EventApiIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void organizerCanCreateEventWithDefaultHoldPeriod() throws Exception {
        HttpResponse<String> response =
                postEvent(
                        """
                        {
                          "name": "Kafka Summit",
                          "capacity": 10
                        }
                        """);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).contains("/api/events/"));
        assertThat(response.body())
                .contains(
                        "\"name\":\"Kafka Summit\"",
                        "\"capacity\":10",
                        "\"holdPeriodSeconds\":120");
    }

    @Test
    void createdEventCanBeRetrievedFromItsLocation() throws Exception {
        HttpResponse<String> createResponse =
                postEvent(
                        """
                        {
                          "name": "Kubernetes Workshop",
                          "capacity": 25,
                          "holdPeriodSeconds": 300
                        }
                        """);
        String location = createResponse.headers().firstValue("Location").orElseThrow();

        HttpRequest getRequest =
                HttpRequest.newBuilder()
                        .uri(URI.create(location))
                        .GET()
                        .build();
        HttpResponse<String> getResponse =
                httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.body())
                .contains(
                        "\"name\":\"Kubernetes Workshop\"",
                        "\"capacity\":25",
                        "\"holdPeriodSeconds\":300");
    }

    @ParameterizedTest
    @MethodSource("invalidEventRequests")
    void invalidCapacityAndHoldPeriodAreRejected(String requestBody) throws Exception {
        HttpResponse<String> response = postEvent(requestBody);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> postEvent(String requestBody) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/events"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static Stream<String> invalidEventRequests() {
        return Stream.of(
                """
                {"name":"No Capacity","capacity":0}
                """,
                """
                {"name":"Too Short","capacity":10,"holdPeriodSeconds":4}
                """,
                """
                {"name":"Too Long","capacity":10,"holdPeriodSeconds":901}
                """);
    }
}
