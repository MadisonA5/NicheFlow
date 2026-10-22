import { useState } from 'react'

function SearchBar({ onSearch }) {
  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    onSearch({ keyword, location })
  }

  return (
    <form
      className="search-panel"
      aria-label="Search listings"
      onSubmit={handleSubmit}
    >
      <label className="search-field">
        <span>What are you looking for?</span>
        <input
          type="search"
          placeholder="A quiet workspace..."
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
      </label>
      <label className="search-field location-field">
        <span>Where?</span>
        <input
          type="text"
          placeholder="City or neighborhood"
          value={location}
          onChange={(event) => setLocation(event.target.value)}
        />
      </label>
      <button className="search-button" type="submit">
        Search
      </button>
    </form>
  )
}

export default SearchBar
