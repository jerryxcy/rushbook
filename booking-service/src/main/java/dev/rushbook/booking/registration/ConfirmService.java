package dev.rushbook.booking.registration;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConfirmService {

    private final RegistrationRepository registrationRepository;

    ConfirmService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    ConfirmDecision confirm(UUID registrationId) {
        Optional<Registration> registration =
                registrationRepository.lockRegistration(registrationId);
        if (registration.isEmpty()) {
            return new ConfirmDecision.RegistrationNotFound(registrationId);
        }
        Registration lockedRegistration = registration.orElseThrow();
        if (lockedRegistration.status() == RegistrationStatus.BOOKED) {
            return new ConfirmDecision.Booked(lockedRegistration);
        }
        if (lockedRegistration.status() == RegistrationStatus.EXPIRED) {
            return new ConfirmDecision.Rejected(
                    registrationId, ConfirmRejectionReason.HOLD_EXPIRED);
        }

        Optional<Registration> bookedRegistration =
                registrationRepository.confirmHeldRegistration(
                        registrationId, UUID.randomUUID());
        if (bookedRegistration.isPresent()) {
            return new ConfirmDecision.Booked(bookedRegistration.orElseThrow());
        }
        registrationRepository.expireHeldRegistration(registrationId);
        return new ConfirmDecision.Rejected(
                registrationId, ConfirmRejectionReason.HOLD_EXPIRED);
    }
}
