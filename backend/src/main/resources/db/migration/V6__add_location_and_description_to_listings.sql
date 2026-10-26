-- This program adds location and description for LST-01. Both
-- columns default to an empty string so rows that already exist
-- stay valid.
--
-- File:   V6__add_location_and_description_to_listings.sql
-- Course: AUCSC 325

ALTER TABLE listings
    ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN description VARCHAR(2000) NOT NULL DEFAULT '';
