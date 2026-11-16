import {getSession} from '../auth/authDataSource'

const listingArtwork = {
    workspace: {icon: '🌿', color: 'sage'},
    outdoor: {icon: '🚲', color: 'peach'},
    sports: {icon: '🚲', color: 'peach'},
    events: {icon: '🍽️', color: 'lavender'},
}

function withArtwork(listing) {
    const artwork = listingArtwork[listing.category.toLowerCase()] ?? {
        icon: '✦',
        color: 'sage',
    }

    return {...listing, ...artwork}
}

export async function getListing(listingId) {
    const session = getSession()
    const response = await fetch(`/api/listings/${listingId}`, {
        headers: session
            ? {Authorization: `Bearer ${session.token}`}
            : undefined,
    })
    if (response.status === 404) {
        return null
    }
    if (!response.ok) {
        throw new Error(`Listing request failed (${response.status}).`)
    }

    return withArtwork(await response.json())
}

export async function getListings() {
    const session = getSession()
    const headers = session
        ? {Authorization: `Bearer ${session.token}`}
        : undefined
    const listings = []
    let page = 0
    let totalPages = 1

    // The backend returns 20 listings per page, so keep loading pages
    // until every active listing has been fetched.
    while (page < totalPages) {
        const url = page === 0 ? '/api/listings' : `/api/listings?page=${page}`
        const response = await fetch(url, {headers})
        if (!response.ok) {
            throw new Error(`Listings request failed (${response.status}).`)
        }

        const result = await response.json()
        if (!result || !Array.isArray(result.items)) {
            throw new Error('Listings response was not in the expected format.')
        }

        listings.push(...result.items)
        totalPages = Number.isInteger(result.totalPages) ? result.totalPages : 1
        page += 1
    }

    return listings.map(withArtwork)
}
