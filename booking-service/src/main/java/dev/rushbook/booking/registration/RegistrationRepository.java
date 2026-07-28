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

    Optional<Registration> findHeldOrBookedRegistration(UUID eventId, String attendeeId) {
        return jdbcClient
                .sql(
                        """
                        SELECT
                            registration_id,
                            event_id,
                            attendee_id,
                            status,
                            expires_at,
                            created_at,
                            booking_id,
                            confirmed_at
                        FROM booking.registrations
                        WHERE event_id = :eventId
                          AND attendee_id = :attendeeId
                          AND status IN ('HELD', 'BOOKED')
                        """)
                .param("eventId", eventId)
                .param("attendeeId", attendeeId)
                .query(RegistrationRepository::mapRegistration)
                .optional();
    }

    void expireHeldRegistrations(UUID eventId) {
        jdbcClient
                .sql(
                        """
                        UPDATE booking.registrations
                        SET status = 'EXPIRED'
                        WHERE event_id = :eventId
                          AND status = 'HELD'
                          AND expires_at <= clock_timestamp()
                        """)
                .param("eventId", eventId)
                .update();
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
                            created_at,
                            booking_id,
                            confirmed_at
                        """)
                .param("registrationId", registrationId)
                .param("eventId", eventId)
                .param("attendeeId", attendeeId)
                .param("holdPeriodSeconds", holdPeriodSeconds)
                .query(RegistrationRepository::mapRegistration)
                .single();
    }

    Optional<Registration> lockRegistration(UUID registrationId) {
        return jdbcClient
                .sql(
                        """
                        SELECT
                            registration_id,
                            event_id,
                            attendee_id,
                            status,
                            expires_at,
                            created_at,
                            booking_id,
                            confirmed_at
                        FROM booking.registrations
                        WHERE registration_id = :registrationId
                        FOR UPDATE
                        """)
                .param("registrationId", registrationId)
                .query(RegistrationRepository::mapRegistration)
                .optional();
    }

    Optional<Registration> confirmHeldRegistration(UUID registrationId, UUID bookingId) {
        return jdbcClient
                .sql(
                        """
                        WITH confirmation AS MATERIALIZED (
                            SELECT clock_timestamp() AS confirmed_at
                        )
                        UPDATE booking.registrations AS registration
                        SET
                            status = 'BOOKED',
                            booking_id = :bookingId,
                            confirmed_at = confirmation.confirmed_at
                        FROM confirmation
                        WHERE registration.registration_id = :registrationId
                          AND registration.status = 'HELD'
                          AND registration.expires_at > confirmation.confirmed_at
                        RETURNING
                            registration.registration_id,
                            registration.event_id,
                            registration.attendee_id,
                            registration.status,
                            registration.expires_at,
                            registration.created_at,
                            registration.booking_id,
                            registration.confirmed_at
                        """)
                .param("registrationId", registrationId)
                .param("bookingId", bookingId)
                .query(RegistrationRepository::mapRegistration)
                .optional();
    }

    void expireHeldRegistration(UUID registrationId) {
        jdbcClient
                .sql(
                        """
                        UPDATE booking.registrations
                        SET status = 'EXPIRED'
                        WHERE registration_id = :registrationId
                          AND status = 'HELD'
                        """)
                .param("registrationId", registrationId)
                .update();
    }

    private static Registration mapRegistration(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new Registration(
                resultSet.getObject("registration_id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("attendee_id"),
                RegistrationStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("expires_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("confirmed_at", OffsetDateTime.class));
    }

    record LockedEvent(int capacity, int holdPeriodSeconds) {}
}
