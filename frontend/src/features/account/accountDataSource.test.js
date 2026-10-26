import {afterEach, describe, expect, it, vi} from 'vitest'
import {deactivateListing, getAccount, updateListing} from './accountDataSource'

describe('account data source', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('requests account data with the saved token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'member.jwt.token',
            user: {id: 8, role: 'CUSTOMER'},
        }))
        const account = {listings: [], bookings: []}
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => account,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(getAccount()).resolves.toEqual(account)
        expect(fetchMock).toHaveBeenCalledWith('/api/account', {
            headers: {Authorization: 'Bearer member.jwt.token'},
        })
    })

    it('clears a rejected session and requests a fresh sign-in on 401', async () => {
        const session = {
            token: 'member.jwt.token',
            user: {id: 8, role: 'CUSTOMER'},
        }
        localStorage.setItem('nicheflow.session', JSON.stringify({
            ...session,
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
            json: async () => ({detail: 'Sign in again'}),
        }))

        await expect(getAccount()).rejects.toMatchObject({
            message: 'Your sign-in session expired or was rejected. Please sign in again.',
            status: 401,
        })
        expect(localStorage.getItem('nicheflow.session')).toBeNull()
    })

    it('rejects access without a session before making a request', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(getAccount()).rejects.toThrow('Sign in')
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('updates a listing using the supplier token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        const updated = {id: 14, title: 'Updated workspace', active: true}
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => updated,
        })
        vi.stubGlobal('fetch', fetchMock)
        const listing = {
            title: updated.title,
            category: 'Workspace',
            price: '42.50',
            location: 'Calgary, AB',
            description: 'A refreshed description.',
        }

        await expect(updateListing(14, listing)).resolves.toEqual(updated)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14', {
            method: 'PUT',
            headers: {
                Authorization: 'Bearer supplier.jwt.token',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({...listing, price: 42.5}),
        })
    })

    it('deactivates a listing using the supplier token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        const inactiveListing = {id: 14, active: false}
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => inactiveListing,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(deactivateListing(14)).resolves.toEqual(inactiveListing)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/14/deactivate', {
            method: 'POST',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })
})
