import {getSession} from '../auth/authDataSource'

// Local wall-clock times for a stay, read in the customer's own timezone.
export const CHECK_IN_TIME = '15:00'
export const CHECK_OUT_TIME = '11:00'

// A date and time with no offset is parsed as local time, so this is
// the customer's local moment, sent to the API as a UTC instant.
function toUtcInstant(date, time) {
    return new Date(`${date}T${time}:00`).toISOString()
}

export async function getUnavailablePeriods(listingId) {
    const response = await fetch(
        `/api/bookings/availability/${Number(listingId)}`,
    )
    const payload = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(
            payload?.detail ?? `Availability request failed (${response.status}).`,
        )
    }
    if (!Array.isArray(payload) || payload.some((period) =>
        !period?.startTime || !period?.endTime ||
        !Number.isFinite(Date.parse(period.startTime)) ||
        !Number.isFinite(Date.parse(period.endTime)))) {
        throw new Error('The availability response was not in the expected format.')
    }

    return payload
}

export async function createBooking({listingId, checkIn, checkOut}) {
    const session = getSession()
    if (!session?.token) {
        throw new Error('Sign in with a customer account to request a booking.')
    }
    if (session.user.role !== 'CUSTOMER') {
        throw new Error('Only customer accounts can request bookings.')
    }

    const response = await fetch('/api/bookings', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({
            listingId: Number(listingId),
            startTime: toUtcInstant(checkIn, CHECK_IN_TIME),
            endTime: toUtcInstant(checkOut, CHECK_OUT_TIME),
        }),
    })
    const payload = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(payload?.detail ?? `Booking request failed (${response.status}).`)
    }
    if (!payload?.id || payload.status !== 'PENDING') {
        throw new Error('The booking response was not in the expected format.')
    }

    return payload
}
