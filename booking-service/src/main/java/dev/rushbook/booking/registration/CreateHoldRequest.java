package dev.rushbook.booking.registration;

import jakarta.validation.constraints.NotBlank;

record CreateHoldRequest(@NotBlank String attendeeId) {}
