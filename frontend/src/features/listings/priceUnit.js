// Issue #14: a listing price buys one day or one hour of the listing.
export function priceUnitLabel(priceUnit) {
    return priceUnit === 'HOUR' ? 'hour' : 'day'
}

export function isHourly(listing) {
    return listing?.priceUnit === 'HOUR'
}
