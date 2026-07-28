package dev.rushbook.booking.registration;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/holds")
class HoldController {

    private final HoldService holdService;

    HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    ResponseEntity<HoldResponse> hold(
            @PathVariable UUID eventId, @Valid @RequestBody CreateHoldRequest request) {
        return switch (holdService.hold(eventId, request.attendeeId())) {
            case HoldDecision.ActiveRegistration active ->
                    ResponseEntity.status(
                                    active.created() ? HttpStatus.CREATED : HttpStatus.OK)
                            .body(ActiveRegistrationResponse.from(active.registration()));
            case HoldDecision.Rejected rejected ->
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(RejectedHoldResponse.from(rejected));
            case HoldDecision.EventNotFound notFound ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(RejectedHoldResponse.eventNotFound(notFound));
        };
    }
}
