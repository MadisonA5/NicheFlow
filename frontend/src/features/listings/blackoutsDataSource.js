import {getSession, signOut} from '../auth/authDataSource'

async function blackoutRequest(listingId, method, path = '', requireSupplier = false, body) {
    const session = getSession()
    if (requireSupplier && (!session?.token || session.user.role !== 'SUPPLIER')) {
        throw new Error('Sign in with a supplier account to manage blackouts.')
    }

    const url = `/api/listings/${Number(listingId)}/blackouts${path}`
    const headers = {
        ...(session?.token ? {Authorization: `Bearer ${session.token}`} : {}),
        ...(body ? {'Content-Type': 'application/json'} : {}),
    }

    const response = method === 'GET'
        ? await fetch(url)
        : await fetch(url, {
            method,
            headers,
            ...(body ? {body: JSON.stringify(body)} : {}),
        })

    if (response.status === 401) {
        signOut()
        const error = new Error(
            'Your sign-in session expired or was rejected. Please sign in again.',
        )
        error.status = response.status
        throw error
    }

    const payload = response.status === 204
        ? null
        : await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(payload?.detail ?? `Blackout request failed (${response.status}).`)
    }

    return payload
}

export async function listBlackouts(listingId) {
    const payload = await blackoutRequest(listingId, 'GET')

    if (!Array.isArray(payload) || payload.some((blackout) =>
        !blackout?.id || !blackout?.startTime || !blackout?.endTime ||
        !Number.isFinite(Date.parse(blackout.startTime)) ||
        !Number.isFinite(Date.parse(blackout.endTime)))) {
        throw new Error('The blackout response was not in the expected format.')
    }

    return payload
}

export async function createBlackout(listingId, {startTime, endTime, reason}) {
    const payload = await blackoutRequest(
        listingId, 'POST', '', true, {startTime, endTime, reason},
    )

    if (!payload?.id || Number(payload.listingId) !== Number(listingId)) {
        throw new Error('The blackout response was not in the expected format.')
    }

    return payload
}

export async function deleteBlackout(listingId, blackoutId) {
    await blackoutRequest(
        listingId, 'DELETE', `/${Number(blackoutId)}`, true,
    )
}
