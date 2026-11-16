import {renderHook, waitFor} from '@testing-library/react'
import {afterEach, describe, expect, it, vi} from 'vitest'
import useListing from './useListing'

/*
 * Boundary-value test for the listing detail and booking pages.
 *
 * The backend returns search results 20 per page. A listing that is
 * active but sits on the second page (the 21st listing) must still be
 * loadable by its id, the same as a listing on the first page.
 *
 * The fetch mock behaves like the real backend: GET /api/listings (or
 * ?page=0) returns listings 1-20, ?page=1 returns listing 21, and
 * GET /api/listings/21 returns listing 21 directly.
 */

const PAGE_SIZE = 20
const TOTAL_LISTINGS = 21

function makeListing(id) {
    return {
        id,
        title: `Test Listing ${id}`,
        category: 'Workspace',
        price: 10,
        location: 'Camrose',
        description: `Boundary test ${id}`,
        supplierId: 2,
        active: true,
    }
}

const allListings = Array.from({length: TOTAL_LISTINGS}, (_, i) =>
    makeListing(i + 1))

function jsonResponse(body, status = 200) {
    return {ok: status >= 200 && status < 300, status, json: async () => body}
}

function backendFetch(input) {
    const url = new URL(String(input), 'http://localhost')
    const single = url.pathname.match(/^\/api\/listings\/(\d+)$/)
    if (single) {
        const listing = allListings.find((l) => l.id === Number(single[1]))
        return Promise.resolve(listing
            ? jsonResponse(listing)
            : jsonResponse({message: 'Not found'}, 404))
    }
    if (url.pathname === '/api/listings') {
        const page = Number(url.searchParams.get('page') ?? 0)
        const items = allListings.slice(page * PAGE_SIZE,
            (page + 1) * PAGE_SIZE)
        return Promise.resolve(jsonResponse({
            items,
            page,
            size: PAGE_SIZE,
            totalElements: TOTAL_LISTINGS,
            totalPages: Math.ceil(TOTAL_LISTINGS / PAGE_SIZE),
        }))
    }
    return Promise.resolve(jsonResponse({message: 'Not found'}, 404))
}

describe('useListing page-size boundary', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('loads the 20th listing (last one on the first page)', async () => {
        vi.stubGlobal('fetch', vi.fn(backendFetch))

        const {result} = renderHook(() => useListing('20'))

        await waitFor(() => expect(result.current.isLoading).toBe(false))
        expect(result.current.loadError).toBe('')
        expect(result.current.listing).toMatchObject({id: 20,
            title: 'Test Listing 20'})
    })

    it('loads the 21st listing (first one on the second page)', async () => {
        vi.stubGlobal('fetch', vi.fn(backendFetch))

        const {result} = renderHook(() => useListing('21'))

        await waitFor(() => expect(result.current.isLoading).toBe(false))
        expect(result.current.loadError).toBe('')
        expect(result.current.listing).toMatchObject({id: 21,
            title: 'Test Listing 21'})
    })
})
