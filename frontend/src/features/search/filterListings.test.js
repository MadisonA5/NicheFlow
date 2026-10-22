import {describe, expect, it} from 'vitest'
import {filterListings} from './filterListings'

const listings = [
    {title: 'Sunny garden studio', category: 'Workspace', location: 'Calgary, AB', price: 38},
    {title: 'Weekend bike rental', category: 'Outdoor', location: 'Calgary, AB', price: 22},
    {title: 'Backyard dinner spot', category: 'Events', location: 'Airdrie, AB', price: 55},
]

const defaultCriteria = {
    keyword: '',
    location: '',
    category: '',
    maxPrice: '',
    sortBy: 'featured',
}

describe('filterListings', () => {
    it('matches keywords without case sensitivity and across title and category', () => {
        expect(filterListings(listings, {...defaultCriteria, keyword: 'BIKE  '}))
            .toEqual([listings[1]])
        expect(filterListings(listings, {...defaultCriteria, keyword: 'workspace'}))
            .toEqual([listings[0]])
    })

    it('matches location together with keyword regardless of case', () => {
        expect(filterListings(listings, {
            ...defaultCriteria,
            keyword: 'dinner',
            location: ' airdrie ',
        })).toEqual([listings[2]])
    })

    it('filters by exact category', () => {
        expect(filterListings(listings, {
            ...defaultCriteria,
            category: 'Outdoor',
        })).toEqual([listings[1]])
    })

    it('filters by an inclusive maximum price', () => {
        expect(filterListings(listings, {
            ...defaultCriteria,
            maxPrice: '38',
        })).toEqual([listings[0], listings[1]])
    })

    it('sorts by ascending price, descending price, and title', () => {
        expect(filterListings(listings, {...defaultCriteria, sortBy: 'price-ascending'}))
            .toEqual([listings[1], listings[0], listings[2]])
        expect(filterListings(listings, {...defaultCriteria, sortBy: 'price-descending'}))
            .toEqual([listings[2], listings[0], listings[1]])
        expect(filterListings(listings, {...defaultCriteria, sortBy: 'title'}))
            .toEqual([listings[2], listings[0], listings[1]])
    })

    it('preserves featured order when no sort is selected', () => {
        expect(filterListings(listings, defaultCriteria)).toEqual(listings)
    })
})
