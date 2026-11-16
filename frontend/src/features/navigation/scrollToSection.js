// Scrolls to a section on the page and moves keyboard focus to it so
// it is clear where the user landed. Users who turned on "reduce
// motion" in their system settings get an instant jump instead.
export function scrollToSection(sectionId) {
    const section = document.getElementById(sectionId)
    if (!section) {
        return false
    }

    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')
        .matches
    section.scrollIntoView({
        behavior: reduceMotion ? 'auto' : 'smooth',
        block: 'start',
    })
    section.focus({preventScroll: true})
    return true
}
