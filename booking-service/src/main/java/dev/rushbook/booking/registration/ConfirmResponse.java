package dev.rushbook.booking.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

sealed interface ConfirmResponse
        permits BookedConfirmationResponse, RejectedConfirmationResponse {}

record BookedConfirmationResponse(String outcome, BookingResponse booking)
        implements ConfirmResponse {

    static BookedConfirmationResponse from(Registration registration) {
        return new BookedConfirmationResponse(
                "BOOKED", BookingResponse.from(registration));
    }
}

record BookingResponse(
        UUID registrationId,
        UUID eventId,
        String attendeeId,
        String status,
        UUID bookingId,
        OffsetDateTime confirmedAt) {

    static BookingResponse from(Registration registration) {
        return new BookingResponse(
                registration.registrationId(),
                registration.eventId(),
                registration.attendeeId(),
                registration.status().name(),
                registration.bookingId(),
                registration.confirmedAt());
    }
}

record RejectedConfirmationResponse(String outcome, String reason, UUID registrationId)
        implements ConfirmResponse {

    static RejectedConfirmationResponse registrationNotFound(UUID registrationId) {
        return new RejectedConfirmationResponse(
                "REJECTED", "REGISTRATION_NOT_FOUND", registrationId);
    }

    static RejectedConfirmationResponse from(ConfirmDecision.Rejected rejected) {
        return new RejectedConfirmationResponse(
                "REJECTED",
                rejected.reason().name(),
                rejected.registrationId());
    }
}
