import {useEffect, useState} from 'react'
import {Link, useParams} from 'react-router-dom'
import useListing from './useListing'
import {getSession} from '../auth/authDataSource'
import {listReviewsForListing} from '../reviews/reviewsDataSource'

function ListingDetailPage() {
    const {listingId} = useParams()
    const session = getSession()
    const {listing, isLoading, loadError} = useListing(listingId)
    const [reviews, setReviews] = useState([])
    const [reviewsError, setReviewsError] = useState('')

    useEffect(() => {
        if (!listing) {
            return
        }

        let isCurrent = true
        setReviews([])
        setReviewsError('')
        listReviewsForListing(listing.id)
            .then((result) => {
                if (isCurrent) setReviews(result)
            })
            .catch((error) => {
                if (isCurrent) setReviewsError(error.message)
            })

        return () => {
            isCurrent = false
        }
    }, [listing])

    if (isLoading) {
        return <p className="listing-page-message" role="status">Loading listing…</p>
    }
    if (loadError) {
        return (
            <main className="listing-page-message">
                <p role="alert">{loadError}</p>
                <Link className="text-link" to="/">Back to explore</Link>
            </main>
        )
    }

    return (
        <main className="listing-page">
            <Link className="back-link" to="/">← Back to explore</Link>
            <article className="listing-detail">
                <div className={`listing-detail-art ${listing.color}`} aria-hidden="true">
                    <span>{listing.icon}</span>
                    <span className="art-sparkle">✳</span>
                </div>
                <div className="listing-detail-content">
                    <p className="listing-category">{listing.category}</p>
                    <h1>{listing.title}</h1>
                    <p className="listing-detail-location">{listing.location}</p>
                    <p className="listing-rating">
                        {listing.averageRating != null
                            ? `★ ${listing.averageRating.toFixed(1)} (${listing.reviewCount} reviews)`
                            : 'No reviews yet'}
                    </p>
                    <p className="listing-description">{listing.description}</p>
                    <div className="listing-detail-bottom">
                        <p className="listing-price">
                            <strong>${listing.price}</strong> / day
                        </p>
                        <Link
                            className="booking-link"
                            to={`/listings/${listing.id}/book`}
                        >
                            Check dates
                        </Link>
                    </div>
                </div>
            </article>

            <section className="listing-reviews-section" aria-labelledby="listing-reviews-heading">
                <h2 id="listing-reviews-heading">What customers are saying</h2>
                {reviewsError ? (
                    <p className="empty-results" role="alert">{reviewsError}</p>
                ) : reviews.length === 0 ? (
                    <p className="empty-results" role="status">No reviews yet.</p>
                ) : (
                    <ul className="listing-reviews-list">
                        {reviews.map((review) => (
                            <li key={review.id}>
                                <span className="listing-rating">
                                    {'★'.repeat(review.rating)}
                                    {'☆'.repeat(5 - review.rating)}
                                    {review.reviewerId === session?.user?.id && (
                                        <span className="account-reviews-own-badge">
                                            Your review
                                        </span>
                                    )}
                                </span>
                                {review.comment && <p>{review.comment}</p>}
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </main>
    )
}

export default ListingDetailPage
