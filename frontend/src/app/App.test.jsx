import {fireEvent, render, screen, within} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import App from './App.jsx'
import {listings} from '../features/listings/listings'

// The detail and booking pages load one listing from /api/listings/{id}.
function singleListingResponse(url) {
    const match = String(url).match(/^\/api\/listings\/(\d+)$/)
    if (!match) return null
    const listing = listings.find((item) => String(item.id) === match[1])
    return Promise.resolve(listing
        ? {ok: true, status: 200, json: async () => ({...listing, active: true})}
        : {ok: false, status: 404, json: async () => ({})})
}

describe('NicheFlow landing page', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.stubGlobal('fetch', vi.fn((url) => singleListingResponse(url) ?? Promise.resolve({
            ok: true,
            json: async () => ({
                items: listings.map(({
                                         id,
                                         title,
                                         category,
                                         price,
                                         location,
                                         description,
                                     }) => ({
                    id,
                    title,
                    category,
                    price,
                    location,
                    description,
                    supplierId: 1,
                    active: true,
                })),
            }),
        })))
    })

    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
    })

    it('shows the welcome message and labeled search fields', () => {
        render(<App/>)

        expect(
            screen.getByRole('heading', {name: 'Find your next favorite place.'}),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
        ).toBeInTheDocument()
        expect(screen.getByRole('textbox', {name: 'Where?'})).toBeInTheDocument()
    })

    it('shows all three local listings with their locations and prices', async () => {
        render(<App/>)

        const listings = await screen.findAllByRole('article')
        expect(listings).toHaveLength(3)

        expect(
            within(listings[0]).getByRole('heading', {
                name: 'Sunny garden studio',
            }),
        ).toBeInTheDocument()
        expect(within(listings[0]).getByText('$38')).toBeInTheDocument()
        expect(within(listings[0]).getByText('Calgary, AB')).toBeInTheDocument()

        expect(
            within(listings[1]).getByRole('heading', {
                name: 'Weekend bike rental',
            }),
        ).toBeInTheDocument()
        expect(
            within(listings[2]).getByRole('heading', {
                name: 'Backyard dinner spot',
            }),
        ).toBeInTheDocument()
        expect(within(listings[2]).getByText('Airdrie, AB')).toBeInTheDocument()
        expect(within(listings[2]).getByText('$55')).toBeInTheDocument()
    })

    it('shows the listings API status to help diagnose connection failures', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
        }))
        render(<App/>)

        expect(await screen.findByRole('alert'))
            .toHaveTextContent('Listings request failed (401).')
    })

    it('filters listings by keyword without case sensitivity', async () => {
        render(<App/>)

        fireEvent.change(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
            {target: {value: 'BIKE'}},
        )
        fireEvent.click(screen.getByRole('button', {name: 'Search'}))

        expect(await screen.findAllByRole('article')).toHaveLength(1)
        expect(
            screen.getByRole('heading', {name: 'Weekend bike rental'}),
        ).toBeInTheDocument()
        expect(screen.getByText('1 listing found')).toBeInTheDocument()
    })

    it('finds a listing that is on the second page of results', async () => {
        // 21 active listings, served 20 per page like the real backend
        const pagedListings = Array.from({length: 21}, (_, index) => ({
            id: index + 1,
            title: `Test Listing ${index + 1}`,
            category: 'Workspace',
            price: 10,
            location: 'Camrose',
            description: `Boundary test ${index + 1}`,
            supplierId: 1,
            active: true,
        }))
        vi.stubGlobal('fetch', vi.fn((url) => {
            const page = Number(
                new URL(url, 'http://localhost').searchParams.get('page') ?? 0)
            return Promise.resolve({
                ok: true,
                json: async () => ({
                    items: pagedListings.slice(page * 20, (page + 1) * 20),
                    page,
                    size: 20,
                    totalElements: 21,
                    totalPages: 2,
                }),
            })
        }))
        render(<App/>)

        fireEvent.change(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
            {target: {value: 'Test Listing 21'}},
        )
        fireEvent.click(screen.getByRole('button', {name: 'Search'}))

        expect(
            await screen.findByRole('heading', {name: 'Test Listing 21'}),
        ).toBeInTheDocument()
        expect(screen.getByText('1 listing found')).toBeInTheDocument()
    })

    it('filters by location and matches it together with the keyword', async () => {
        render(<App/>)

        fireEvent.change(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
            {target: {value: 'dinner'}},
        )
        fireEvent.change(screen.getByRole('textbox', {name: 'Where?'}), {
            target: {value: 'airdrie'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Search'}))

        expect(await screen.findAllByRole('article')).toHaveLength(1)
        expect(screen.getByRole('heading', {name: 'Backyard dinner spot'}))
            .toBeInTheDocument()
    })

    it('shows an empty-results message when nothing matches', async () => {
        render(<App/>)

        fireEvent.change(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
            {target: {value: 'spaceship'}},
        )
        fireEvent.click(screen.getByRole('button', {name: 'Search'}))

        expect(await screen.findByText('0 listings found')).toBeInTheDocument()
        expect(screen.queryByRole('article')).not.toBeInTheDocument()
        expect(screen.getByRole('status')).toHaveTextContent(
            'No listings match your search. Try different keywords or location.',
        )
    })

    it('filters by category and maximum price', async () => {
        render(<App/>)

        await screen.findByRole('option', {name: 'Outdoor'})
        fireEvent.change(screen.getByRole('combobox', {name: 'Category'}), {
            target: {value: 'Outdoor'},
        })
        fireEvent.change(screen.getByRole('spinbutton', {name: 'Maximum price per day'}), {
            target: {value: '30'},
        })

        expect(await screen.findAllByRole('article')).toHaveLength(1)
        expect(screen.getByRole('heading', {name: 'Weekend bike rental'})).toBeInTheDocument()
        expect(screen.getByText(/Outdoor.*Up to \$30/)).toBeInTheDocument()
    })

    it('sorts results by price and clears all search and filter criteria', async () => {
        render(<App/>)

        fireEvent.change(screen.getByRole('combobox', {name: 'Sort by'}), {
            target: {value: 'price-ascending'},
        })
        await screen.findByRole('heading', {name: 'Weekend bike rental'})
        expect(screen.getAllByRole('article')[0]).toContainElement(
            screen.getByRole('heading', {name: 'Weekend bike rental'}),
        )

        fireEvent.change(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
            {target: {value: 'garden'}},
        )
        fireEvent.click(screen.getByRole('button', {name: 'Search'}))
        expect(await screen.findAllByRole('article')).toHaveLength(1)

        fireEvent.click(screen.getByRole('button', {name: 'Clear filters'}))

        expect(await screen.findAllByRole('article')).toHaveLength(3)
        expect(screen.getByRole('searchbox', {name: 'What are you looking for?'})).toHaveValue('')
        expect(screen.getByRole('combobox', {name: 'Sort by'})).toHaveValue('featured')
        expect(screen.queryByText(/Showing results for:/)).not.toBeInTheDocument()
        expect(screen.queryByText(/listings found/)).not.toBeInTheDocument()
    })

    it('opens the sign-in page and navigates to account creation', async () => {
        render(<App/>)

        fireEvent.click(screen.getByRole('link', {name: 'Sign in'}))

        expect(
            await screen.findByRole('heading', {name: 'Welcome back'}),
        ).toBeInTheDocument()
        expect(screen.getByRole('textbox', {name: 'Email'})).toBeRequired()
        expect(screen.getByLabelText('Password')).toBeRequired()
        expect(screen.queryByText(/account access isn’t connected yet/)).not.toBeInTheDocument()

        fireEvent.click(screen.getByRole('link', {name: 'Create an account'}))

        expect(
            await screen.findByRole('heading', {name: 'Create your account'}),
        ).toBeInTheDocument()
        expect(screen.getByRole('textbox', {name: 'Email'})).toBeRequired()
        expect(screen.getByRole('combobox', {name: 'Account type'}))
            .toHaveValue('CUSTOMER')
        expect(screen.getByRole('option', {name: 'Supplier — create listings'}))
            .toBeInTheDocument()
        expect(screen.getByText(
            'Choose customer to book listings or supplier to create and manage listings.',
        ))
            .toBeInTheDocument()
    })

    it('registers the selected supplier account type', async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({items: []}),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    id: 10,
                    email: 'maker@example.com',
                    role: 'SUPPLIER',
                }),
            })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(screen.getByRole('link', {name: 'Sign in'}))
        fireEvent.click(await screen.findByRole('link', {name: 'Create an account'}))
        fireEvent.change(await screen.findByRole('textbox', {name: 'Email'}), {
            target: {value: 'maker@example.com'},
        })
        fireEvent.change(screen.getByLabelText('Password'), {
            target: {value: 'correct-horse'},
        })
        fireEvent.change(screen.getByRole('combobox', {name: 'Account type'}), {
            target: {value: 'SUPPLIER'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Create account'}))

        expect(await screen.findByRole('status')).toHaveTextContent(
            'Your supplier account is ready. You can now sign in.',
        )
        expect(fetchMock).toHaveBeenLastCalledWith('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                email: 'maker@example.com',
                password: 'correct-horse',
                role: 'SUPPLIER',
            }),
        })
    })

    it('lets a signed-in supplier create a listing', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const createdListing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            supplierId: 7,
            active: true,
        }
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/listings' && options?.method === 'POST') {
                return Promise.resolve({
                    ok: true,
                    json: async () => createdListing,
                })
            }
            return Promise.resolve({
                ok: true,
                json: async () => ({items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'Create listing'}))
        fireEvent.change(screen.getByRole('textbox', {name: 'Listing title'}), {
            target: {value: createdListing.title},
        })
        fireEvent.change(screen.getByRole('combobox', {name: 'Category'}), {
            target: {value: createdListing.category},
        })
        fireEvent.change(screen.getByRole('spinbutton', {name: 'Price per day ($)'}), {
            target: {value: String(createdListing.price)},
        })
        fireEvent.change(screen.getByRole('textbox', {name: 'Location'}), {
            target: {value: createdListing.location},
        })
        fireEvent.change(screen.getByRole('textbox', {name: 'Description'}), {
            target: {value: createdListing.description},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Create listing'}))

        const successMessage = await screen.findByRole('status')
        expect(successMessage).toHaveTextContent(/Garden workspace\s+is now active\./)
        expect(successMessage).toHaveTextContent(/Listing ID:\s*14/)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer supplier.jwt.token',
            },
            body: JSON.stringify({
                title: 'Garden workspace',
                category: 'Workspace',
                price: 38,
                location: 'Calgary, AB',
                description: 'A quiet place to work.',
            }),
        })
    })

    it('shows a customer their booking history on the account page', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'neighbor@example.com', role: 'CUSTOMER'},
        }))
        const fetchMock = vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {
                    listings: [],
                    bookings: [{
                        id: 31,
                        listingId: 3,
                        listingTitle: 'Garden workspace',
                        location: 'Calgary, AB',
                        price: 38,
                        customerId: 9,
                        status: 'PENDING',
                        startTime: '2026-11-01T15:00:00Z',
                        endTime: '2026-11-02T15:00:00Z',
                    }],
                }
                : {items: []}),
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByRole('heading', {name: 'My account'}))
            .toBeInTheDocument()
        expect(screen.getByRole('heading', {name: 'My bookings'})).toBeInTheDocument()
        expect(screen.getByRole('heading', {name: 'Garden workspace'}))
            .toBeInTheDocument()
        expect(screen.getByText('pending')).toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/account', {
            headers: {Authorization: 'Bearer customer.jwt.token'},
        })
    })

    it('shows a supplier their listings and bookings sections', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {
                    listings: [{
                        id: 14,
                        title: 'Garden workspace',
                        category: 'Workspace',
                        price: 38,
                        location: 'Calgary, AB',
                        active: true,
                    }],
                    bookings: [],
                }
                : {items: []}),
        })))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByRole('heading', {name: 'My listings'}))
            .toBeInTheDocument()
        expect(screen.getByRole('heading', {name: 'Bookings on my listings'}))
            .toBeInTheDocument()
        expect(screen.getByText('Garden workspace')).toBeInTheDocument()
        expect(screen.getByText('Active')).toBeInTheDocument()
    })

    it('lets a supplier accept a pending booking from their account', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const booking = {
            id: 11,
            customerId: 9,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'PENDING',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        const fetchMock = vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => {
                if (url === '/api/bookings/11/accept') {
                    return {id: 11, status: 'ACCEPTED'}
                }
                if (url === '/api/account') {
                    return {listings: [], bookings: [booking]}
                }
                return {items: []}
            },
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Accept booking'}))

        expect(await screen.findByText('accepted')).toBeInTheDocument()
        expect(screen.queryByRole('button', {name: 'Accept booking'}))
            .not.toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/11/accept', {
            method: 'PUT',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })

    it('lets a supplier mark a confirmed booking complete from their account', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const booking = {
            id: 11,
            customerId: 9,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'CONFIRMED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        const fetchMock = vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => {
                if (url === '/api/bookings/11/complete') {
                    return {id: 11, status: 'COMPLETED'}
                }
                if (url === '/api/account') {
                    return {listings: [], bookings: [booking]}
                }
                return {items: []}
            },
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Mark complete'}))

        expect(await screen.findByText('completed')).toBeInTheDocument()
        expect(screen.queryByRole('button', {name: 'Mark complete'}))
            .not.toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/11/complete', {
            method: 'PUT',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })

    it('shows a pending booking requester\'s standing with other suppliers', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const booking = {
            id: 11,
            customerId: 9,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'PENDING',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
            customerAverageRating: 4.5,
            customerReviewCount: 2,
        }
        vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {listings: [], bookings: [booking]}
                : {items: []}),
        })))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByText('★ 4.5 (2 reviews from other suppliers)'))
            .toBeInTheDocument()
    })

    it('shows no-reviews-yet standing for a first-time customer request', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const booking = {
            id: 11,
            customerId: 9,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'PENDING',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
            customerAverageRating: null,
            customerReviewCount: 0,
        }
        vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {listings: [], bookings: [booking]}
                : {items: []}),
        })))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByText('No supplier reviews of this customer yet'))
            .toBeInTheDocument()
    })

    it('lets a supplier edit their listing from the account page', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            active: true,
        }
        const updated = {...listing, title: 'Bright garden workspace', price: 42}
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/listings/14' && options?.method === 'PUT') {
                return Promise.resolve({ok: true, json: async () => updated})
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [listing], bookings: []}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Edit'}))
        const form = screen.getByRole('form', {name: 'Edit Garden workspace'})
        fireEvent.change(within(form).getByRole('textbox', {name: 'Listing title'}), {
            target: {value: updated.title},
        })
        fireEvent.change(within(form).getByRole('spinbutton', {name: 'Price per day ($)'}), {
            target: {value: String(updated.price)},
        })
        fireEvent.click(within(form).getByRole('button', {name: 'Save changes'}))

        expect(await screen.findByRole('heading', {name: updated.title}))
            .toBeInTheDocument()
        expect(screen.getByRole('heading', {name: updated.title})
            .closest('article')).toHaveTextContent(/\$42\s*\/ day/)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14', {
            method: 'PUT',
            headers: {
                Authorization: 'Bearer supplier.jwt.token',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                title: updated.title,
                category: listing.category,
                price: updated.price,
                location: listing.location,
                description: listing.description,
            }),
        })
    })

    it('requires supplier confirmation before deactivating a listing', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            active: true,
        }
        const deactivated = {...listing, active: false}
        const fetchMock = vi.fn((url) => {
            if (url === '/api/listings/14/deactivate') {
                return Promise.resolve({ok: true, json: async () => deactivated})
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [listing], bookings: []}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Deactivate'}))
        expect(screen.getByText('This hides the listing from customers.'))
            .toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', {name: 'Confirm deactivate'}))

        expect(await screen.findByText('Inactive')).toBeInTheDocument()
        expect(screen.queryByRole('button', {name: 'Deactivate'}))
            .not.toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14/deactivate', {
            method: 'POST',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })

    it('lets a supplier add and remove a blackout from their account page', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            active: true,
        }
        const created = {
            id: 5,
            listingId: 14,
            startTime: '2026-12-01T00:00:00Z',
            endTime: '2026-12-05T00:00:00Z',
            reason: 'Vacation',
            createdAt: '2026-11-01T00:00:00Z',
        }
        let blackouts = []
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/listings/14/blackouts' && !options) {
                return Promise.resolve({ok: true, json: async () => blackouts})
            }
            if (url === '/api/listings/14/blackouts' && options?.method === 'POST') {
                blackouts = [created]
                return Promise.resolve({ok: true, json: async () => created})
            }
            if (url === '/api/listings/14/blackouts/5' && options?.method === 'DELETE') {
                blackouts = []
                return Promise.resolve({ok: true, status: 204})
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [listing], bookings: []}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Blackout dates'}))
        expect(await screen.findByText('No blackout dates yet.')).toBeInTheDocument()

        fireEvent.change(screen.getByLabelText('Start date'), {
            target: {value: '2026-12-01'},
        })
        fireEvent.change(screen.getByLabelText('End date'), {
            target: {value: '2026-12-05'},
        })
        fireEvent.change(screen.getByRole('textbox', {name: 'Reason (optional)'}), {
            target: {value: 'Vacation'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Add blackout'}))

        expect(await screen.findByText('(Vacation)')).toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14/blackouts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer supplier.jwt.token',
            },
            body: JSON.stringify({
                startTime: '2026-12-01T00:00:00Z',
                endTime: '2026-12-05T00:00:00Z',
                reason: 'Vacation',
            }),
        })

        fireEvent.click(screen.getByRole('button', {name: 'Remove'}))

        expect(await screen.findByText('No blackout dates yet.')).toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14/blackouts/5', {
            method: 'DELETE',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })

    it('shows an error message when adding a blackout fails', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            active: true,
        }
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/listings/14/blackouts' && !options) {
                return Promise.resolve({ok: true, json: async () => []})
            }
            if (url === '/api/listings/14/blackouts' && options?.method === 'POST') {
                return Promise.resolve({
                    ok: false,
                    status: 409,
                    json: async () => ({detail: 'That window overlaps an existing blackout'}),
                })
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [listing], bookings: []}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Blackout dates'}))
        await screen.findByText('No blackout dates yet.')

        fireEvent.change(screen.getByLabelText('Start date'), {
            target: {value: '2026-12-01'},
        })
        fireEvent.change(screen.getByLabelText('End date'), {
            target: {value: '2026-12-05'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Add blackout'}))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'That window overlaps an existing blackout',
        )
    })

    it('lets a customer leave a review on a completed booking and see it appear', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const booking = {
            id: 11,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'COMPLETED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        const created = {
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 7,
            rating: 5,
            comment: 'Great stay',
            createdAt: '2026-11-04T00:00:00Z',
        }
        let reviews = []
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/bookings/11/reviews' && !options) {
                return Promise.resolve({ok: true, json: async () => reviews})
            }
            if (url === '/api/bookings/11/reviews' && options?.method === 'POST') {
                reviews = [created]
                return Promise.resolve({ok: true, json: async () => created})
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [], bookings: [booking]}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Review'}))
        expect(await screen.findAllByText('No review yet.')).toHaveLength(2)

        fireEvent.change(screen.getByRole('combobox', {name: 'Rating'}), {
            target: {value: '5'},
        })
        fireEvent.change(screen.getByRole('textbox', {name: 'Comment (optional)'}), {
            target: {value: 'Great stay'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Leave a review'}))

        expect(await screen.findByText(/Great stay/)).toBeInTheDocument()
        expect(await screen.findByText('Your review')).toBeInTheDocument()
        expect(fetchMock).toHaveBeenCalledWith('/api/bookings/11/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer customer.jwt.token',
            },
            body: JSON.stringify({rating: 5, comment: 'Great stay'}),
        })
        expect(await screen.findByText('You already reviewed this booking.'))
            .toBeInTheDocument()
        expect(await screen.findByRole('button', {name: 'See reviews'}))
            .toBeInTheDocument()
    })

    it('separates the listing review from the customer review on a completed booking', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const booking = {
            id: 11,
            customerId: 9,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'COMPLETED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
            reviewedByCaller: true,
        }
        const reviewOfListing = {
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 7,
            rating: 5,
            comment: 'Great stay',
            createdAt: '2026-11-04T00:00:00Z',
        }
        const reviewOfCustomer = {
            id: 22,
            bookingId: 11,
            reviewerId: 7,
            revieweeId: 9,
            rating: 4,
            comment: 'Easy guest',
            createdAt: '2026-11-04T00:00:00Z',
        }
        vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => {
                if (url === '/api/bookings/11/reviews') {
                    return [reviewOfListing, reviewOfCustomer]
                }
                return url === '/api/account'
                    ? {listings: [], bookings: [booking]}
                    : {items: []}
            },
        })))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'See reviews'}))

        const listingHeading = await screen.findByRole('heading', {name: 'Review of the listing'})
        const customerHeading = await screen.findByRole('heading', {name: 'Review of the customer'})

        expect(listingHeading.nextElementSibling).toHaveTextContent('Great stay')
        expect(listingHeading.nextElementSibling).not.toHaveTextContent('Easy guest')
        expect(customerHeading.nextElementSibling).toHaveTextContent('Easy guest')
        expect(customerHeading.nextElementSibling).not.toHaveTextContent('Great stay')
    })

    it('shows an error message when adding a review fails', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const booking = {
            id: 11,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'COMPLETED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        const fetchMock = vi.fn((url, options) => {
            if (url === '/api/bookings/11/reviews' && !options) {
                return Promise.resolve({ok: true, json: async () => []})
            }
            if (url === '/api/bookings/11/reviews' && options?.method === 'POST') {
                return Promise.resolve({
                    ok: false,
                    status: 400,
                    json: async () => ({
                        detail: 'Reviews are only allowed after a completed booking',
                    }),
                })
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [], bookings: [booking]}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        fireEvent.click(await screen.findByRole('button', {name: 'Review'}))
        await screen.findAllByText('No review yet.')

        fireEvent.click(screen.getByRole('button', {name: 'Leave a review'}))

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'Reviews are only allowed after a completed booking',
        )
    })

    it('shows "See reviews" on load for a booking the caller already reviewed, with no review fetch needed', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const booking = {
            id: 11,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'COMPLETED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
            reviewedByCaller: true,
        }
        const fetchMock = vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {listings: [], bookings: [booking]}
                : {items: []}),
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByRole('button', {name: 'See reviews'}))
            .toBeInTheDocument()
        expect(screen.queryByRole('button', {name: 'Review'}))
            .not.toBeInTheDocument()
        expect(fetchMock).not.toHaveBeenCalledWith('/api/bookings/11/reviews')
    })

    it('never fetches per-booking reviews on account load, even with many completed bookings', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const bookings = Array.from({length: 5}, (_, index) => ({
            id: index + 1,
            listingTitle: `Listing ${index + 1}`,
            location: 'Calgary, AB',
            price: 38,
            status: 'COMPLETED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
            reviewedByCaller: index % 2 === 0,
        }))
        const fetchMock = vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {listings: [], bookings}
                : {items: []}),
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findAllByRole('article')).toHaveLength(5)
        expect(fetchMock.mock.calls.some(([url]) =>
            String(url).endsWith('/reviews'))).toBe(false)
        expect(await screen.findAllByRole('button', {name: 'See reviews'}))
            .toHaveLength(3)
        expect(screen.getAllByRole('button', {name: 'Review'}))
            .toHaveLength(2)
    })

    it('does not offer a review option for a booking that is not completed yet', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const booking = {
            id: 11,
            listingTitle: 'Garden workspace',
            location: 'Calgary, AB',
            price: 38,
            status: 'CONFIRMED',
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-03T00:00:00Z',
        }
        vi.stubGlobal('fetch', vi.fn((url) => Promise.resolve({
            ok: true,
            json: async () => (url === '/api/account'
                ? {listings: [], bookings: [booking]}
                : {items: []}),
        })))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))

        expect(await screen.findByText('Garden workspace')).toBeInTheDocument()
        expect(screen.queryByRole('button', {name: 'Review'}))
            .not.toBeInTheDocument()
    })

    it('disables blackout dates for a deactivated listing', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, email: 'maker@example.com', role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
            active: true,
        }
        const deactivated = {...listing, active: false}
        const fetchMock = vi.fn((url) => {
            if (url === '/api/listings/14/deactivate') {
                return Promise.resolve({ok: true, json: async () => deactivated})
            }
            return Promise.resolve({
                ok: true,
                json: async () => (url === '/api/account'
                    ? {listings: [listing], bookings: []}
                    : {items: []}),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'My account'}))
        expect(await screen.findByRole('button', {name: 'Blackout dates'}))
            .toBeEnabled()

        fireEvent.click(screen.getByRole('button', {name: 'Deactivate'}))
        fireEvent.click(screen.getByRole('button', {name: 'Confirm deactivate'}))

        await screen.findByText('Inactive')
        expect(screen.getByRole('button', {name: 'Blackout dates'}))
            .toBeDisabled()
        expect(fetchMock).not.toHaveBeenCalledWith(
            '/api/listings/14/blackouts', expect.anything())
    })

    it('signs in through the backend and stores the returned session', async () => {
        const user = {id: 9, email: 'neighbor@example.com', role: 'CUSTOMER'}
        const fetchMock = vi.fn()
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({items: []}),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: async () => ({
                    token: 'signed.jwt.token',
                    tokenType: 'Bearer',
                    expiresInSeconds: 3600,
                    user,
                }),
            })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)

        fireEvent.click(screen.getByRole('link', {name: 'Sign in'}))
        fireEvent.change(await screen.findByRole('textbox', {name: 'Email'}), {
            target: {value: user.email},
        })
        fireEvent.change(screen.getByLabelText('Password'), {
            target: {value: 'correct-horse'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Sign in'}))

        expect(await screen.findByRole('button', {name: 'Sign out'})).toBeInTheDocument()
        expect(JSON.parse(localStorage.getItem('nicheflow.session'))).toEqual({
            token: 'signed.jwt.token',
            tokenType: 'Bearer',
            expiresInSeconds: 3600,
            user,
        })
        expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({email: user.email, password: 'correct-horse'}),
        })
    })

    it('shows a clear non-connected message when submitting password reset', async () => {
        render(<App/>)

        fireEvent.click(screen.getByRole('link', {name: 'Sign in'}))
        fireEvent.click(
            await screen.findByRole('link', {name: 'Forgot password?'}),
        )

        expect(
            await screen.findByRole('heading', {name: 'Reset your password'}),
        ).toBeInTheDocument()

        fireEvent.change(screen.getByRole('textbox', {name: 'Email'}), {
            target: {value: 'neighbor@example.com'},
        })
        fireEvent.click(screen.getByRole('button', {name: 'Send reset link'}))

        expect(
            await screen.findByRole('status'),
        ).toHaveTextContent('No reset email was sent.')
    })

    it('opens a listing detail page and proceeds to its booking form', async () => {
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'Sunny garden studio'}))

        expect(
            await screen.findByRole('heading', {name: 'Sunny garden studio'}),
        ).toBeInTheDocument()
        expect(
            screen.getByText(/A bright, peaceful workspace tucked beside a sunny neighborhood garden/),
        ).toBeInTheDocument()

        fireEvent.click(screen.getByRole('link', {name: 'Check dates'}))

        expect(
            await screen.findByRole('heading', {name: 'Request a booking'}),
        ).toBeInTheDocument()
        expect(screen.getByText('$38 × — days')).toBeInTheDocument()
        expect(
            screen.getByText(/Sign in with a customer account to request this booking/),
        ).toBeInTheDocument()
    })

    it('shows other customers\' reviews on a listing detail page', async () => {
        const review = {
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 1,
            rating: 4,
            comment: 'Lovely little garden spot',
            createdAt: '2026-11-04T00:00:00Z',
        }
        vi.stubGlobal('fetch', vi.fn((url) => {
            if (String(url).endsWith('/reviews')) {
                return Promise.resolve({ok: true, json: async () => [review]})
            }
            return singleListingResponse(url) ?? Promise.resolve({
                ok: true,
                json: async () => ({
                    items: listings.map(({
                                             id,
                                             title,
                                             category,
                                             price,
                                             location,
                                             description,
                                         }) => ({
                        id,
                        title,
                        category,
                        price,
                        location,
                        description,
                        supplierId: 1,
                        active: true,
                    })),
                }),
            })
        }))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'Sunny garden studio'}))

        expect(
            await screen.findByRole('heading', {name: 'What customers are saying'}),
        ).toBeInTheDocument()
        expect(await screen.findByText('Lovely little garden spot'))
            .toBeInTheDocument()
    })

    it('marks the signed-in customer\'s own review on a listing detail page', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'customer.jwt.token',
            user: {id: 9, email: 'bob@example.com', role: 'CUSTOMER'},
        }))
        const ownReview = {
            id: 21,
            bookingId: 11,
            reviewerId: 9,
            revieweeId: 1,
            rating: 4,
            comment: 'Lovely little garden spot',
            createdAt: '2026-11-04T00:00:00Z',
        }
        const otherReview = {
            id: 22,
            bookingId: 12,
            reviewerId: 99,
            revieweeId: 1,
            rating: 5,
            comment: 'Loved it too',
            createdAt: '2026-11-05T00:00:00Z',
        }
        vi.stubGlobal('fetch', vi.fn((url) => {
            if (String(url).endsWith('/reviews')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => [ownReview, otherReview],
                })
            }
            return singleListingResponse(url) ?? Promise.resolve({
                ok: true,
                json: async () => ({
                    items: listings.map(({
                                             id,
                                             title,
                                             category,
                                             price,
                                             location,
                                             description,
                                         }) => ({
                        id,
                        title,
                        category,
                        price,
                        location,
                        description,
                        supplierId: 1,
                        active: true,
                    })),
                }),
            })
        }))
        render(<App/>)

        fireEvent.click(await screen.findByRole('link', {name: 'Sunny garden studio'}))
        await screen.findByText('Loved it too')

        const badges = await screen.findAllByText('Your review')
        expect(badges).toHaveLength(1)
        expect(badges[0].closest('li')).toHaveTextContent('Lovely little garden spot')
    })

    it('submits a booking request and displays the pending confirmation', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'signed.jwt.token',
            user: {id: 9, email: 'neighbor@example.com', role: 'CUSTOMER'},
        }))
        const fetchMock = vi.fn((url) => {
            const single = singleListingResponse(url)
            if (single) return single
            if (url.startsWith('/api/bookings/availability/')) {
                return Promise.resolve({
                    ok: true,
                    json: async () => [],
                })
            }
            if (url === '/api/bookings') {
                return Promise.resolve({
                    ok: true,
                    json: async () => ({
                        id: 15,
                        listingId: listings[0].id,
                        status: 'PENDING',
                    }),
                })
            }

            return Promise.resolve({
                ok: true,
                json: async () => ({
                    items: listings.map((listing) => ({
                        ...listing,
                        active: true,
                    }))
                }),
            })
        })
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)
        fireEvent.click(await screen.findByRole('link', {name: 'Sunny garden studio'}))
        fireEvent.click(await screen.findByRole('link', {name: 'Check dates'}))
        await screen.findByRole('heading', {name: 'Request a booking'})

        const tomorrow = new Date()
        tomorrow.setDate(tomorrow.getDate() + 1)
        const checkout = new Date(tomorrow)
        checkout.setDate(checkout.getDate() + 2)
        const formatDate = (date) =>
            `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

        // The calendar opens on the current month; advance to whatever
        // month each date falls in, so this test keeps passing across a
        // real month boundary instead of only when "today + 3 days"
        // happens to stay inside the same month. Wait for the calendar
        // to finish loading availability before navigating, since its
        // days don't exist yet while "Checking availability…" shows.
        const today = new Date()
        const monthsToTomorrow =
            (tomorrow.getFullYear() - today.getFullYear()) * 12 +
            (tomorrow.getMonth() - today.getMonth())
        await screen.findAllByRole('button', {name: /^\d{4}-\d{2}-\d{2}/})
        for (let month = 0; month < monthsToTomorrow; month += 1) {
            fireEvent.click(screen.getByRole('button', {name: 'Next month'}))
        }
        fireEvent.click(screen.getByRole('button', {
            name: formatDate(tomorrow),
        }))

        const monthsToCheckout =
            (checkout.getFullYear() - tomorrow.getFullYear()) * 12 +
            (checkout.getMonth() - tomorrow.getMonth())
        for (let month = 0; month < monthsToCheckout; month += 1) {
            fireEvent.click(screen.getByRole('button', {name: 'Next month'}))
        }
        fireEvent.click(screen.getByRole('button', {
            name: formatDate(checkout),
        }))

        expect(screen.getByText('$38 × 2 days')).toBeInTheDocument()
        expect(
            within(screen.getByLabelText('Booking estimate')).getAllByText('$76'),
        ).toHaveLength(2)

        fireEvent.click(screen.getByRole('button', {name: 'Request booking'}))
        expect(await screen.findByRole('status')).toHaveTextContent(
            'Booking request submitted. Its current status is pending. (Request #15)',
        )
        expect(fetchMock).toHaveBeenLastCalledWith('/api/bookings', expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
                Authorization: 'Bearer signed.jwt.token',
            }),
            body: JSON.stringify({
                listingId: listings[0].id,
                startTime: `${formatDate(tomorrow)}T00:00:00Z`,
                endTime: `${formatDate(checkout)}T00:00:00Z`,
            }),
        }))
    })

    it('marks reserved booking dates unavailable while leaving the checkout boundary open', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'signed.jwt.token',
            user: {id: 9, email: 'neighbor@example.com', role: 'CUSTOMER'},
        }))
        const tomorrow = new Date()
        tomorrow.setDate(tomorrow.getDate() + 1)
        const checkoutBoundary = new Date(tomorrow)
        checkoutBoundary.setDate(checkoutBoundary.getDate() + 2)
        const formatDate = (date) =>
            `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        const fetchMock = vi.fn((url) => singleListingResponse(url) ?? Promise.resolve({
            ok: true,
            json: async () => url.startsWith('/api/bookings/availability/')
                ? [{
                    startTime: `${formatDate(tomorrow)}T00:00:00Z`,
                    endTime: `${formatDate(checkoutBoundary)}T00:00:00Z`,
                }]
                : {items: listings.map((listing) => ({...listing, active: true}))},
        }))
        vi.stubGlobal('fetch', fetchMock)
        render(<App/>)
        fireEvent.click(await screen.findByRole('link', {name: 'Sunny garden studio'}))
        fireEvent.click(await screen.findByRole('link', {name: 'Check dates'}))

        // The calendar opens on the current month; advance to whatever
        // month each date falls in, so this test keeps passing across a
        // real month boundary instead of only when "today + 3 days"
        // happens to stay inside the same month. Wait for the calendar
        // to finish loading availability before navigating, since its
        // days don't exist yet while "Checking availability…" shows.
        const today = new Date()
        const monthsToTomorrow =
            (tomorrow.getFullYear() - today.getFullYear()) * 12 +
            (tomorrow.getMonth() - today.getMonth())
        await screen.findAllByRole('button', {name: /^\d{4}-\d{2}-\d{2}/})
        for (let month = 0; month < monthsToTomorrow; month += 1) {
            fireEvent.click(screen.getByRole('button', {name: 'Next month'}))
        }

        const unavailableDay = screen.getByRole('button', {
            name: `${formatDate(tomorrow)}, unavailable`,
        })
        expect(unavailableDay).toBeDisabled()

        const monthsToBoundary =
            (checkoutBoundary.getFullYear() - tomorrow.getFullYear()) * 12 +
            (checkoutBoundary.getMonth() - tomorrow.getMonth())
        for (let month = 0; month < monthsToBoundary; month += 1) {
            fireEvent.click(screen.getByRole('button', {name: 'Next month'}))
        }

        const boundaryDay = screen.getByRole('button', {
            name: formatDate(checkoutBoundary),
        })
        expect(boundaryDay).toBeEnabled()
        fireEvent.click(boundaryDay)
        const displayDate = new Intl.DateTimeFormat(undefined, {
            dateStyle: 'medium',
            timeZone: 'UTC',
        }).format(new Date(`${formatDate(checkoutBoundary)}T00:00:00Z`))
        expect(screen.getByText(displayDate, {
            selector: 'strong',
        })).toBeInTheDocument()
    })
})
