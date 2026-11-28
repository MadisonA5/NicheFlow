import {fireEvent, render, screen, waitFor} from '@testing-library/react'
import {Link, MemoryRouter, Route, Routes} from 'react-router-dom'
import {afterEach, describe, expect, it, vi} from 'vitest'
import ListingDetailPage from './ListingDetailPage'

describe('ListingDetailPage reviews', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
    })

    it('does not keep showing the previous listing\'s reviews while the next listing loads', async () => {
        const listingOne = {
            id: 1, title: 'Garden studio', category: 'Workspace', price: 38,
            location: 'Calgary, AB', description: 'A quiet spot.', supplierId: 1,
            active: true,
        }
        const listingTwo = {
            id: 2, title: 'Bike rental', category: 'Outdoor', price: 20,
            location: 'Calgary, AB', description: 'Two wheels.', supplierId: 1,
            active: true,
        }
        const reviewOne = {
            id: 21, bookingId: 11, reviewerId: 9, revieweeId: 1, rating: 5,
            comment: 'Loved the garden studio', createdAt: '2026-11-04T00:00:00Z',
        }

        let resolveListingTwoReviews
        const fetchMock = vi.fn((url) => {
            if (url === '/api/listings/1') {
                return Promise.resolve({ok: true, json: async () => listingOne})
            }
            if (url === '/api/listings/2') {
                return Promise.resolve({ok: true, json: async () => listingTwo})
            }
            if (url === '/api/listings/1/reviews') {
                return Promise.resolve({ok: true, json: async () => [reviewOne]})
            }
            if (url === '/api/listings/2/reviews') {
                return new Promise((resolve) => {
                    resolveListingTwoReviews = resolve
                })
            }
            return Promise.resolve({ok: true, json: async () => ({items: []})})
        })
        vi.stubGlobal('fetch', fetchMock)

        render(
            <MemoryRouter initialEntries={['/listings/1']}>
                <Link to="/listings/2">Go to listing 2</Link>
                <Routes>
                    <Route path="/listings/:listingId" element={<ListingDetailPage/>}/>
                </Routes>
            </MemoryRouter>,
        )

        expect(await screen.findByText('Loved the garden studio'))
            .toBeInTheDocument()

        fireEvent.click(screen.getByRole('link', {name: 'Go to listing 2'}))

        expect(await screen.findByRole('heading', {name: 'Bike rental'}))
            .toBeInTheDocument()
        await waitFor(() => {
            expect(screen.queryByText('Loved the garden studio'))
                .not.toBeInTheDocument()
        })

        resolveListingTwoReviews({ok: true, json: async () => []})
        expect(await screen.findByText('No reviews yet.')).toBeInTheDocument()
    })
})
