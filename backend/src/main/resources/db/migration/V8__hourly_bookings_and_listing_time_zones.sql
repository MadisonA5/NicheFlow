ALTER TABLE listings
    ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';

ALTER TABLE bookings
    ADD COLUMN time_zone VARCHAR(64);

UPDATE bookings booking
SET time_zone = listing.time_zone
FROM listings listing
WHERE booking.listing_id = listing.id;

ALTER TABLE bookings
    ALTER COLUMN time_zone SET NOT NULL;

UPDATE listings
SET price = ROUND(price / 24, 2);
