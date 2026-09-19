import { useState, useEffect } from 'react'
import { bookmarkPrompt, removeBookmark, isBookmarked } from '../../api/bookmarkApi'
import CategoryBadge from './CategoryBadge'
import styles from './PromptCard.module.css'

/**
 * The main prompt display card — the visual centerpiece of every mode.
 * Includes a bookmark toggle button.
 */
export default function PromptCard({ prompt, animationClass = '' }) {
  const [saved, setSaved] = useState(false)
  const [toggling, setToggling] = useState(false)

  // Check bookmark status when prompt changes
  useEffect(() => {
    if (!prompt?.id) {
      setSaved(false)
      return
    }

    let cancelled = false
    isBookmarked(prompt.id)
      .then((result) => {
        if (!cancelled) setSaved(result)
      })
      .catch(() => {
        if (!cancelled) setSaved(false)
      })

    return () => { cancelled = true }
  }, [prompt?.id])

  async function handleToggleBookmark(e) {
    e.stopPropagation()
    if (!prompt?.id || toggling) return

    setToggling(true)
    try {
      if (saved) {
        await removeBookmark(prompt.id)
        setSaved(false)
      } else {
        await bookmarkPrompt(prompt.id)
        setSaved(true)
      }
    } catch (err) {
      console.error('Bookmark toggle failed:', err)
    } finally {
      setToggling(false)
    }
  }

  if (!prompt) {
    return (
      <div className={styles.card}>
        <p className={styles.loading}>Loading prompt…</p>
      </div>
    )
  }

  return (
    <div className={`${styles.card} ${animationClass}`}>
      <button
        className={`${styles.bookmarkBtn} ${saved ? styles.bookmarkActive : ''}`}
        onClick={handleToggleBookmark}
        disabled={toggling}
        title={saved ? 'Remove bookmark' : 'Bookmark this topic'}
        aria-label={saved ? 'Remove bookmark' : 'Bookmark this topic'}
      >
        {saved ? '★' : '☆'}
      </button>
      <div className={styles.categoryRow}>
        <CategoryBadge category={prompt.category} />
      </div>
      <p className={styles.text}>{prompt.text}</p>
    </div>
  )
}
