import {Link, useParams} from 'react-router-dom'
import useListing from './useListing'

function ListingDetailPage() {
    const {listingId} = useParams()
    const {listing, isLoading, loadError} = useListing(listingId)

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
                    <p className="preview-note">
                        Sample listing preview — booking isn’t connected yet.
                    </p>
                </div>
            </article>
        </main>
    )
}

export default ListingDetailPage
