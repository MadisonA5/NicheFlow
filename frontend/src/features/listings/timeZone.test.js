import {describe, expect, it} from 'vitest'
import {
    formatDateTimeInZone,
    localDateTimeToInstant,
} from './timeZone'

describe('listing time zones', () => {
    it('converts a local booking time to its listing-zone instant', () => {
        expect(localDateTimeToInstant(
            '2026-07-15T12:00', 'America/Edmonton',
        ).toISOString()).toBe('2026-07-15T18:00:00.000Z')
    })

    it('rejects local times skipped by daylight saving time', () => {
        expect(() => localDateTimeToInstant(
            '2023-03-12T02:00', 'America/Edmonton',
        )).toThrow('does not exist')
    })

    it('uses the second occurrence of an ambiguous time when needed', () => {
        const firstOccurrence = localDateTimeToInstant(
            '2023-11-05T01:00', 'America/Edmonton',
        )
        const secondOccurrence = localDateTimeToInstant(
            '2023-11-05T01:00',
            'America/Edmonton',
            firstOccurrence.toISOString(),
        )

        expect(firstOccurrence.toISOString()).toBe('2023-11-05T07:00:00.000Z')
        expect(secondOccurrence.toISOString()).toBe('2023-11-05T08:00:00.000Z')
    })

    it('formats booking times using the listing zone', () => {
        expect(formatDateTimeInZone(
            '2026-07-15T18:00:00Z', 'America/Edmonton',
        )).toContain('12:00')
    })
})
