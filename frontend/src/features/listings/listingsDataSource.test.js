import {afterEach, describe, expect, it, vi} from 'vitest'
import {signIn} from '../auth/authDataSource'
import {getListings} from './listingsDataSource'

describe('getListings', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('loads the page of listings and adds presentation defaults', async () => {
        const apiListing = {
            id: 12,
            title: 'Kayak rental',
            category: 'SPORTS',
            price: 25,
            location: 'Calgary, AB',
            description: 'A comfortable bike for exploring local paths.',
            supplierId: 4,
            active: true,
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({items: [apiListing]}),
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(getListings()).resolves.toEqual([
            {...apiListing, icon: '🚲', color: 'peach'},
        ])
        expect(fetchMock).toHaveBeenCalledWith('/api/listings', {
            headers: undefined,
        })
    })

    it('includes the saved bearer token when requesting listings', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                token: 'signed.jwt.token',
                user: {id: 5, email: 'person@example.com', role: 'CUSTOMER'},
            }),
        }))
        await signIn('person@example.com', 'correct-horse')

        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({items: []}),
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(getListings()).resolves.toEqual([])
        expect(fetchMock).toHaveBeenCalledWith('/api/listings', {
            headers: {Authorization: 'Bearer signed.jwt.token'},
        })
    })

    it('reports HTTP errors instead of returning sample listings', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 503,
        }))

        await expect(getListings()).rejects.toThrow(
            'Listings request failed (503).',
        )
    })

    it('rejects an unexpected API response shape', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({content: []}),
        }))

        await expect(getListings()).rejects.toThrow(
            'Listings response was not in the expected format.',
        )
    })
})
