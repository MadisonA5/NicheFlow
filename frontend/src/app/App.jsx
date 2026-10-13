import ListingCard from '../features/listings/ListingCard'
import { listings } from '../features/listings/listings'
import SearchBar from '../features/search/SearchBar'
import '../styles/App.css'

function App() {
  return (
    <>
      <header className="site-header">
        <a className="brand" href="/" aria-label="NicheFlow home">
          <span className="brand-mark">N</span>
          NicheFlow
        </a>

        <nav className="main-nav" aria-label="Main navigation">
          <a href="#listings">Explore</a>
          <a href="#how-it-works">How it works</a>
        </nav>

        <button className="sign-in-button" type="button">
          Sign in
        </button>
      </header>

      <main>
        <section className="welcome-section" aria-labelledby="welcome-heading">
          <p className="eyebrow">Make room for something local</p>
          <h1 id="welcome-heading">Find your next favorite place.</h1>
          <p className="welcome-copy">
            Discover unique spaces, services, and things to do in your
            neighborhood.
          </p>

          <SearchBar />
        </section>

        <section className="listings-section" id="listings">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Picked for your area</p>
              <h2>Explore local finds</h2>
            </div>
            <a className="text-link" href="#listings">
              See all
            </a>
          </div>

          <div className="listing-grid">
            {listings.map((listing) => (
              <ListingCard key={listing.id} listing={listing} />
            ))}
          </div>
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
          <span className="brand-mark">N</span>
          NicheFlow
        </span>
        <p>Find it nearby. Make it yours.</p>
      </footer>
    </>
  )
}

export default App
