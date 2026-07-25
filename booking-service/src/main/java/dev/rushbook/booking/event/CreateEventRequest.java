package dev.rushbook.booking.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

record CreateEventRequest(
        @NotBlank String name,
        @Positive int capacity,
        @Min(5) @Max(900) Integer holdPeriodSeconds) {}
