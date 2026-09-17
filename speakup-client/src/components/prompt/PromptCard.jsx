import CategoryBadge from './CategoryBadge'
import styles from './PromptCard.module.css'

/**
 * The main prompt display card — the visual centerpiece of every mode.
 */
export default function PromptCard({ prompt, animationClass = '' }) {
  if (!prompt) {
    return (
      <div className={styles.card}>
        <p className={styles.loading}>Loading prompt…</p>
      </div>
    )
  }

  return (
    <div className={`${styles.card} ${animationClass}`}>
      <div className={styles.categoryRow}>
        <CategoryBadge category={prompt.category} />
      </div>
      <p className={styles.text}>{prompt.text}</p>
    </div>
  )
}
