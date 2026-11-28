import {getSession, rejectIfUnauthorized} from '../auth/authDataSource'

async function reviewRequest(basePath, method, path = '', requireAuth = false, body) {
    const session = getSession()
    if (requireAuth && !session?.token) {
        throw new Error('Sign in to leave a review.')
    }

    const url = `${basePath}${path}`
    const headers = {
        ...(session?.token ? {Authorization: `Bearer ${session.token}`} : {}),
        ...(body ? {'Content-Type': 'application/json'} : {}),
    }

    const response = method === 'GET'
        ? await fetch(url)
        : await fetch(url, {
            method,
            headers,
            ...(body ? {body: JSON.stringify(body)} : {}),
        })

    rejectIfUnauthorized(response)

    const payload = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(payload?.detail ?? `Review request failed (${response.status}).`)
    }

    return payload
}

function isValidReviewList(payload) {
    return Array.isArray(payload) && payload.every((review) =>
        Number.isFinite(review?.id) &&
        Number.isFinite(review?.rating) &&
        review.rating >= 1 && review.rating <= 5 &&
        Number.isFinite(review?.reviewerId) &&
        Number.isFinite(review?.revieweeId))
}

export async function listReviewsForBooking(bookingId) {
    const payload = await reviewRequest(
        `/api/bookings/${Number(bookingId)}/reviews`, 'GET',
    )

    if (!isValidReviewList(payload)) {
        throw new Error('The review response was not in the expected format.')
    }

    return payload
}

export async function listReviewsForListing(listingId) {
    const payload = await reviewRequest(
        `/api/listings/${Number(listingId)}/reviews`, 'GET',
    )

    if (!isValidReviewList(payload)) {
        throw new Error('The review response was not in the expected format.')
    }

    return payload
}

export async function createReview(bookingId, {rating, comment}) {
    const payload = await reviewRequest(
        `/api/bookings/${Number(bookingId)}/reviews`, 'POST', '', true,
        {rating: Number(rating), comment},
    )

    if (!payload?.id || Number(payload.bookingId) !== Number(bookingId)) {
        throw new Error('The review response was not in the expected format.')
    }

    return payload
}
