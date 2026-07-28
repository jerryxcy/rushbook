package dev.rushbook.booking.registration;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations/{registrationId}/confirm")
class ConfirmController {

    private final ConfirmService confirmService;

    ConfirmController(ConfirmService confirmService) {
        this.confirmService = confirmService;
    }

    @PostMapping
    ResponseEntity<ConfirmResponse> confirm(@PathVariable UUID registrationId) {
        return switch (confirmService.confirm(registrationId)) {
            case ConfirmDecision.Booked booked ->
                    ResponseEntity.ok(
                            BookedConfirmationResponse.from(booked.registration()));
            case ConfirmDecision.Rejected rejected ->
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(RejectedConfirmationResponse.from(rejected));
            case ConfirmDecision.RegistrationNotFound notFound ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(
                                    RejectedConfirmationResponse.registrationNotFound(
                                            notFound.registrationId()));
        };
    }
}
