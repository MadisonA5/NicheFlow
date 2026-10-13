import './App.css'

const listings = [
  {
    id: 1,
    title: 'Sunny garden studio',
    category: 'Workspace',
    location: 'Calgary, AB',
    price: 38,
    icon: '🌿',
    color: 'sage',
  },
  {
    id: 2,
    title: 'Weekend bike rental',
    category: 'Outdoor',
    location: 'Calgary, AB',
    price: 22,
    icon: '🚲',
    color: 'peach',
  },
  {
    id: 3,
    title: 'Backyard dinner spot',
    category: 'Events',
    location: 'Airdrie, AB',
    price: 55,
    icon: '🍽️',
    color: 'lavender',
  },
]

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

          <div className="search-panel" aria-label="Search listings">
            <label className="search-field">
              <span>What are you looking for?</span>
              <input type="search" placeholder="A quiet workspace..." />
            </label>
            <label className="search-field location-field">
              <span>Where?</span>
              <input type="text" placeholder="City or neighborhood" />
            </label>
            <button className="search-button" type="button">
              Search
            </button>
          </div>
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
              <article className="listing-card" key={listing.id}>
                <div
                  className={`listing-art ${listing.color}`}
                  aria-hidden="true"
                >
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
