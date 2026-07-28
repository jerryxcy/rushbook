package dev.rushbook.booking.registration;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class HoldService {

    private final RegistrationRepository registrationRepository;

    HoldService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    HoldDecision hold(UUID eventId, String attendeeId) {
        Optional<RegistrationRepository.LockedEvent> event =
                registrationRepository.lockEvent(eventId);
        if (event.isEmpty()) {
            return new HoldDecision.EventNotFound(eventId, attendeeId);
        }
        RegistrationRepository.LockedEvent lockedEvent = event.orElseThrow();
        registrationRepository.expireHeldRegistrations(eventId);

        Optional<Registration> existingRegistration =
                registrationRepository.findHeldOrBookedRegistration(eventId, attendeeId);
        if (existingRegistration.isPresent()) {
            return new HoldDecision.ActiveRegistration(
                    existingRegistration.orElseThrow(), false);
        }

        if (registrationRepository.countOccupiedSpots(eventId) >= lockedEvent.capacity()) {
            return new HoldDecision.Rejected(
                    eventId, attendeeId, HoldRejectionReason.CAPACITY_EXHAUSTED);
        }

        Registration registration =
                registrationRepository.createHeldRegistration(
                        UUID.randomUUID(),
                        eventId,
                        attendeeId,
                        lockedEvent.holdPeriodSeconds());
        return new HoldDecision.ActiveRegistration(registration, true);
    }
}
