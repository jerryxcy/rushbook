package dev.rushbook.booking.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

sealed interface HoldResponse permits ActiveRegistrationResponse, RejectedHoldResponse {}

record ActiveRegistrationResponse(String outcome, RegistrationResponse registration)
        implements HoldResponse {

    static ActiveRegistrationResponse from(Registration registration) {
        return new ActiveRegistrationResponse(
                registration.status().name(),
                RegistrationResponse.from(registration));
    }
}

record RegistrationResponse(
        UUID registrationId,
        UUID eventId,
        String attendeeId,
        String status,
        OffsetDateTime expiresAt) {

    static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.registrationId(),
                registration.eventId(),
                registration.attendeeId(),
                registration.status().name(),
                registration.expiresAt());
    }
}

record RejectedHoldResponse(String outcome, String reason, UUID eventId, String attendeeId)
        implements HoldResponse {

    static RejectedHoldResponse from(HoldDecision.Rejected rejected) {
        return new RejectedHoldResponse(
                "REJECTED",
                rejected.reason().name(),
                rejected.eventId(),
                rejected.attendeeId());
    }

    static RejectedHoldResponse eventNotFound(HoldDecision.EventNotFound notFound) {
        return new RejectedHoldResponse(
                "REJECTED", "EVENT_NOT_FOUND", notFound.eventId(), notFound.attendeeId());
    }
}
