package dev.rushbook.booking.registration;

import java.util.UUID;

sealed interface HoldDecision {

    record ActiveRegistration(Registration registration, boolean created)
            implements HoldDecision {}

    record Rejected(UUID eventId, String attendeeId, HoldRejectionReason reason)
            implements HoldDecision {}

    record EventNotFound(UUID eventId, String attendeeId) implements HoldDecision {}
}
