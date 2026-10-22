export function filterListings(listings, criteria) {
    const keyword = criteria.keyword.trim().toLowerCase()
    const location = criteria.location.trim().toLowerCase()
    const maxPrice = criteria.maxPrice === '' ? null : Number(criteria.maxPrice)

    const results = listings.filter((listing) => {
        const searchableText = `${listing.title} ${listing.category}`.toLowerCase()

        return (
            searchableText.includes(keyword) &&
            listing.location.toLowerCase().includes(location) &&
            (criteria.category === '' || listing.category === criteria.category) &&
            (maxPrice === null || listing.price <= maxPrice)
        )
    })

    if (criteria.sortBy === 'price-ascending') {
        return results.sort((first, second) => first.price - second.price)
    }
    if (criteria.sortBy === 'price-descending') {
        return results.sort((first, second) => second.price - first.price)
    }
    if (criteria.sortBy === 'title') {
        return results.sort((first, second) => first.title.localeCompare(second.title))
    }

    return results
}
