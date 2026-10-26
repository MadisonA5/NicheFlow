import {afterEach, describe, expect, it, vi} from 'vitest'
import {createListing} from './createListingDataSource'

describe('create listing data source', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('posts listing fields with the supplier token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        const listing = {
            id: 14,
            title: 'Garden workspace',
            category: 'Workspace',
            price: 38,
            location: 'Calgary, AB',
            description: 'A quiet place to work.',
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => listing,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(createListing({
            title: listing.title,
            category: listing.category,
            price: '38.00',
            location: listing.location,
            description: listing.description,
        })).resolves.toEqual(listing)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer supplier.jwt.token',
            },
            body: JSON.stringify({
                title: listing.title,
                category: listing.category,
                price: 38,
                location: listing.location,
                description: listing.description,
            }),
        })
    })

    it('shows backend validation errors', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 400,
            json: async () => ({detail: 'Price must not exceed the allowed limit'}),
        }))

        await expect(createListing({
            title: 'Listing',
            category: 'Other',
            price: '100000000',
            location: 'Calgary',
            description: 'Description',
        })).rejects.toThrow('Price must not exceed the allowed limit')
    })

    it('does not send requests without a supplier session', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(createListing({})).rejects.toThrow('supplier account')
        expect(fetchMock).not.toHaveBeenCalled()
    })
})
