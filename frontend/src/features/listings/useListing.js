import {useEffect, useState} from 'react'
import {getListings} from './listingsDataSource'

function useListing(listingId) {
    const [listing, setListing] = useState(null)
    const [isLoading, setIsLoading] = useState(true)
    const [loadError, setLoadError] = useState('')

    useEffect(() => {
        let isCurrent = true

        getListings()
            .then((results) => {
                if (!isCurrent) return
                const result = results.find((item) => String(item.id) === listingId)
                if (result) {
                    setListing(result)
                } else {
                    setLoadError('This listing could not be found.')
                }
            })
            .catch(() => {
                if (isCurrent) {
                    setLoadError('Listings could not be loaded. Please try again.')
                }
            })
            .finally(() => {
                if (isCurrent) setIsLoading(false)
            })

        return () => {
            isCurrent = false
        }
    }, [listingId])

    return {listing, isLoading, loadError}
}

export default useListing
