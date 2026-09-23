import { useState, useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './Header.module.css'

export default function Header() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, isAuthenticated, logout } = useAuth()
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const menuRef = useRef(null)

  const isHome = location.pathname === '/'

  // Close menu on click outside or Escape key
  useEffect(() => {
    if (!isMenuOpen) return

    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false)
      }
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        setIsMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDown)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isMenuOpen])

  // Close menu on navigation
  const [prevPathname, setPrevPathname] = useState(location.pathname)
  if (prevPathname !== location.pathname) {
    setPrevPathname(location.pathname)
    setIsMenuOpen(false)
  }

  const handleLogout = () => {
    setIsMenuOpen(false)
    logout()
  }

  const handleNavigateProfile = () => {
    setIsMenuOpen(false)
    navigate('/profile')
  }

  const displayName = user?.displayName || user?.email?.split('@')[0] || 'Account'
  const initial = (displayName.trim()[0] || 'U').toUpperCase()

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <div className={styles.brandGroup}>
          <Link to="/" className={styles.brand}>
            <span className={styles.brandName}>Speak</span>
            <span className={styles.brandAccent}>Up</span>
          </Link>

          {!isHome && (
            <Link to="/" className={styles.backLink}>
              ← Modes
            </Link>
          )}
        </div>

        <nav className={styles.nav}>
          <div className={styles.navLinks}>
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
          </div>

          {isAuthenticated ? (
            <div className={styles.profileContainer} ref={menuRef}>
              <button
                type="button"
                className={`${styles.profileBtn} ${isMenuOpen ? styles.profileBtnActive : ''}`}
                onClick={() => setIsMenuOpen((prev) => !prev)}
                aria-expanded={isMenuOpen}
                aria-haspopup="true"
                aria-label={`Account menu for ${displayName}`}
              >
                <div className={styles.avatarCircle} aria-hidden="true">
                  <span className={styles.avatarInitial}>{initial}</span>
                </div>
                <span className={styles.profileName}>{displayName}</span>
              </button>

              {isMenuOpen && (
                <div className={styles.dropdown} role="menu" aria-orientation="vertical">
                  <div className={styles.dropdownHeader}>
                    <div className={styles.dropdownName}>{displayName}</div>
                    <div className={styles.dropdownEmail}>{user?.email}</div>
                  </div>

                  <div className={styles.dropdownDivider} />

                  <button
                    type="button"
                    className={styles.dropdownItem}
                    role="menuitem"
                    onClick={handleNavigateProfile}
                  >
                    <span className={styles.itemIcon}>👤</span>
                    <span>Edit Profile</span>
                  </button>

                  <button
                    type="button"
                    className={`${styles.dropdownItem} ${styles.dropdownLogout}`}
                    role="menuitem"
                    onClick={handleLogout}
                  >
                    <span className={styles.itemIcon}>🚪</span>
                    <span>Log out</span>
                  </button>
                </div>
              )}
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
        </nav>
      </div>
    </header>
  )
}
