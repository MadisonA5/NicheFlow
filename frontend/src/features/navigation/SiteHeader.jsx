import {useState} from 'react'
import {Link, NavLink, useLocation, useNavigate} from 'react-router-dom'
import nicheFlowLogo from '../../assets/nicheflow logo1.png'
import {getSession, signOut} from '../auth/authDataSource'

function SiteHeader() {
    const [session, setSession] = useState(getSession)
    const navigate = useNavigate()
    const {pathname} = useLocation()
    // Browsing covers the home page and any listing or its booking page
    const isExploring = pathname === '/' || /^\/listings\/\d+/.test(pathname)

    return (
        <header className="site-header">
            <Link className="brand" to="/" aria-label="NicheFlow home">
                <img className="brand-logo" src={nicheFlowLogo} alt="NicheFlow"/>
            </Link>

            <nav className="main-nav" aria-label="Main navigation">
                <a
                    className={isExploring ? 'is-current' : undefined}
                    href="/#listings"
                    aria-current={isExploring ? 'true' : undefined}
                >
                    Explore
                </a>
                <a href="/#how-it-works">How it works</a>
            </nav>

            <div className="header-actions">
                {session ? (
                    <>
                        {session.user.role === 'SUPPLIER' && (
                            <NavLink className="sign-in-button header-action-primary" to="/listings/new" end>
                                Create listing
                            </NavLink>
                        )}
                        <NavLink className="sign-in-button header-action-secondary" to="/account" end>
                            My account
                        </NavLink>
                        <button
                            className="sign-in-button header-action-secondary"
                            type="button"
                            onClick={() => {
                                signOut()
                                setSession(null)
                                navigate('/')
                            }}
                        >
                            Sign out
                        </button>
                    </>
                ) : (
                    <NavLink className="sign-in-button header-action-primary" to="/sign-in" end>
                        Sign in
                    </NavLink>
                )}
            </div>
        </header>
    )
}

export default SiteHeader
