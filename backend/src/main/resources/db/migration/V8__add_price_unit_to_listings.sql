-- This program adds the price unit for issue #14 hourly booking. A
-- listing is priced per DAY or per HOUR. Rows that already exist
-- default to DAY, which is how they were priced before.
--
-- File:   V8__add_price_unit_to_listings.sql
-- Course: AUCSC 325

ALTER TABLE listings
    ADD COLUMN price_unit VARCHAR(16) NOT NULL DEFAULT 'DAY',
    -- To keep the unit to the values the PriceUnit enum knows
    ADD CONSTRAINT ck_listings_price_unit
        CHECK (price_unit IN ('DAY', 'HOUR'));
