-- This program adds the supplier-accepted state before customer
-- confirmation and payment.
--
-- File:   V7__allow_supplier_accepted_bookings.sql
-- Course: AUCSC 325
--

ALTER TABLE bookings
    DROP CONSTRAINT ck_bookings_status;

ALTER TABLE bookings
    ADD CONSTRAINT ck_bookings_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'CONFIRMED', 'COMPLETED',
                   'CANCELLED')
    );
