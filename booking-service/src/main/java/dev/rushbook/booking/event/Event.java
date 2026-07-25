package dev.rushbook.booking.event;

import java.time.OffsetDateTime;
import java.util.UUID;

record Event(
        UUID eventId,
        String name,
        int capacity,
        int holdPeriodSeconds,
        OffsetDateTime createdAt) {}
