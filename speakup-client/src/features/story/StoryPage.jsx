import styles from './StoryPage.module.css'

export default function StoryPage() {
  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Story</h1>
      <p className={styles.subtitle}>Get a scenario. Tell the story.</p>
      <div className={styles.coming}>Coming in Phase 5</div>
    </div>
  )
}
