package dev.rushbook.booking.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

record Registration(
        UUID registrationId,
        UUID eventId,
        String attendeeId,
        RegistrationStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt) {}
