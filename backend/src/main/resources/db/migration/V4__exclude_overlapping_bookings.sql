-- This program adds the BK-01 double-booking guard. The exclusion
-- constraint rejects a second booking whose UTC window overlaps one
-- already stored for the same listing. PostgreSQL applies it during
-- the insert, so two transactions in flight cannot both commit.
--
-- File:   V4__exclude_overlapping_bookings.sql
-- Course: AUCSC 325
--
-- References:
-- https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-EXCLUSION
--     - Exclusion constraints
-- https://www.postgresql.org/docs/current/rangetypes.html
--     - tstzrange and the overlap operator
--

-- To compare listing equality and time overlap in one GiST index
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- To reject an overlapping window on the same listing. A booking
-- that starts when another ends is allowed. A cancelled row does
-- not hold the slot.
ALTER TABLE bookings
    ADD CONSTRAINT ex_bookings_listing_window
    EXCLUDE USING gist (
        listing_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    )
    WHERE (status <> 'CANCELLED');
