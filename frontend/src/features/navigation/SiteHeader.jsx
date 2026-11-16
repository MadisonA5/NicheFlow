import {useState} from 'react'
import {Link, useNavigate} from 'react-router-dom'
import nicheFlowLogo from '../../assets/nicheflow logo1.png'
import {getSession, signOut} from '../auth/authDataSource'

function SiteHeader() {
    const [session, setSession] = useState(getSession)
    const navigate = useNavigate()

    return (
        <header className="site-header">
            <Link className="brand" to="/" aria-label="NicheFlow home">
                <img className="brand-logo" src={nicheFlowLogo} alt="NicheFlow"/>
            </Link>

            <nav className="main-nav" aria-label="Main navigation">
                <a href="/#listings">Explore</a>
                <a href="/#how-it-works">How it works</a>
            </nav>

            <div className="header-actions">
                {session ? (
                    <>
                        {session.user.role === 'SUPPLIER' && (
                            <Link className="sign-in-button header-action-primary" to="/listings/new">
                                Create listing
                            </Link>
                        )}
                        <Link className="sign-in-button header-action-secondary" to="/account">
                            My account
                        </Link>
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
                    <Link className="sign-in-button header-action-primary" to="/sign-in">
                        Sign in
                    </Link>
                )}
            </div>
        </header>
    )
}

export default SiteHeader
