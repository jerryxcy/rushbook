package dev.rushbook.booking.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class EventService {

    static final int DEFAULT_HOLD_PERIOD_SECONDS = 120;

    private final EventRepository eventRepository;

    EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    Event create(String name, int capacity, Integer holdPeriodSeconds) {
        int effectiveHoldPeriodSeconds =
                holdPeriodSeconds == null ? DEFAULT_HOLD_PERIOD_SECONDS : holdPeriodSeconds;
        return eventRepository.create(
                UUID.randomUUID(), name, capacity, effectiveHoldPeriodSeconds);
    }

    Optional<Event> findById(UUID eventId) {
        return eventRepository.findById(eventId);
    }
}
