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
})
