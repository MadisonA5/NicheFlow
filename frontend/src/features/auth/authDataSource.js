const sessionKey = 'nicheflow.session'

async function requestAuth(path, body) {
    const response = await fetch(`/api/auth/${path}`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
    })
    const payload = await response.json().catch(() => null)

    if (!response.ok) {
        throw new Error(payload?.detail ?? 'Account request failed. Please try again.')
    }

    return payload
}

export async function signIn(email, password) {
    const session = await requestAuth('login', {email, password})
    if (!session?.token || !session.user) {
        throw new Error('The sign-in response was not in the expected format.')
    }

    localStorage.setItem(sessionKey, JSON.stringify(session))
    return session.user
}

export async function signUp(email, password, role = 'CUSTOMER') {
    if (!['CUSTOMER', 'SUPPLIER'].includes(role)) {
        throw new Error('Choose a customer or supplier account.')
    }

    return requestAuth('register', {email, password, role})
}

export function getSession() {
    const storedSession = localStorage.getItem(sessionKey)
    if (!storedSession) return null

    try {
        const session = JSON.parse(storedSession)
        return session?.token && session.user ? session : null
    } catch {
        return null
    }
}

export function signOut() {
    localStorage.removeItem(sessionKey)
}
