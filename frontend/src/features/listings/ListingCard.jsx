function ListingCard({ listing }) {
  return (
    <article className="listing-card">
      <div className={`listing-art ${listing.color}`} aria-hidden="true">
        <span>{listing.icon}</span>
        <span className="art-sparkle">✳</span>
      </div>
      <div className="listing-details">
        <p className="listing-category">{listing.category}</p>
        <h3>{listing.title}</h3>
        <p className="listing-location">{listing.location}</p>
        <p className="listing-price">
          <strong>${listing.price}</strong> / day
        </p>
      </div>
    </article>
  )
}

export default ListingCard
