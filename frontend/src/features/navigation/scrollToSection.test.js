import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {scrollToSection} from './scrollToSection'

function mockReducedMotion(reduce) {
    vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({matches: reduce}))
}

describe('scrollToSection', () => {
    let section

    beforeEach(() => {
        section = document.createElement('section')
        section.id = 'how-it-works'
        section.tabIndex = -1
        section.scrollIntoView = vi.fn()
        document.body.append(section)
    })

    afterEach(() => {
        section.remove()
        vi.unstubAllGlobals()
    })

    it('scrolls smoothly to the section and focuses it', () => {
        mockReducedMotion(false)

        expect(scrollToSection('how-it-works')).toBe(true)

        expect(section.scrollIntoView).toHaveBeenCalledWith({
            behavior: 'smooth',
            block: 'start',
        })
        expect(document.activeElement).toBe(section)
    })

    it('jumps instantly when the user prefers reduced motion', () => {
        mockReducedMotion(true)

        scrollToSection('how-it-works')

        expect(section.scrollIntoView).toHaveBeenCalledWith({
            behavior: 'auto',
            block: 'start',
        })
        expect(window.matchMedia)
            .toHaveBeenCalledWith('(prefers-reduced-motion: reduce)')
    })

    it('scrolls smoothly when the browser cannot report motion settings', () => {
        vi.stubGlobal('matchMedia', undefined)

        scrollToSection('how-it-works')

        expect(section.scrollIntoView).toHaveBeenCalledWith({
            behavior: 'smooth',
            block: 'start',
        })
    })

    it('does nothing when the section is not on the page', () => {
        mockReducedMotion(false)

        expect(scrollToSection('missing-section')).toBe(false)
        expect(section.scrollIntoView).not.toHaveBeenCalled()
    })
})
