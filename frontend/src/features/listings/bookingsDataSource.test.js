import {afterEach, describe, expect, it, vi} from 'vitest'
import {createBooking, getUnavailablePeriods} from './bookingsDataSource'

describe('booking data source', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
    })

    it('creates a pending booking with the customer bearer token and UTC dates', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'signed.jwt.token',
            user: {id: 9, role: 'CUSTOMER'},
        }))
        const booking = {
            id: 15,
            listingId: 3,
            status: 'PENDING',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => booking,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(createBooking({
            listingId: '3',
            checkIn: '2026-11-01',
            checkOut: '2026-11-03',
        })).resolves.toEqual(booking)

        expect(fetchMock).toHaveBeenCalledWith('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer signed.jwt.token',
            },
            body: JSON.stringify({
                listingId: 3,
                startTime: '2026-11-01T00:00:00Z',
                endTime: '2026-11-03T00:00:00Z',
            }),
        })
    })

    it('loads public unavailable booking periods for a listing', async () => {
        const periods = [{
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }]
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => periods,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(getUnavailablePeriods('3')).resolves.toEqual(periods)
        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/availability/3')
    })

    it('rejects a malformed availability response', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => [{startTime: 'not-a-date'}],
        }))

        await expect(getUnavailablePeriods(3))
            .rejects.toThrow('availability response')
    })

    it('shows the API detail when a date range conflicts', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'signed.jwt.token',
            user: {id: 9, role: 'CUSTOMER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 409,
            json: async () => ({detail: 'The requested dates are already booked'}),
        }))

        await expect(createBooking({
            listingId: 3,
            checkIn: '2026-11-01',
            checkOut: '2026-11-03',
        })).rejects.toThrow('The requested dates are already booked')
    })

    it('rejects a booking request without a signed-in session', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(createBooking({
            listingId: 3,
            checkIn: '2026-11-01',
            checkOut: '2026-11-03',
        })).rejects.toThrow('Sign in with a customer account')
        expect(fetchMock).not.toHaveBeenCalled()
    })
})
