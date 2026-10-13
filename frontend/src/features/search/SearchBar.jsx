function SearchBar() {
  return (
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
  )
}

export default SearchBar
