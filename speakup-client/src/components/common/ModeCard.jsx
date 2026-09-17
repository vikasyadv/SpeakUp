import { useNavigate } from 'react-router-dom'
import styles from './ModeCard.module.css'

/**
 * A card for the homepage representing a speaking mode.
 */
export default function ModeCard({ mode }) {
  const navigate = useNavigate()

  return (
    <button
      className={styles.card}
      onClick={() => navigate(mode.path)}
      aria-label={`Start ${mode.name} mode`}
    >
      <span className={styles.icon}>{mode.icon}</span>
      <h2 className={styles.name}>{mode.name}</h2>
      <p className={styles.tagline}>{mode.tagline}</p>
      <span className={styles.arrow}>→</span>
    </button>
  )
}
