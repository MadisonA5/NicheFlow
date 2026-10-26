import {useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import nicheFlowLogo from '../../assets/nicheflow logo1.png'
import {signIn, signUp} from './authDataSource'

const pageContent = {
    'sign-in': {
        heading: 'Welcome back',
        description: 'Sign in to pick up where you left off.',
        button: 'Sign in',
        fields: ['email', 'password'],
    },
    'sign-up': {
        heading: 'Create your account',
        description: 'Join your neighborhood and find something local.',
        button: 'Create account',
        fields: ['email', 'password'],
    },
    'forgot-password': {
        heading: 'Reset your password',
        description: 'Enter your email and we’ll help you get back in.',
        button: 'Send reset link',
        fields: ['email'],
    },
}

const fieldDetails = {
    name: {label: 'Name', type: 'text', autoComplete: 'name'},
    email: {label: 'Email', type: 'email', autoComplete: 'email'},
    password: {
        label: 'Password',
        type: 'password',
        autoComplete: 'new-password',
    },
}

function AuthPage({mode}) {
    const [message, setMessage] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const navigate = useNavigate()
    const content = pageContent[mode]

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage('')

        if (mode === 'forgot-password') {
            setMessage(
                'Password reset is not connected yet. No reset email was sent.',
            )
            return
        }

        const formData = new FormData(event.currentTarget)
        const email = formData.get('email')
        const password = formData.get('password')
        const role = formData.get('role')
        setIsSubmitting(true)

        try {
            if (mode === 'sign-in') {
                await signIn(email, password)
                navigate('/', {replace: true})
            } else {
                await signUp(email, password, role)
                setMessage(
                    `Your ${role === 'SUPPLIER' ? 'supplier' : 'customer'} account is ready. You can now sign in.`,
                )
            }
        } catch (error) {
            setMessage(error.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="auth-page">
            <header className="auth-header">
                <Link className="auth-brand" to="/" aria-label="NicheFlow home">
                    <img className="brand-logo" src={nicheFlowLogo} alt="NicheFlow"/>
                </Link>
                <Link className="auth-back-link" to="/">Back to explore</Link>
            </header>

            <main className="auth-main">
                <section className="auth-card" aria-labelledby="auth-heading">
                    <p className="eyebrow">Welcome to NicheFlow</p>
                    <h1 id="auth-heading">{content.heading}</h1>
                    <p className="auth-description">{content.description}</p>
                    {mode !== 'sign-in' && (
                        <p className="auth-preview-note">
                            {mode === 'sign-up'
                                ? 'Choose customer to book listings or supplier to create and manage listings.'
                                : 'Password reset is not connected yet.'}
                        </p>
                    )}

                    <form className="auth-form" onSubmit={handleSubmit}>
                        {content.fields.map((field) => {
                            const details = fieldDetails[field]

                            return (
                                <label className="auth-field" key={field}>
                                    <span>{details.label}</span>
                                    <input
                                        type={details.type}
                                        name={field}
                                        required
                                        minLength={
                                            field === 'password'
                                                ? 8
                                                : undefined
                                        }
                                        autoComplete={
                                            field === 'password' && mode === 'sign-in'
                                                ? 'current-password'
                                                : details.autoComplete
                                        }
                                    />
                                </label>
                            )
                        })}

                        {mode === 'sign-up' && (
                            <label className="auth-field">
                                <span>Account type</span>
                                <select name="role" defaultValue="CUSTOMER" required>
                                    <option value="CUSTOMER">Customer — book listings</option>
                                    <option value="SUPPLIER">Supplier — create listings</option>
                                </select>
                            </label>
                        )}

                        {mode === 'sign-in' && (
                            <Link className="auth-inline-link" to="/forgot-password">
                                Forgot password?
                            </Link>
                        )}

                        <button
                            className="auth-submit-button"
                            type="submit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Please wait…' : content.button}
                        </button>
                    </form>

                    {message && (
                        <p className="auth-status" role="status">
                            {message}
                        </p>
                    )}

                    {mode === 'sign-in' ? (
                        <p className="auth-switch">
                            New to NicheFlow? <Link to="/sign-up">Create an account</Link>
                        </p>
                    ) : mode === 'sign-up' ? (
                        <p className="auth-switch">
                            Already have an account? <Link to="/sign-in">Sign in</Link>
                        </p>
                    ) : (
                        <p className="auth-switch">
                            Remember your password? <Link to="/sign-in">Sign in</Link>
                        </p>
                    )}
                </section>
            </main>
        </div>
    )
}

export default AuthPage
