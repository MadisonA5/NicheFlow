import {useEffect} from 'react'

export const SITE_TITLE = 'NicheFlow | Find your next favorite place'

// Sets the browser tab title for the current page, e.g.
// "My account · NicheFlow". Without a title it uses the site title.
function usePageTitle(title) {
    useEffect(() => {
        document.title = title ? `${title} · NicheFlow` : SITE_TITLE
    }, [title])
}

export default usePageTitle
