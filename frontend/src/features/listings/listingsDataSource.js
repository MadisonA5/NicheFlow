import {getSession} from '../auth/authDataSource'

const listingArtwork = {
    workspace: {icon: '🌿', color: 'sage'},
    outdoor: {icon: '🚲', color: 'peach'},
    sports: {icon: '🚲', color: 'peach'},
    events: {icon: '🍽️', color: 'lavender'},
}

export async function getListings() {
    const session = getSession()
    const response = await fetch('/api/listings', {
        headers: session
            ? {Authorization: `Bearer ${session.token}`}
            : undefined,
    })
    if (!response.ok) {
        throw new Error(`Listings request failed (${response.status}).`)
    }

    const page = await response.json()
    if (!page || !Array.isArray(page.items)) {
        throw new Error('Listings response was not in the expected format.')
    }

    return page.items.map((listing) => {
        const artwork = listingArtwork[listing.category.toLowerCase()] ?? {
            icon: '✦',
            color: 'sage',
        }

        return {...listing, ...artwork}
    })
}
