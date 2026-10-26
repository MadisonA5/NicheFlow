import {useState} from 'react'
import {Link} from 'react-router-dom'
import {getSession} from '../auth/authDataSource'
import {createListing} from './createListingDataSource'

const initialForm = {
    title: '',
    category: '',
    price: '',
    location: '',
    description: '',
}

function CreateListingPage() {
    const session = getSession()
    const [form, setForm] = useState(initialForm)
    const [message, setMessage] = useState('')
    const [createdListing, setCreatedListing] = useState(null)
    const [isSubmitting, setIsSubmitting] = useState(false)

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage('')
        setIsSubmitting(true)

        try {
            const listing = await createListing(form)
            setCreatedListing(listing)
        } catch (error) {
            setMessage(error.message)
        } finally {
            setIsSubmitting(false)
        }
    }

    function handleChange(event) {
        const {name, value} = event.target
        setForm((current) => ({...current, [name]: value}))
        setMessage('')
    }

    if (!session) {
        return (
            <main className="listing-page-message">
                <p role="status">Sign in with a supplier account to create a listing.</p>
                <Link className="text-link" to="/sign-in">Sign in</Link>
            </main>
        )
    }

    if (session.user.role !== 'SUPPLIER') {
        return (
            <main className="listing-page-message">
                <p role="alert">Only supplier accounts can create listings.</p>
                <Link className="text-link" to="/">Back to explore</Link>
            </main>
        )
    }

    return (
        <main className="auth-page">
            <header className="auth-header">
                <Link className="auth-back-link" to="/">← Back to explore</Link>
            </header>
            <section className="auth-main">
                <div className="auth-card create-listing-card">
                    <p className="eyebrow">Share something local</p>
                    <h1 id="create-listing-heading">Create a listing</h1>
                    <p className="auth-description">
                        Add the details customers need to discover your offering.
                    </p>

                    {createdListing ? (
                        <div className="create-listing-success" role="status">
                            <h2>Listing created</h2>
                            <p><strong>{createdListing.title}</strong> is now active.</p>
                            <p>Listing ID: {createdListing.id}</p>
                            <Link className="booking-link" to="/">Back to explore</Link>
                        </div>
                    ) : (
                        <form
                            className="auth-form create-listing-form"
                            aria-labelledby="create-listing-heading"
                            onSubmit={handleSubmit}
                        >
                            <label className="auth-field">
                                <span>Listing title</span>
                                <input
                                    name="title"
                                    value={form.title}
                                    onChange={handleChange}
                                    maxLength={255}
                                    required
                                />
                            </label>
                            <label className="auth-field">
                                <span>Category</span>
                                <select
                                    name="category"
                                    value={form.category}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Choose a category</option>
                                    <option value="Workspace">Workspace</option>
                                    <option value="Outdoor">Outdoor</option>
                                    <option value="Events">Events</option>
                                    <option value="Services">Services</option>
                                    <option value="Other">Other</option>
                                </select>
                            </label>
                            <label className="auth-field">
                                <span>Price per day ($)</span>
                                <input
                                    name="price"
                                    type="number"
                                    min="0"
                                    max="99999999.99"
                                    step="0.01"
                                    value={form.price}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                            <label className="auth-field">
                                <span>Location</span>
                                <input
                                    name="location"
                                    value={form.location}
                                    onChange={handleChange}
                                    maxLength={255}
                                    placeholder="City, province"
                                    required
                                />
                            </label>
                            <label className="auth-field">
                                <span>Description</span>
                                <textarea
                                    name="description"
                                    value={form.description}
                                    onChange={handleChange}
                                    maxLength={2000}
                                    rows={5}
                                    required
                                />
                            </label>
                            <button
                                className="auth-submit-button"
                                type="submit"
                                disabled={isSubmitting}
                            >
                                {isSubmitting ? 'Creating…' : 'Create listing'}
                            </button>
                        </form>
                    )}
                    {message && <p className="booking-validation" role="alert">{message}</p>}
                </div>
            </section>
        </main>
    )
}

export default CreateListingPage
