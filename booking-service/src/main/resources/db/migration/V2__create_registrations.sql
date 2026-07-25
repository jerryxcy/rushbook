CREATE TABLE booking.registrations (
    registration_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES booking.events (event_id),
    attendee_id TEXT NOT NULL CHECK (btrim(attendee_id) <> ''),
    status TEXT NOT NULL CHECK (status IN ('HELD', 'BOOKED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE UNIQUE INDEX registrations_one_held_or_booked_per_attendee
    ON booking.registrations (event_id, attendee_id)
    WHERE status IN ('HELD', 'BOOKED');

CREATE INDEX registrations_capacity_lookup
    ON booking.registrations (event_id, status, expires_at);
