CREATE TABLE booking.events (
    event_id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (btrim(name) <> ''),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    hold_period_seconds INTEGER NOT NULL
        DEFAULT 120
        CHECK (hold_period_seconds BETWEEN 5 AND 900),
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
