import {afterEach, describe, expect, it, vi} from 'vitest'
import {
    createReview,
    listReviewsForBooking,
    listReviewsForListing,
} from './reviewsDataSource'

describe('reviews data source', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
    })

    it('loads public reviews for a booking with no auth header', async () => {
        const reviews = [{
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 7,
            rating: 5,
            comment: 'Great stay',
            createdAt: '2026-11-03T00:00:00Z',
        }]
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => reviews,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(listReviewsForBooking('11')).resolves.toEqual(reviews)
        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/11/reviews')
    })

    it('loads public reviews for a listing with no auth header', async () => {
        const reviews = [{
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 7,
            rating: 5,
            comment: 'Great stay',
            createdAt: '2026-11-03T00:00:00Z',
        }]
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => reviews,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(listReviewsForListing('3')).resolves.toEqual(reviews)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/3/reviews')
    })

    it('rejects a malformed review list response', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => [{id: 21, rating: 9}],
        }))

        await expect(listReviewsForBooking(11))
            .rejects.toThrow('review response')
    })

    it('creates a review with the signed-in bearer token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, role: 'CUSTOMER'},
        }))
        const created = {
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 7,
            rating: 5,
            comment: 'Great stay',
            createdAt: '2026-11-03T00:00:00Z',
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => created,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(createReview(11, {rating: 5, comment: 'Great stay'}))
            .resolves.toEqual(created)

        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/11/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer customer.jwt.token',
            },
            body: JSON.stringify({rating: 5, comment: 'Great stay'}),
        })
    })

    it('rejects a review create without a session', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(createReview(11, {rating: 5, comment: 'Great stay'}))
            .rejects.toThrow('Sign in')
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('clears a rejected session and requests a fresh sign-in on 401', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, role: 'CUSTOMER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
            json: async () => ({detail: 'Sign in again'}),
        }))

        await expect(createReview(11, {rating: 5, comment: null}))
            .rejects.toMatchObject({
                message: 'Your sign-in session expired or was rejected. Please sign in again.',
                status: 401,
            })
        expect(localStorage.getItem('nicheflow.session')).toBeNull()
    })

    it('shows the API detail when a review create fails', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, role: 'CUSTOMER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 400,
            json: async () => ({
                detail: 'Reviews are only allowed after a completed booking',
            }),
        }))

        await expect(createReview(11, {rating: 5, comment: null}))
            .rejects.toThrow('Reviews are only allowed after a completed booking')
    })
})
