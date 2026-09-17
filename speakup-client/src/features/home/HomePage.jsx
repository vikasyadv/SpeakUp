import ModeCard from '../../components/common/ModeCard'
import { MODES } from '../../utils/constants'
import styles from './HomePage.module.css'

export default function HomePage() {
  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <h1 className={styles.title}>
          Think quick.<br />
          <span className={styles.accent}>Speak better.</span>
        </h1>
        <p className={styles.subtitle}>
          Practice impromptu speaking, sharpen your arguments, and find your voice.
        </p>
      </div>

      <div className={styles.grid}>
        {MODES.map((mode) => (
          <ModeCard key={mode.id} mode={mode} />
        ))}
      </div>
    </div>
  )
}
