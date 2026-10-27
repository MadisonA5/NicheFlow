import {useEffect, useState} from 'react'
import {Link, useParams} from 'react-router-dom'
import {getSession} from '../auth/authDataSource'
import {createBooking, getUnavailablePeriods} from './bookingsDataSource'
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

function dateKey(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

function addOneDay(value) {
    return new Date(Date.parse(`${value}T00:00:00Z`) + 86_400_000)
        .toISOString()
        .slice(0, 10)
}

function overlapsUnavailablePeriod(start, end, periods) {
    if (!start || !end || start >= end) return false
    return periods.some((period) => {
        const bookedStart = period.startTime.slice(0, 10)
        const bookedEnd = period.endTime.slice(0, 10)
        return start < bookedEnd && bookedStart < end
    })
}

function isUnavailableDate(value, periods) {
    return overlapsUnavailablePeriod(value, addOneDay(value), periods)
}

function formatDate(value) {
    if (!value) return 'Choose a date'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeZone: 'UTC',
    }).format(new Date(`${value}T00:00:00Z`))
}

function BookingPage() {
    const {listingId} = useParams()
    const {listing, isLoading, loadError} = useListing(listingId)
    const [checkIn, setCheckIn] = useState('')
    const [checkOut, setCheckOut] = useState('')
    const [availabilityResult, setAvailabilityResult] = useState(null)
    const [calendarMonth, setCalendarMonth] = useState(() => {
        const today = new Date()
        return new Date(today.getFullYear(), today.getMonth(), 1)
    })
    const [message, setMessage] = useState('')
    const [booking, setBooking] = useState(null)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const session = getSession()
    const rentalDays = getRentalDays(checkIn, checkOut)
    const todayKey = getToday()
    const listingIdForAvailability = listing?.id
    const availabilityMatchesListing = Boolean(
        listingIdForAvailability &&
        availabilityResult?.listingId === listingIdForAvailability,
    )
    const unavailablePeriods = availabilityMatchesListing
        ? availabilityResult.periods
        : []
    const availabilityLoading = !availabilityMatchesListing
    const availabilityError = availabilityMatchesListing
        ? availabilityResult.error
        : ''

    useEffect(() => {
        if (!listingIdForAvailability) return undefined
        let isCurrent = true

        getUnavailablePeriods(listingIdForAvailability)
            .then((periods) => {
                if (isCurrent) {
                    setAvailabilityResult({
                        listingId: listingIdForAvailability,
                        periods,
                        error: '',
                    })
                }
            })
            .catch((error) => {
                if (isCurrent) {
                    setAvailabilityResult({
                        listingId: listingIdForAvailability,
                        periods: [],
                        error: error.message,
                    })
                }
            })

        return () => {
            isCurrent = false
        }
    }, [listingIdForAvailability])

    const monthStart = new Date(
        calendarMonth.getFullYear(),
        calendarMonth.getMonth(),
        1,
    )
    const monthLength = new Date(
        calendarMonth.getFullYear(),
        calendarMonth.getMonth() + 1,
        0,
    ).getDate()
    const calendarDays = [
        ...Array((monthStart.getDay() + 6) % 7).fill(null),
        ...Array.from({length: monthLength}, (_, index) =>
            new Date(calendarMonth.getFullYear(),
                calendarMonth.getMonth(), index + 1)),
    ]
    const monthLabel = new Intl.DateTimeFormat(undefined, {
        month: 'long',
        year: 'numeric',
    }).format(calendarMonth)
    const selectedRangeUnavailable = overlapsUnavailablePeriod(
        checkIn, checkOut, unavailablePeriods,
    )

    function chooseDate(value) {
        setMessage('')
        if (!checkIn || checkOut || value <= checkIn) {
            setCheckIn(value)
            setCheckOut('')
        } else {
            setCheckOut(value)
        }
    }

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
                checkIn,
                checkOut,
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
                        Choose available dates to see an estimated total.
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
                        <div className="booking-calendar" role="group"
                            aria-label="Choose booking dates">
                            <div className="booking-calendar-heading">
                                <button
                                    className="account-action-button"
                                    type="button"
                                    aria-label="Previous month"
                                    disabled={calendarMonth.getFullYear() ===
                                        new Date(`${todayKey}T00:00:00`).getFullYear() &&
                                        calendarMonth.getMonth() <=
                                        new Date(`${todayKey}T00:00:00`).getMonth()}
                                    onClick={() => setCalendarMonth((month) =>
                                        new Date(month.getFullYear(),
                                            month.getMonth() - 1, 1))}
                                >
                                    ←
                                </button>
                                <h2>{monthLabel}</h2>
                                <button
                                    className="account-action-button"
                                    type="button"
                                    aria-label="Next month"
                                    onClick={() => setCalendarMonth((month) =>
                                        new Date(month.getFullYear(),
                                            month.getMonth() + 1, 1))}
                                >
                                    →
                                </button>
                            </div>
                            {availabilityLoading ? (
                                <p role="status">Checking availability…</p>
                            ) : availabilityError ? (
                                <p className="booking-validation" role="alert">
                                    Could not load availability: {availabilityError}
                                </p>
                            ) : (
                                <>
                                    <div className="booking-calendar-grid booking-calendar-weekdays"
                                        aria-hidden="true">
                                        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
                                            .map((day) => <span key={day}>{day}</span>)}
                                    </div>
                                    <div className="booking-calendar-grid">
                                        {calendarDays.map((day, index) => {
                                            if (!day) {
                                                return <span key={`blank-${index}`}
                                                    aria-hidden="true"/>
                                            }
                                            const value = dateKey(day)
                                            const unavailable =
                                                isUnavailableDate(value, unavailablePeriods)
                                            const choosingCheckout =
                                                Boolean(checkIn && !checkOut && value > checkIn)
                                            const overlap = choosingCheckout &&
                                                overlapsUnavailablePeriod(
                                                    checkIn, value, unavailablePeriods,
                                                )
                                            const selected = value === checkIn ||
                                                value === checkOut
                                            const inRange = checkIn && checkOut &&
                                                value >= checkIn && value < checkOut
                                            const canUseAsCheckoutBoundary =
                                                choosingCheckout && !overlap
                                            const disabled = value < todayKey ||
                                                Boolean(booking) ||
                                                (!choosingCheckout && unavailable) ||
                                                (choosingCheckout && overlap)
                                            const classes = [
                                                'booking-calendar-day',
                                                unavailable && 'unavailable',
                                                selected && 'selected',
                                                inRange && 'in-range',
                                            ].filter(Boolean).join(' ')
                                            return (
                                                <button
                                                    className={classes}
                                                    type="button"
                                                    key={value}
                                                    aria-label={`${value}${unavailable
                                                        ? canUseAsCheckoutBoundary
                                                            ? ', unavailable for stay but available as checkout'
                                                            : ', unavailable'
                                                        : ''}`}
                                                    aria-pressed={Boolean(selected)}
                                                    disabled={disabled}
                                                    onClick={() => chooseDate(value)}
                                                >
                                                    {day.getDate()}
                                                </button>
                                            )
                                        })}
                                    </div>
                                    <div className="booking-calendar-selection">
                                        <p>Check-in <strong>{formatDate(checkIn)}</strong></p>
                                        <p>Check-out <strong>{formatDate(checkOut)}</strong></p>
                                    </div>
                                    <p className="booking-calendar-legend">
                                        <span aria-hidden="true"/> Unavailable dates are shaded.
                                    </p>
                                </>
                            )}
                        </div>
                        {checkIn && checkOut && rentalDays === 0 && (
                            <p className="booking-validation" role="alert">
                                Check-out must be after check-in.
                            </p>
                        )}
                        {selectedRangeUnavailable && (
                            <p className="booking-validation" role="alert">
                                Your selected dates include unavailable time.
                            </p>
                        )}
                        <button
                            className="auth-submit-button"
                            type="submit"
                            disabled={availabilityLoading || Boolean(availabilityError) ||
                                selectedRangeUnavailable || rentalDays === 0 || !session ||
                                session.user.role !== 'CUSTOMER' || isSubmitting || Boolean(booking)}
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
