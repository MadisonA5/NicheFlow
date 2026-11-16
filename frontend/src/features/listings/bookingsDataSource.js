import {getSession} from '../auth/authDataSource'

function toUtcMidnight(date) {
    return `${date}T00:00:00Z`
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

// A daily booking sends checkIn and checkOut dates. An hourly booking
// (issue #14) sends the startTime and endTime instants it picked.
export async function createBooking({listingId, checkIn, checkOut, startTime, endTime}) {
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
            startTime: startTime ?? toUtcMidnight(checkIn),
            endTime: endTime ?? toUtcMidnight(checkOut),
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
