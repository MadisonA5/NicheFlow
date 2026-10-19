import {useEffect, useState} from 'react'
import {BrowserRouter, Link, Navigate, Route, Routes} from 'react-router-dom'
import nicheFlowLogo from '../assets/nicheflow logo1.png'
import AuthPage from '../features/auth/AuthPage'
import ListingCard from '../features/listings/ListingCard'
import {getListings} from '../features/listings/listingsDataSource'
import {filterListings} from '../features/search/filterListings'
import SearchBar from '../features/search/SearchBar'
import '../styles/App.css'

function HomePage() {
    const [listings, setListings] = useState([])
    const [isLoading, setIsLoading] = useState(true)
    const [loadError, setLoadError] = useState('')
    const [search, setSearch] = useState({
        keyword: '',
        location: '',
        category: '',
        maxPrice: '',
        sortBy: 'featured',
    })
    const [hasSearched, setHasSearched] = useState(false)
    const [searchResetKey, setSearchResetKey] = useState(0)
    const filteredListings = filterListings(listings, search)
    const hasCustomizedResults =
        hasSearched ||
        search.category !== '' ||
        search.maxPrice !== '' ||
        search.sortBy !== 'featured'
    const activeCriteria = [
        search.keyword.trim(),
        search.location.trim(),
        search.category,
        search.maxPrice !== '' ? `Up to $${search.maxPrice}` : '',
        search.sortBy === 'price-ascending'
            ? 'Price: low to high'
            : search.sortBy === 'price-descending'
                ? 'Price: high to low'
                : search.sortBy === 'title'
                    ? 'Name: A to Z'
                    : '',
    ].filter(Boolean)

    useEffect(() => {
        let isCurrent = true

        getListings()
            .then((results) => {
                if (isCurrent) setListings(results)
            })
            .catch(() => {
                if (isCurrent) setLoadError('Listings could not be loaded. Please try again.')
            })
            .finally(() => {
                if (isCurrent) setIsLoading(false)
            })

        return () => {
            isCurrent = false
        }
    }, [])

    function handleSearch(searchCriteria) {
        setSearch((currentSearch) => ({
            ...currentSearch,
            keyword: searchCriteria.keyword,
            location: searchCriteria.location,
        }))
        setHasSearched(true)
    }

    function handleFilterChange(event) {
        const {name, value} = event.target
        setSearch((currentSearch) => ({...currentSearch, [name]: value}))
        setHasSearched(true)
    }

    function clearFilters() {
        setSearch({
            keyword: '',
            location: '',
            category: '',
            maxPrice: '',
            sortBy: 'featured',
        })
        setHasSearched(false)
        setSearchResetKey((key) => key + 1)
    }

    return (
        <>
            <header className="site-header">
                <Link className="brand" to="/" aria-label="NicheFlow home">
                    <img className="brand-logo" src={nicheFlowLogo} alt="NicheFlow"/>
                </Link>

                <nav className="main-nav" aria-label="Main navigation">
                    <a href="#listings">Explore</a>
                    <a href="#how-it-works">How it works</a>
                </nav>

                <Link className="sign-in-button" to="/sign-in">
                    Sign in
                </Link>
            </header>

            <main>
                <section className="welcome-section" aria-labelledby="welcome-heading">
                    <p className="eyebrow">Make room for something local</p>
                    <h1 id="welcome-heading">Find your next favorite place.</h1>
                    <p className="welcome-copy">
                        Discover unique spaces, services, and things to do in your
                        neighborhood.
                    </p>

                    <SearchBar key={searchResetKey} onSearch={handleSearch}/>
                </section>

                <section className="listings-section" id="listings">
                    <div className="section-heading">
                        <div>
                            <p className="eyebrow">Picked for your area</p>
                            <h2>Explore local finds</h2>
                        </div>
                        {hasCustomizedResults && (
                            <button
                                className="text-link clear-filters-button"
                                type="button"
                                onClick={clearFilters}
                            >
                                Clear filters
                            </button>
                        )}
                    </div>

                    <div
                        className="listing-controls"
                        role="group"
                        aria-label="Filter and sort listings"
                    >
                        <label>
                            <span>Category</span>
                            <select
                                name="category"
                                value={search.category}
                                onChange={handleFilterChange}
                            >
                                <option value="">All categories</option>
                                {[...new Set(listings.map((listing) => listing.category))]
                                    .sort((first, second) => first.localeCompare(second))
                                    .map((category) => (
                                        <option key={category} value={category}>{category}</option>
                                    ))}
                            </select>
                        </label>
                        <label>
                            <span>Maximum price per day</span>
                            <input
                                name="maxPrice"
                                type="number"
                                min="0"
                                step="1"
                                placeholder="Any price"
                                value={search.maxPrice}
                                onChange={handleFilterChange}
                            />
                        </label>
                        <label>
                            <span>Sort by</span>
                            <select
                                name="sortBy"
                                value={search.sortBy}
                                onChange={handleFilterChange}
                            >
                                <option value="featured">Featured</option>
                                <option value="price-ascending">Price: low to high</option>
                                <option value="price-descending">Price: high to low</option>
                                <option value="title">Name: A to Z</option>
                            </select>
                        </label>
                    </div>

                    {activeCriteria.length > 0 && (
                        <p className="active-search-criteria">
                            Showing results for: {activeCriteria.join(' · ')}
                        </p>
                    )}

                    {hasCustomizedResults && !isLoading && !loadError && (
                        <p className="search-result-count" aria-live="polite">
                            {filteredListings.length}{' '}
                            {filteredListings.length === 1 ? 'listing' : 'listings'} found
                        </p>
                    )}
                    {isLoading ? (
                        <p className="empty-results" role="status">Loading listings…</p>
                    ) : loadError ? (
                        <p className="empty-results" role="alert">{loadError}</p>
                    ) : filteredListings.length > 0 ? (
                        <div className="listing-grid">
                            {filteredListings.map((listing) => (
                                <ListingCard key={listing.id} listing={listing}/>
                            ))}
                        </div>
                    ) : (
                        <p className="empty-results" role="status">
                            No listings match your search. Try different keywords or location.
                        </p>
                    )}
                </section>

                <section className="how-it-works" id="how-it-works">
                    <p className="eyebrow">A little more local</p>
                    <h2>Good things are closer than you think.</h2>
                    <p>
                        NicheFlow brings neighbors together to share useful spaces,
                        services, and experiences.
                    </p>
                </section>
            </main>

            <footer className="site-footer">
                <span className="brand brand-footer">
                    <img className="brand-logo" src={nicheFlowLogo} alt="NicheFlow"/>
                </span>
                <p>Find it nearby. Make it yours.</p>
            </footer>
        </>
    )
}

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/sign-in" element={<AuthPage mode="sign-in"/>}/>
                <Route path="/sign-up" element={<AuthPage mode="sign-up"/>}/>
                <Route path="/forgot-password" element={<AuthPage mode="forgot-password"/>}/>
                <Route path="*" element={<Navigate to="/" replace/>}/>
            </Routes>
        </BrowserRouter>
    )
}

export default App
