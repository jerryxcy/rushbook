ALTER TABLE booking.registrations
    ADD COLUMN booking_id UUID,
    ADD COLUMN confirmed_at TIMESTAMPTZ;

ALTER TABLE booking.registrations
    ADD CONSTRAINT registrations_lifecycle_columns
    CHECK (
        (
            status = 'HELD'
            AND booking_id IS NULL
            AND confirmed_at IS NULL
        )
        OR (
            status = 'BOOKED'
            AND booking_id IS NOT NULL
            AND confirmed_at IS NOT NULL
            AND confirmed_at < expires_at
        )
        OR (
            status = 'EXPIRED'
            AND booking_id IS NULL
            AND confirmed_at IS NULL
        )
    );

CREATE UNIQUE INDEX registrations_booking_id_unique
    ON booking.registrations (booking_id)
    WHERE booking_id IS NOT NULL;
