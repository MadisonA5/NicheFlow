import {useEffect, useState} from 'react'
import {Link, useParams} from 'react-router-dom'
import {getSession} from '../auth/authDataSource'
import {createBooking, getUnavailablePeriods} from './bookingsDataSource'
import {
    formatDateTimeInZone,
    getNextLocalHour,
    localDateTimeToInstant,
} from './timeZone'
import useListing from './useListing'

function overlapsUnavailablePeriod(start, end, periods) {
    if (!start || !end || end <= start) return false
    return periods.some((period) =>
        start < Date.parse(period.endTime) &&
        Date.parse(period.startTime) < end)
}

function BookingPage() {
    const {listingId} = useParams()
    const {listing, isLoading, loadError} = useListing(listingId)
    const [startTime, setStartTime] = useState('')
    const [endTime, setEndTime] = useState('')
    const [availabilityResult, setAvailabilityResult] = useState(null)
    const [message, setMessage] = useState('')
    const [booking, setBooking] = useState(null)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const session = getSession()
    const availabilityMatchesListing = Boolean(
        listing?.id && availabilityResult?.listingId === listing.id,
    )
    const unavailablePeriods = availabilityMatchesListing
        ? availabilityResult.periods
        : []
    const availabilityLoading = !availabilityMatchesListing
    const availabilityError = availabilityMatchesListing
        ? availabilityResult.error
        : ''
    let selectedStart = null
    let selectedEnd = null
    let selectionError = ''

    if (listing && startTime && endTime) {
        try {
            selectedStart = localDateTimeToInstant(startTime, listing.timeZone)
            selectedEnd = localDateTimeToInstant(
                endTime, listing.timeZone, selectedStart.toISOString(),
            )
        } catch (error) {
            selectionError = error.message
        }
    }

    const startInstant = selectedStart?.getTime()
    const endInstant = selectedEnd?.getTime()
    const selectedRangeUnavailable = overlapsUnavailablePeriod(
        startInstant, endInstant, unavailablePeriods,
    )
    const bookedHours = startInstant !== undefined && endInstant !== undefined
        ? (endInstant - startInstant) / 3_600_000
        : 0
    const estimatedTotal = bookedHours > 0
        ? (Number(listing.price) * bookedHours).toFixed(2)
        : ''

    useEffect(() => {
        if (!listing?.id) return undefined
        let isCurrent = true

        getUnavailablePeriods(listing.id)
            .then((periods) => {
                if (isCurrent) {
                    setAvailabilityResult({
                        listingId: listing.id,
                        periods,
                        error: '',
                    })
                }
            })
            .catch((error) => {
                if (isCurrent) {
                    setAvailabilityResult({
                        listingId: listing.id,
                        periods: [],
                        error: error.message,
                    })
                }
            })

        return () => {
            isCurrent = false
        }
    }, [listing?.id])

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

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage('')
        setBooking(null)
        setIsSubmitting(true)

        try {
            const createdBooking = await createBooking({
                listingId: listing.id,
                startTime: selectedStart.toISOString(),
                endTime: selectedEnd.toISOString(),
            })
            setBooking(createdBooking)
            setMessage(`Booking request submitted. Its current status is ${createdBooking.status.toLowerCase()}.`)
        } catch (error) {
            setMessage(error.message)
        } finally {
            setIsSubmitting(false)
        }
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
                        Select a start and end time. Times are shown in {listing.timeZone}.
                    </p>
                    {!session ? (
                        <p className="preview-note">
                            Sign in with a customer account to request this booking.{' '}
                            <Link to="/sign-in">Sign in</Link>
                        </p>
                    ) : session.user.role !== 'CUSTOMER' ? (
                        <p className="preview-note">
                            Only customer accounts can request bookings.
                        </p>
                    ) : (
                        <p className="preview-note">
                            Submitting creates a pending request. Payment isn’t connected yet.
                        </p>
                    )}

                    <form className="booking-form" onSubmit={handleSubmit}>
                        {availabilityLoading ? (
                            <p role="status">Checking availability…</p>
                        ) : availabilityError ? (
                            <p className="booking-validation" role="alert">
                                Could not load availability: {availabilityError}
                            </p>
                        ) : (
                            <>
                                <label className="auth-field">
                                    <span>Start time ({listing.timeZone})</span>
                                    <input
                                        aria-label="Start time"
                                        type="datetime-local"
                                        step="3600"
                                        min={getNextLocalHour(listing.timeZone)}
                                        value={startTime}
                                        onChange={(event) => {
                                            setStartTime(event.target.value)
                                            setMessage('')
                                        }}
                                        required
                                    />
                                </label>
                                <label className="auth-field">
                                    <span>End time ({listing.timeZone})</span>
                                    <input
                                        aria-label="End time"
                                        type="datetime-local"
                                        step="3600"
                                        min={startTime || getNextLocalHour(listing.timeZone)}
                                        value={endTime}
                                        onChange={(event) => {
                                            setEndTime(event.target.value)
                                            setMessage('')
                                        }}
                                        required
                                    />
                                </label>
                            </>
                        )}
                        {selectionError && (
                            <p className="booking-validation" role="alert">
                                {selectionError}
                            </p>
                        )}
                        {selectedRangeUnavailable && (
                            <p className="booking-validation" role="alert">
                                Your selected time overlaps an unavailable booking.
                            </p>
                        )}
                        <button
                            className="auth-submit-button"
                            type="submit"
                            disabled={availabilityLoading || Boolean(availabilityError) ||
                                !selectedStart || !selectedEnd || Boolean(selectionError) ||
                                selectedRangeUnavailable || !session ||
                                session.user.role !== 'CUSTOMER' || isSubmitting ||
                                Boolean(booking)}
                        >
                            {isSubmitting ? 'Submitting…' : booking ? 'Request submitted' : 'Request booking'}
                        </button>
                    </form>
                    {message && (
                        <p className={booking ? 'auth-status' : 'booking-validation'}
                            role={booking ? 'status' : 'alert'}>
                            {message}
                            {booking && ` (Request #${booking.id})`}
                        </p>
                    )}
                </section>

                <aside className="booking-summary" aria-label="Booking estimate">
                    <div className={`booking-summary-art ${listing.color}`} aria-hidden="true">
                        <span>{listing.icon}</span>
                    </div>
                    <p className="listing-category">{listing.category}</p>
                    <h2>{listing.title}</h2>
                    <p className="listing-location">{listing.location}</p>
                    <p className="listing-location">Time zone: {listing.timeZone}</p>
                    <div className="booking-price-line">
                        <span>${listing.price} × {bookedHours || '—'} {bookedHours === 1 ? 'hour' : 'hours'}</span>
                        <strong>{estimatedTotal ? `$${estimatedTotal}` : '—'}</strong>
                    </div>
                    {selectedStart && selectedEnd && !selectionError && (
                        <p className="booking-estimate-note">
                            {formatDateTimeInZone(selectedStart, listing.timeZone)} –{' '}
                            {formatDateTimeInZone(selectedEnd, listing.timeZone)}
                        </p>
                    )}
                    <p className="booking-total">
                        <span>Estimated total</span>
                        <strong>{estimatedTotal ? `$${estimatedTotal}` : 'Choose times'}</strong>
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
