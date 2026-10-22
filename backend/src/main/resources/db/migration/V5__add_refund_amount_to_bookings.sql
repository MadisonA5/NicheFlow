-- This program adds refund_amount for BK-04. The column stays null
-- until a booking is cancelled. Existing rows are left null.
--
-- File:   V5__add_refund_amount_to_bookings.sql
-- Course: AUCSC 325
--

-- To store the refund computed when a booking is cancelled
ALTER TABLE bookings
    ADD COLUMN refund_amount NUMERIC(10, 2);
