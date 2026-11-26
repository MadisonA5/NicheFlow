import {afterEach, describe, expect, it, vi} from 'vitest'
import {createBlackout, deleteBlackout, listBlackouts} from './blackoutsDataSource'

describe('blackouts data source', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        localStorage.clear()
    })

    it('loads public blackouts for a listing with no auth header', async () => {
        const blackouts = [{
            id: 5,
            listingId: 3,
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-05T00:00:00Z',
            reason: 'Vacation',
            createdAt: '2026-10-01T00:00:00Z',
        }]
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => blackouts,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(listBlackouts('3')).resolves.toEqual(blackouts)
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/3/blackouts')
    })

    it('rejects a malformed blackout list response', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => [{id: 5, startTime: 'not-a-date'}],
        }))

        await expect(listBlackouts(3))
            .rejects.toThrow('blackout response')
    })

    it('creates a blackout with the supplier bearer token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        const created = {
            id: 5,
            listingId: 3,
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-05T00:00:00Z',
            reason: 'Vacation',
            createdAt: '2026-10-01T00:00:00Z',
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => created,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(createBlackout(3, {
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-05T00:00:00Z',
            reason: 'Vacation',
        })).resolves.toEqual(created)

        expect(fetchMock).toHaveBeenCalledWith('/api/listings/3/blackouts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer supplier.jwt.token',
            },
            body: JSON.stringify({
                startTime: '2026-11-01T00:00:00Z',
                endTime: '2026-11-05T00:00:00Z',
                reason: 'Vacation',
            }),
        })
    })

    it('rejects a blackout create without a supplier session', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(createBlackout(3, {
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-05T00:00:00Z',
            reason: null,
        })).rejects.toThrow('Sign in with a supplier account')
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('clears a rejected session and requests a fresh sign-in on 401', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
            json: async () => ({detail: 'Sign in again'}),
        }))

        await expect(createBlackout(3, {
            startTime: '2026-11-01T00:00:00Z',
            endTime: '2026-11-05T00:00:00Z',
            reason: null,
        })).rejects.toMatchObject({
            message: 'Your sign-in session expired or was rejected. Please sign in again.',
            status: 401,
        })
        expect(localStorage.getItem('nicheflow.session')).toBeNull()
    })

    it('deletes a blackout with the supplier bearer token', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        const fetchMock = vi.fn().mockResolvedValue({ok: true, status: 204})
        vi.stubGlobal('fetch', fetchMock)

        await expect(deleteBlackout(3, 5)).resolves.toBeUndefined()
        expect(fetchMock).toHaveBeenCalledWith('/api/listings/3/blackouts/5', {
            method: 'DELETE',
            headers: {Authorization: 'Bearer supplier.jwt.token'},
        })
    })

    it('shows the API detail when a blackout delete fails', async () => {
        localStorage.setItem('nicheflow.session', JSON.stringify({
            token: 'supplier.jwt.token',
            user: {id: 7, role: 'SUPPLIER'},
        }))
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 404,
            json: async () => ({detail: 'No blackout found with id 5'}),
        }))

        await expect(deleteBlackout(3, 5))
            .rejects.toThrow('No blackout found with id 5')
    })
})
