import {useEffect, useState} from 'react'
import {Link} from 'react-router-dom'
import {getSession} from '../auth/authDataSource'
import {deactivateListing, getAccount, updateListing} from './accountDataSource'

const categories = ['Workspace', 'Outdoor', 'Events', 'Services', 'Other']

function formatDateTime(value) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(new Date(value))
}

function AccountPage() {
    const session = getSession()
    const [account, setAccount] = useState(null)
    const [isLoading, setIsLoading] = useState(true)
    const [loadError, setLoadError] = useState('')
    const [requiresSignIn, setRequiresSignIn] = useState(false)
    const [editingListingId, setEditingListingId] = useState(null)
    const [editForm, setEditForm] = useState(null)
    const [confirmDeactivateId, setConfirmDeactivateId] = useState(null)
    const [workingListingId, setWorkingListingId] = useState(null)
    const [actionError, setActionError] = useState('')

    useEffect(() => {
        let isCurrent = true
        getAccount()
            .then((result) => {
                if (isCurrent) setAccount(result)
            })
            .catch((error) => {
                if (isCurrent) {
                    setLoadError(error.message)
                    setRequiresSignIn(error.status === 401)
                }
            })
            .finally(() => {
                if (isCurrent) setIsLoading(false)
            })

        return () => {
            isCurrent = false
        }
    }, [])

    function startEditing(listing) {
        setEditingListingId(listing.id)
        setEditForm({
            title: listing.title,
            category: listing.category,
            price: String(listing.price),
            location: listing.location,
            description: listing.description,
        })
        setConfirmDeactivateId(null)
        setActionError('')
    }

    function handleEditChange(event) {
        const {name, value} = event.target
        setEditForm((current) => ({...current, [name]: value}))
    }

    async function handleSaveListing(event, listingId) {
        event.preventDefault()
        setWorkingListingId(listingId)
        setActionError('')
        try {
            const updated = await updateListing(listingId, editForm)
            setAccount((current) => ({
                ...current,
                listings: current.listings.map((listing) =>
                    listing.id === listingId ? updated : listing),
            }))
            setEditingListingId(null)
            setEditForm(null)
        } catch (error) {
            setActionError(error.message)
        } finally {
            setWorkingListingId(null)
        }
    }

    async function handleDeactivateListing(listingId) {
        setWorkingListingId(listingId)
        setActionError('')
        try {
            const updated = await deactivateListing(listingId)
            setAccount((current) => ({
                ...current,
                listings: current.listings.map((listing) =>
                    listing.id === listingId ? updated : listing),
            }))
            setConfirmDeactivateId(null)
        } catch (error) {
            setActionError(error.message)
        } finally {
            setWorkingListingId(null)
        }
    }

    if (!session) {
        return (
            <main className="listing-page-message">
                <p role="status">Sign in to view your account.</p>
                <Link className="text-link" to="/sign-in">Sign in</Link>
            </main>
        )
    }

    return (
        <main className="account-page">
            <div className="account-heading">
                <Link className="back-link" to="/">← Back to explore</Link>
                <p className="eyebrow">Your NicheFlow activity</p>
                <h1>My account</h1>
                <p className="account-email">{session.user.email}</p>
            </div>

            {isLoading ? (
                <p className="empty-results" role="status">Loading your account…</p>
            ) : loadError ? (
                <>
                    <p className="empty-results" role="alert">{loadError}</p>
                    {requiresSignIn && (
                        <Link className="text-link" to="/sign-in">Sign in again</Link>
                    )}
                </>
            ) : (
                <>
                    {session.user.role === 'SUPPLIER' && (
                        <section className="account-section" aria-labelledby="my-listings-heading">
                            <div className="account-section-heading">
                                <h2 id="my-listings-heading">My listings</h2>
                                <Link className="booking-link" to="/listings/new">
                                    Create listing
                                </Link>
                            </div>
                            {account.listings.length === 0 ? (
                                <p className="empty-results" role="status">
                                    You haven’t created any listings yet.
                                </p>
                            ) : (
                                <div className="account-list">
                                    {account.listings.map((listing) => (
                                        <article className="account-item" key={listing.id}>
                                            {editingListingId === listing.id ? (
                                                <form
                                                    className="account-edit-form"
                                                    aria-label={`Edit ${listing.title}`}
                                                    onSubmit={(event) =>
                                                        handleSaveListing(event, listing.id)}
                                                >
                                                    <label className="auth-field">
                                                        <span>Listing title</span>
                                                        <input name="title" value={editForm.title}
                                                            maxLength={255} required
                                                            onChange={handleEditChange}/>
                                                    </label>
                                                    <label className="auth-field">
                                                        <span>Category</span>
                                                        <select name="category" value={editForm.category}
                                                            required onChange={handleEditChange}>
                                                            {!categories.includes(editForm.category) && (
                                                                <option value={editForm.category}>
                                                                    {editForm.category}
                                                                </option>
                                                            )}
                                                            {categories.map((category) => (
                                                                <option key={category} value={category}>
                                                                    {category}
                                                                </option>
                                                            ))}
                                                        </select>
                                                    </label>
                                                    <label className="auth-field">
                                                        <span>Price per day ($)</span>
                                                        <input name="price" type="number" min="0"
                                                            max="99999999.99" step="0.01"
                                                            value={editForm.price} required
                                                            onChange={handleEditChange}/>
                                                    </label>
                                                    <label className="auth-field">
                                                        <span>Location</span>
                                                        <input name="location" value={editForm.location}
                                                            maxLength={255} required
                                                            onChange={handleEditChange}/>
                                                    </label>
                                                    <label className="auth-field">
                                                        <span>Description</span>
                                                        <textarea name="description"
                                                            value={editForm.description}
                                                            maxLength={2000} rows={4} required
                                                            onChange={handleEditChange}/>
                                                    </label>
                                                    <div className="account-listing-actions">
                                                        <button className="booking-link" type="submit"
                                                            disabled={workingListingId === listing.id}>
                                                            {workingListingId === listing.id
                                                                ? 'Saving…'
                                                                : 'Save changes'}
                                                        </button>
                                                        <button className="account-action-button"
                                                            type="button"
                                                            onClick={() => {
                                                                setEditingListingId(null)
                                                                setEditForm(null)
                                                                setActionError('')
                                                            }}>
                                                            Cancel
                                                        </button>
                                                    </div>
                                                </form>
                                            ) : (
                                                <>
                                                    <div>
                                                        <p className="listing-category">
                                                            {listing.category}
                                                        </p>
                                                        <h3>{listing.title}</h3>
                                                        <p>{listing.location} · ${listing.price} / day</p>
                                                    </div>
                                                    <div className="account-listing-controls">
                                                        <span className={listing.active
                                                            ? 'account-status active'
                                                            : 'account-status'}>
                                                            {listing.active ? 'Active' : 'Inactive'}
                                                        </span>
                                                        <div className="account-listing-actions">
                                                            <button className="account-action-button"
                                                                type="button"
                                                                onClick={() => startEditing(listing)}>
                                                                Edit
                                                            </button>
                                                            {listing.active && (
                                                                <button
                                                                    className="account-action-button danger"
                                                                    type="button"
                                                                    onClick={() => {
                                                                        setConfirmDeactivateId(listing.id)
                                                                        setActionError('')
                                                                    }}>
                                                                    Deactivate
                                                                </button>
                                                            )}
                                                        </div>
                                                        {confirmDeactivateId === listing.id && (
                                                            <div className="account-deactivate-confirm">
                                                                <p>This hides the listing from customers.</p>
                                                                <button
                                                                    className="account-action-button danger"
                                                                    type="button"
                                                                    disabled={workingListingId === listing.id}
                                                                    onClick={() =>
                                                                        handleDeactivateListing(listing.id)}>
                                                                    {workingListingId === listing.id
                                                                        ? 'Deactivating…'
                                                                        : 'Confirm deactivate'}
                                                                </button>
                                                                <button className="account-action-button"
                                                                    type="button"
                                                                    onClick={() =>
                                                                        setConfirmDeactivateId(null)}>
                                                                    Keep active
                                                                </button>
                                                            </div>
                                                        )}
                                                    </div>
                                                </>
                                            )}
                                        </article>
                                    ))}
                                </div>
                            )}
                            {actionError && (
                                <p className="booking-validation" role="alert">{actionError}</p>
                            )}
                        </section>
                    )}

                    <section className="account-section" aria-labelledby="my-bookings-heading">
                        <h2 id="my-bookings-heading">
                            {session.user.role === 'SUPPLIER' ? 'Bookings on my listings' : 'My bookings'}
                        </h2>
                        {account.bookings.length === 0 ? (
                            <p className="empty-results" role="status">
                                {session.user.role === 'SUPPLIER'
                                    ? 'Your listings don’t have any booking requests yet.'
                                    : 'You haven’t made any bookings yet.'}
                            </p>
                        ) : (
                            <div className="account-list">
                                {account.bookings.map((booking) => (
                                    <article className="account-item" key={booking.id}>
                                        <div>
                                            <p className="listing-category">
                                                Booking #{booking.id}
                                                {session.user.role === 'SUPPLIER' &&
                                                    ` · Customer #${booking.customerId}`}
                                            </p>
                                            <h3>{booking.listingTitle}</h3>
                                            <p>{booking.location} · ${booking.price} / day</p>
                                            <p>
                                                {formatDateTime(booking.startTime)} –{' '}
                                                {formatDateTime(booking.endTime)}
                                            </p>
                                        </div>
                                        <span className="account-status">{booking.status.toLowerCase()}</span>
                                    </article>
                                ))}
                            </div>
                        )}
                    </section>
                </>
            )}
        </main>
    )
}

export default AccountPage
