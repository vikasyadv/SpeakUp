import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './Header.module.css'

export default function Header() {
  const location = useLocation()
  const { user, isAuthenticated, logout } = useAuth()
  const isHome = location.pathname === '/'

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.brand}>
          <span className={styles.brandName}>Speak</span>
          <span className={styles.brandAccent}>Up</span>
        </Link>

        <nav className={styles.nav}>
          <Link
            to="/bookshelf"
            className={`${styles.navLink} ${location.pathname === '/bookshelf' ? styles.navActive : ''}`}
          >
            Bookshelf
          </Link>
          <Link
            to="/history"
            className={`${styles.navLink} ${location.pathname === '/history' ? styles.navActive : ''}`}
          >
            History
          </Link>

          {isAuthenticated ? (
            <div className={styles.userSection}>
              <span className={styles.userGreeting} title={user?.email}>
                {user?.displayName || user?.email?.split('@')[0] || 'Account'}
              </span>
              <button
                type="button"
                onClick={logout}
                className={styles.logoutBtn}
                title="Sign out"
              >
                Log out
              </button>
            </div>
          ) : (
            <Link
              to="/login"
              state={{ from: location.pathname }}
              className={`${styles.authLink} ${
                location.pathname === '/login' || location.pathname === '/register'
                  ? styles.navActive
                  : ''
              }`}
            >
              Sign in
            </Link>
          )}

          {!isHome && (
            <Link to="/" className={styles.backLink}>
              ← Modes
            </Link>
          )}
        </nav>
      </div>
    </header>
  )
}
