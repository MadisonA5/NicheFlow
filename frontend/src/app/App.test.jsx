import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.jsx'

describe('NicheFlow landing page', () => {
  it('shows the welcome message and labeled search fields', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: 'Find your next favorite place.' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('searchbox', { name: 'What are you looking for?' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', { name: 'Where?' }),
    ).toBeInTheDocument()
  })

  it('shows all three local listings with their locations and prices', () => {
    render(<App />)

    const listings = screen.getAllByRole('article')
    expect(listings).toHaveLength(3)

    expect(
      within(listings[0]).getByRole('heading', {
        name: 'Sunny garden studio',
      }),
    ).toBeInTheDocument()
    expect(within(listings[0]).getByText('Calgary, AB')).toBeInTheDocument()
    expect(within(listings[0]).getByText('$38')).toBeInTheDocument()

    expect(
      within(listings[1]).getByRole('heading', {
        name: 'Weekend bike rental',
      }),
    ).toBeInTheDocument()
    expect(
      within(listings[2]).getByRole('heading', {
        name: 'Backyard dinner spot',
      }),
    ).toBeInTheDocument()
    expect(within(listings[2]).getByText('Airdrie, AB')).toBeInTheDocument()
    expect(within(listings[2]).getByText('$55')).toBeInTheDocument()
  })
})
