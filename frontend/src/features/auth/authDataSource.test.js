import {afterEach, describe, expect, it, vi} from 'vitest'
import {getSession, signIn, signOut, signUp} from './authDataSource'

describe('authentication data source', () => {
    afterEach(() => {
        localStorage.clear()
        vi.unstubAllGlobals()
    })

    it('sends credentials to login and stores the returned session', async () => {
        const session = {
            token: 'jwt.token',
            tokenType: 'Bearer',
            expiresInSeconds: 3600,
            user: {id: 2, email: 'person@example.com', role: 'CUSTOMER'},
        }
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => session,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(signIn('person@example.com', 'correct-horse'))
            .resolves.toEqual(session.user)
        expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                email: 'person@example.com',
                password: 'correct-horse',
            }),
        })
        expect(getSession()).toEqual(session)
    })

    it('returns the backend login error instead of storing a session', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            json: async () => ({detail: 'Invalid email or password.'}),
        }))

        await expect(signIn('person@example.com', 'wrong'))
            .rejects.toThrow('Invalid email or password.')
        expect(getSession()).toBeNull()
    })

    it('registers a customer account', async () => {
        const user = {id: 3, email: 'new@example.com', role: 'CUSTOMER'}
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => user,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(signUp('new@example.com', 'correct-horse'))
            .resolves.toEqual(user)
        expect(fetchMock).toHaveBeenCalledWith('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                email: 'new@example.com',
                password: 'correct-horse',
                role: 'CUSTOMER',
            }),
        })
        expect(getSession()).toBeNull()
    })

    it('registers supplier accounts when selected', async () => {
        const user = {id: 4, email: 'supplier@example.com', role: 'SUPPLIER'}
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: async () => user,
        })
        vi.stubGlobal('fetch', fetchMock)

        await expect(signUp('supplier@example.com', 'correct-horse', 'SUPPLIER'))
            .resolves.toEqual(user)
        expect(fetchMock).toHaveBeenCalledWith('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                email: 'supplier@example.com',
                password: 'correct-horse',
                role: 'SUPPLIER',
            }),
        })
    })

    it('does not send registration requests for unsupported roles', async () => {
        const fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)

        await expect(signUp('admin@example.com', 'correct-horse', 'ADMIN'))
            .rejects.toThrow('Choose a customer or supplier account.')
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('clears a stored session when signing out', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            json: async () => ({
                token: 'jwt.token',
                user: {id: 2, email: 'person@example.com', role: 'CUSTOMER'},
            }),
        }))
        await signIn('person@example.com', 'correct-horse')

        signOut()

        expect(getSession()).toBeNull()
    })
})
