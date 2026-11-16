import {afterEach, describe, expect, it, vi} from 'vitest'
import {createBooking, getUnavailablePeriods} from './bookingsDataSource'

describe('booking data source', () => {
    const originalTimeZone = process.env.TZ

    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
        process.env.TZ = originalTimeZone
    })

    it('creates a pending booking with the customer bearer token and UTC check-in times', async () => {
        process.env.TZ = 'UTC'
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
                startTime: '2026-11-01T15:00:00.000Z',
                endTime: '2026-11-03T11:00:00.000Z',
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

    describe("in the customer's own timezone (issue #13)", () => {
        async function sentWindow(timeZone, checkIn, checkOut) {
            process.env.TZ = timeZone
            localStorage.setItem('nicheflow.session', JSON.stringify({
                token: 'signed.jwt.token',
                user: {id: 9, role: 'CUSTOMER'},
            }))
            const fetchMock = vi.fn().mockResolvedValue({
                ok: true,
                json: async () => ({id: 15, status: 'PENDING'}),
            })
            vi.stubGlobal('fetch', fetchMock)

            await createBooking({listingId: 3, checkIn, checkOut})

            const {startTime, endTime} = JSON.parse(fetchMock.mock.calls[0][1].body)
            return {startTime, endTime}
        }

        it('starts an Edmonton stay at local check-in on the chosen day, not the day before', async () => {
            // Edmonton is UTC-7 on 2026-11-13, so 15:00 local is 22:00 UTC
            await expect(sentWindow('America/Edmonton', '2026-11-13', '2026-11-14'))
                .resolves.toEqual({
                    startTime: '2026-11-13T22:00:00.000Z',
                    endTime: '2026-11-14T18:00:00.000Z',
                })
        })

        it('starts a Tokyo stay at local check-in on the chosen day', async () => {
            // Tokyo is UTC+9, so 15:00 local is 06:00 UTC the same day
            await expect(sentWindow('Asia/Tokyo', '2026-11-13', '2026-11-14'))
                .resolves.toEqual({
                    startTime: '2026-11-13T06:00:00.000Z',
                    endTime: '2026-11-14T02:00:00.000Z',
                })
        })

        it('keeps the same local check-in hour across a daylight-saving change', async () => {
            // Edmonton leaves daylight time on 2026-11-01 (UTC-6 to UTC-7)
            await expect(sentWindow('America/Edmonton', '2026-10-31', '2026-11-02'))
                .resolves.toEqual({
                    startTime: '2026-10-31T21:00:00.000Z',
                    endTime: '2026-11-02T18:00:00.000Z',
                })
        })
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
