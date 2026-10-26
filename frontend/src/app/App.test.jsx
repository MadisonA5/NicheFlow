import {fireEvent, render, screen, within} from '@testing-library/react'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import App from './App.jsx'
import {listings} from '../features/listings/listings'

describe('NicheFlow landing page', () => {
    beforeEach(() => {
        localStorage.clear()
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
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
        }))
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

    it('submits a booking request and displays the pending confirmation', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'signed.jwt.token',
            user: {id: 9, email: 'neighbor@example.com', role: 'CUSTOMER'},
        }))
        const fetchMock = vi.fn((url) => {
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

        fireEvent.change(screen.getByLabelText('Check-in'), {
            target: {value: formatDate(tomorrow)},
        })
        fireEvent.change(screen.getByLabelText('Check-out'), {
            target: {value: formatDate(checkout)},
        })

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
})
