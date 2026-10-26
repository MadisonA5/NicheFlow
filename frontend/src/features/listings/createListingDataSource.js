import {getSession} from '../auth/authDataSource'

export async function createListing(listing) {
    const session = getSession()
    if (!session?.token || session.user.role !== 'SUPPLIER') {
        throw new Error('Sign in with a supplier account to create a listing.')
    }

    const response = await fetch('/api/listings', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({
            ...listing,
            price: Number(listing.price),
        }),
    })
    const payload = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(payload?.detail ?? `Listing creation failed (${response.status}).`)
    }
    if (!payload?.id || !payload.title) {
        throw new Error('The listing response was not in the expected format.')
    }

    return payload
}
