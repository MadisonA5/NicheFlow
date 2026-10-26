import {getSession, signOut} from '../auth/authDataSource'

async function listingRequest(listingId, method, path = '', listing) {
    const session = getSession()
    if (!session?.token || session.user.role !== 'SUPPLIER') {
        throw new Error('Sign in with a supplier account to manage listings.')
    }

    const response = await fetch(`/api/listings/${listingId}${path}`, {
        method,
        headers: {
            Authorization: `Bearer ${session.token}`,
            ...(listing ? {'Content-Type': 'application/json'} : {}),
        },
        ...(listing ? {body: JSON.stringify({
            ...listing,
            price: Number(listing.price),
        })} : {}),
    })
    const payload = await response.json().catch(() => null)

    if (response.status === 401) {
        signOut()
        const error = new Error(
            'Your sign-in session expired or was rejected. Please sign in again.',
        )
        error.status = response.status
        throw error
    }
    if (!response.ok) {
        throw new Error(payload?.detail ?? `Listing request failed (${response.status}).`)
    }
    if (!payload?.id || Number(payload.id) !== Number(listingId)) {
        throw new Error('The listing response was not in the expected format.')
    }

    return payload
}

export async function getAccount() {
    const session = getSession()
    if (!session?.token) {
        throw new Error('Sign in to view your account.')
    }

    const response = await fetch('/api/account', {
        headers: {Authorization: `Bearer ${session.token}`},
    })
    const payload = await response.json().catch(() => null)

    if (response.status === 401) {
        signOut()
        const error = new Error(
            'Your sign-in session expired or was rejected. Please sign in again.',
        )
        error.status = response.status
        throw error
    }
    if (!response.ok) {
        const error = new Error(
            payload?.detail ?? `Account request failed (${response.status}).`,
        )
        error.status = response.status
        throw error
    }
    if (!Array.isArray(payload?.listings) || !Array.isArray(payload?.bookings)) {
        throw new Error('Account response was not in the expected format.')
    }

    return payload
}

export function updateListing(listingId, listing) {
    return listingRequest(listingId, 'PUT', '', listing)
}

export function deactivateListing(listingId) {
    return listingRequest(listingId, 'POST', '/deactivate')
}
