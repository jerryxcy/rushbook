package dev.rushbook.booking.registration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class RegistrationRepository {

    private final JdbcClient jdbcClient;

    RegistrationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<LockedEvent> lockEvent(UUID eventId) {
        return jdbcClient
                .sql(
                        """
                        SELECT
                            capacity,
                            hold_period_seconds
                        FROM booking.events
                        WHERE event_id = :eventId
                        FOR UPDATE
                        """)
                .param("eventId", eventId)
                .query(
                        (resultSet, rowNumber) ->
                                new LockedEvent(
                                        resultSet.getInt("capacity"),
                                        resultSet.getInt("hold_period_seconds")))
                .optional();
    }

    boolean hasHeldOrBookedRegistration(UUID eventId, String attendeeId) {
        int matchingRegistrations =
                jdbcClient
                        .sql(
                                """
                                SELECT count(*)
                                FROM booking.registrations
                                WHERE event_id = :eventId
                                  AND attendee_id = :attendeeId
                                  AND status IN ('HELD', 'BOOKED')
                                """)
                        .param("eventId", eventId)
                        .param("attendeeId", attendeeId)
                        .query(Integer.class)
                        .single();
        return matchingRegistrations > 0;
    }

    int countOccupiedSpots(UUID eventId) {
        return jdbcClient
                .sql(
                        """
                        SELECT count(*)
                        FROM booking.registrations
                        WHERE event_id = :eventId
                          AND (
                              status = 'BOOKED'
                              OR (
                                  status = 'HELD'
                                  AND expires_at > clock_timestamp()
                              )
                          )
                        """)
                .param("eventId", eventId)
                .query(Integer.class)
                .single();
    }

    Registration createHeldRegistration(
            UUID registrationId, UUID eventId, String attendeeId, int holdPeriodSeconds) {
        return jdbcClient
                .sql(
                        """
                        INSERT INTO booking.registrations (
                            registration_id,
                            event_id,
                            attendee_id,
                            status,
                            expires_at
                        )
                        VALUES (
                            :registrationId,
                            :eventId,
                            :attendeeId,
                            'HELD',
                            clock_timestamp()
                                + make_interval(secs => :holdPeriodSeconds)
                        )
                        RETURNING
                            registration_id,
                            event_id,
                            attendee_id,
                            status,
                            expires_at,
                            created_at
                        """)
                .param("registrationId", registrationId)
                .param("eventId", eventId)
                .param("attendeeId", attendeeId)
                .param("holdPeriodSeconds", holdPeriodSeconds)
                .query(RegistrationRepository::mapRegistration)
                .single();
    }

    private static Registration mapRegistration(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new Registration(
                resultSet.getObject("registration_id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("attendee_id"),
                RegistrationStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("expires_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    record LockedEvent(int capacity, int holdPeriodSeconds) {}
}
