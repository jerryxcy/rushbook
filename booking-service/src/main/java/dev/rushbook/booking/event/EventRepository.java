package dev.rushbook.booking.event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class EventRepository {

    private final JdbcClient jdbcClient;

    EventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Event create(UUID eventId, String name, int capacity, int holdPeriodSeconds) {
        return jdbcClient
                .sql(
                        """
                        INSERT INTO booking.events (
                            event_id,
                            name,
                            capacity,
                            hold_period_seconds
                        )
                        VALUES (
                            :eventId,
                            :name,
                            :capacity,
                            :holdPeriodSeconds
                        )
                        RETURNING
                            event_id,
                            name,
                            capacity,
                            hold_period_seconds,
                            created_at
                        """)
                .param("eventId", eventId)
                .param("name", name)
                .param("capacity", capacity)
                .param("holdPeriodSeconds", holdPeriodSeconds)
                .query(EventRepository::mapEvent)
                .single();
    }

    Optional<Event> findById(UUID eventId) {
        return jdbcClient
                .sql(
                        """
                        SELECT
                            event_id,
                            name,
                            capacity,
                            hold_period_seconds,
                            created_at
                        FROM booking.events
                        WHERE event_id = :eventId
                        """)
                .param("eventId", eventId)
                .query(EventRepository::mapEvent)
                .optional();
    }

    private static Event mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Event(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getInt("capacity"),
                resultSet.getInt("hold_period_seconds"),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }
}
