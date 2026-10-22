import {useState} from 'react'
import {Link, useParams} from 'react-router-dom'
import useListing from './useListing'

function getToday() {
    const today = new Date()
    const offset = today.getTimezoneOffset()
    return new Date(today.getTime() - offset * 60 * 1000)
        .toISOString()
        .slice(0, 10)
}

function getRentalDays(checkIn, checkOut) {
    if (!checkIn || !checkOut) return 0
    const start = Date.parse(`${checkIn}T00:00:00Z`)
    const end = Date.parse(`${checkOut}T00:00:00Z`)
    return Math.max(0, Math.round((end - start) / 86_400_000))
}

function BookingPage() {
    const {listingId} = useParams()
    const {listing, isLoading, loadError} = useListing(listingId)
    const [checkIn, setCheckIn] = useState('')
    const [checkOut, setCheckOut] = useState('')
    const [guestName, setGuestName] = useState('')
    const [message, setMessage] = useState('')
    const rentalDays = getRentalDays(checkIn, checkOut)

    if (isLoading) {
        return <p className="listing-page-message" role="status">Loading booking details…</p>
    }
    if (loadError) {
        return (
            <main className="listing-page-message">
                <p role="alert">{loadError}</p>
                <Link className="text-link" to="/">Back to explore</Link>
            </main>
        )
    }

    function handleSubmit(event) {
        event.preventDefault()
        setMessage(
            'This is a booking preview only. No reservation was created and no information was sent.',
        )
    }

    return (
        <main className="listing-page booking-page">
            <Link className="back-link" to={`/listings/${listing.id}`}>
                ← Back to listing
            </Link>
            <div className="booking-layout">
                <section className="booking-card" aria-labelledby="booking-heading">
                    <p className="eyebrow">Plan your visit</p>
                    <h1 id="booking-heading">Request a booking</h1>
                    <p className="auth-description">
                        Choose your dates to see an estimated total.
                    </p>
                    <p className="preview-note">
                        Preview only — availability and reservations aren’t connected.
                    </p>

                    <form className="booking-form" onSubmit={handleSubmit}>
                        <label className="auth-field">
                            <span>Your name</span>
                            <input
                                name="guestName"
                                type="text"
                                autoComplete="name"
                                value={guestName}
                                onChange={(event) => setGuestName(event.target.value)}
                                required
                            />
                        </label>
                        <div className="booking-dates">
                            <label className="auth-field">
                                <span>Check-in</span>
                                <input
                                    name="checkIn"
                                    type="date"
                                    min={getToday()}
                                    value={checkIn}
                                    onChange={(event) => {
                                        setCheckIn(event.target.value)
                                        setMessage('')
                                    }}
                                    required
                                />
                            </label>
                            <label className="auth-field">
                                <span>Check-out</span>
                                <input
                                    name="checkOut"
                                    type="date"
                                    min={checkIn || getToday()}
                                    value={checkOut}
                                    onChange={(event) => {
                                        setCheckOut(event.target.value)
                                        setMessage('')
                                    }}
                                    required
                                />
                            </label>
                        </div>
                        {checkIn && checkOut && rentalDays === 0 && (
                            <p className="booking-validation" role="alert">
                                Check-out must be after check-in.
                            </p>
                        )}
                        <button
                            className="auth-submit-button"
                            type="submit"
                            disabled={rentalDays === 0}
                        >
                            Continue
                        </button>
                    </form>
                    {message && <p className="auth-status" role="status">{message}</p>}
                </section>

                <aside className="booking-summary" aria-label="Booking estimate">
                    <div className={`booking-summary-art ${listing.color}`} aria-hidden="true">
                        <span>{listing.icon}</span>
                    </div>
                    <p className="listing-category">{listing.category}</p>
                    <h2>{listing.title}</h2>
                    <p className="listing-location">{listing.location}</p>
                    <div className="booking-price-line">
                        <span>${listing.price} × {rentalDays || '—'} {rentalDays === 1 ? 'day' : 'days'}</span>
                        <strong>{rentalDays ? `$${listing.price * rentalDays}` : '—'}</strong>
                    </div>
                    <p className="booking-total">
                        <span>Estimated total</span>
                        <strong>{rentalDays ? `$${listing.price * rentalDays}` : 'Choose dates'}</strong>
                    </p>
                    <p className="booking-estimate-note">
                        Estimate only. Taxes, availability, and final pricing aren’t included.
                    </p>
                </aside>
            </div>
        </main>
    )
}

export default BookingPage
