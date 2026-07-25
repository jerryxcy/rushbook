package dev.rushbook.booking.event;

import java.time.OffsetDateTime;
import java.util.UUID;

record EventResponse(
        UUID eventId,
        String name,
        int capacity,
        int holdPeriodSeconds,
        OffsetDateTime createdAt) {

    static EventResponse from(Event event) {
        return new EventResponse(
                event.eventId(),
                event.name(),
                event.capacity(),
                event.holdPeriodSeconds(),
                event.createdAt());
    }
}
