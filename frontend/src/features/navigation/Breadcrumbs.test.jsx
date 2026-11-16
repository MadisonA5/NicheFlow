import {render, screen, within} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import {describe, expect, it} from 'vitest'
import Breadcrumbs from './Breadcrumbs'

function renderBreadcrumbs(items) {
    return render(
        <MemoryRouter>
            <Breadcrumbs items={items}/>
        </MemoryRouter>,
    )
}

describe('Breadcrumbs', () => {
    it('links every item except the current page', () => {
        renderBreadcrumbs([
            {label: 'Explore', to: '/'},
            {label: 'Kayak rental', to: '/listings/3'},
            {label: 'Book'},
        ])

        const trail = screen.getByRole('navigation', {name: 'Breadcrumb'})
        const items = within(trail).getAllByRole('listitem')
        expect(items).toHaveLength(3)
        expect(within(trail).getByRole('link', {name: 'Explore'}))
            .toHaveAttribute('href', '/')
        expect(within(trail).getByRole('link', {name: 'Kayak rental'}))
            .toHaveAttribute('href', '/listings/3')
        expect(within(trail).queryByRole('link', {name: 'Book'}))
            .not.toBeInTheDocument()
        expect(within(items[2]).getByText('Book'))
            .toHaveAttribute('aria-current', 'page')
    })

    it('marks only the last item as the current page', () => {
        renderBreadcrumbs([
            {label: 'Explore', to: '/'},
            {label: 'Kayak rental'},
        ])

        const trail = screen.getByRole('navigation', {name: 'Breadcrumb'})
        expect(within(trail).getByRole('link', {name: 'Explore'}))
            .not.toHaveAttribute('aria-current')
        expect(within(trail).getByText('Kayak rental'))
            .toHaveAttribute('aria-current', 'page')
    })

    it('shows a single item as the current page with no links', () => {
        renderBreadcrumbs([{label: 'Explore'}])

        const trail = screen.getByRole('navigation', {name: 'Breadcrumb'})
        expect(within(trail).queryAllByRole('link')).toHaveLength(0)
        expect(within(trail).getByText('Explore'))
            .toHaveAttribute('aria-current', 'page')
    })

    it('keeps the items in the order given', () => {
        renderBreadcrumbs([
            {label: 'Explore', to: '/'},
            {label: 'Kayak rental', to: '/listings/3'},
            {label: 'Book'},
        ])

        const labels = within(screen.getByRole('navigation', {name: 'Breadcrumb'}))
            .getAllByRole('listitem')
            .map((item) => item.textContent)
        expect(labels).toEqual(['Explore', 'Kayak rental', 'Book'])
    })

    it('shows every item even when two have the same label', () => {
        renderBreadcrumbs([
            {label: 'Explore', to: '/'},
            {label: 'Explore'},
        ])

        const trail = screen.getByRole('navigation', {name: 'Breadcrumb'})
        expect(within(trail).getAllByRole('listitem')).toHaveLength(2)
        expect(within(trail).getByRole('link', {name: 'Explore'}))
            .toHaveAttribute('href', '/')
    })
})
