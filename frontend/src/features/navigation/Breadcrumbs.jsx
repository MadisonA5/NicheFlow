import {Link} from 'react-router-dom'

// Shows where the page sits in the site, e.g. Explore › Kayak › Book.
// Every item except the last is a link; the last item is the current page.
function Breadcrumbs({items}) {
    return (
        <nav className="breadcrumbs" aria-label="Breadcrumb">
            <ol>
                {items.map((item, index) => {
                    const isCurrent = index === items.length - 1

                    return (
                        <li key={`${index}-${item.label}`}>
                            {isCurrent ? (
                                <span aria-current="page">{item.label}</span>
                            ) : (
                                <Link to={item.to}>{item.label}</Link>
                            )}
                        </li>
                    )
                })}
            </ol>
        </nav>
    )
}

export default Breadcrumbs
