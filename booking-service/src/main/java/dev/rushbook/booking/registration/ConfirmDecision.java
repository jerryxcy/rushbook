package dev.rushbook.booking.registration;

import java.util.UUID;

sealed interface ConfirmDecision {

    record Booked(Registration registration) implements ConfirmDecision {}

    record Rejected(UUID registrationId, ConfirmRejectionReason reason)
            implements ConfirmDecision {}

    record RegistrationNotFound(UUID registrationId) implements ConfirmDecision {}
}
