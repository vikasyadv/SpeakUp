import { Link, useLocation } from 'react-router-dom'
import styles from './Header.module.css'

export default function Header() {
  const location = useLocation()
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
