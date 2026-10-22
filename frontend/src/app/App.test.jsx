import {fireEvent, render, screen, within} from '@testing-library/react'
import {describe, expect, it} from 'vitest'
import App from './App.jsx'

describe('NicheFlow landing page', () => {
    it('shows the welcome message and labeled search fields', () => {
        render(<App/>)

        expect(
            screen.getByRole('heading', {name: 'Find your next favorite place.'}),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('searchbox', {name: 'What are you looking for?'}),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('textbox', {name: 'Where?'}),
        ).toBeInTheDocument()
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
        expect(within(listings[0]).getByText('Calgary, AB')).toBeInTheDocument()
        expect(within(listings[0]).getByText('$38')).toBeInTheDocument()

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
        expect(
            screen.getByRole('heading', {name: 'Backyard dinner spot'}),
        ).toBeInTheDocument()
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
        expect(
            screen.getByText('Preview only — account access isn’t connected yet.'),
        ).toBeInTheDocument()

        fireEvent.click(screen.getByRole('link', {name: 'Create an account'}))

        expect(
            await screen.findByRole('heading', {name: 'Create your account'}),
        ).toBeInTheDocument()
        expect(screen.getByRole('textbox', {name: 'Name'})).toBeRequired()
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
            screen.getByText(/availability and reservations aren’t connected/),
        ).toBeInTheDocument()
    })

    it('estimates the booking total without creating a reservation', async () => {
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

        fireEvent.change(screen.getByRole('textbox', {name: 'Your name'}), {
            target: {value: 'Taylor'},
        })
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

        fireEvent.click(screen.getByRole('button', {name: 'Continue'}))
        expect(await screen.findByRole('status')).toHaveTextContent(
            'No reservation was created and no information was sent.',
        )
    })
})
