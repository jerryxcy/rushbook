package dev.rushbook.booking.event;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/events")
class EventController {

    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        Event event =
                eventService.create(
                        request.name(), request.capacity(), request.holdPeriodSeconds());
        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{eventId}")
                        .buildAndExpand(event.eventId())
                        .toUri();

        return ResponseEntity.created(location).body(EventResponse.from(event));
    }

    @GetMapping("/{eventId}")
    ResponseEntity<EventResponse> get(@PathVariable UUID eventId) {
        return ResponseEntity.of(eventService.findById(eventId).map(EventResponse::from));
    }
}
