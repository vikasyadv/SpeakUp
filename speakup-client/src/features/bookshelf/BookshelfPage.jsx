import { useState, useEffect } from 'react'
import { getAllBookmarks, removeBookmark } from '../../api/bookmarkApi'
import { useNavigate } from 'react-router-dom'
import styles from './BookshelfPage.module.css'

export default function BookshelfPage() {
  const [bookmarks, setBookmarks] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    fetchBookmarks()
  }, [])

  async function fetchBookmarks() {
    setLoading(true)
    try {
      const data = await getAllBookmarks()
      setBookmarks(data)
    } catch (err) {
      console.error('Failed to load bookmarks:', err)
    } finally {
      setLoading(false)
    }
  }

  async function handleRemove(promptId) {
    try {
      await removeBookmark(promptId)
      setBookmarks((prev) => prev.filter((b) => b.prompt.id !== promptId))
    } catch (err) {
      console.error('Failed to remove bookmark:', err)
    }
  }

  function handlePractice(bookmark) {
    if (bookmark.prompt?.mode === 'STORY') {
      navigate('/story', { state: { prompt: bookmark.prompt } })
    } else if (bookmark.prompt?.mode === 'DEBATE') {
      navigate('/debate', { state: { prompt: bookmark.prompt } })
    } else if (bookmark.prompt?.mode === 'RESEARCH') {
      navigate('/research', { state: { prompt: bookmark.prompt } })
    } else {
      navigate('/off-the-cuff', { state: { prompt: bookmark.prompt } })
    }
  }

  if (loading) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>Bookshelf</h1>
        <p className={styles.subtitle}>Loading your saved topics…</p>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Bookshelf</h1>
      <p className={styles.subtitle}>
        {bookmarks.length === 0
          ? 'No saved topics yet. Bookmark prompts during practice to find them here.'
          : `${bookmarks.length} saved topic${bookmarks.length !== 1 ? 's' : ''}`}
      </p>

      {bookmarks.length > 0 && (
        <div className={styles.list}>
          {bookmarks.map((bookmark) => {
            const isStory = bookmark.prompt?.mode === 'STORY'
            const isDebate = bookmark.prompt?.mode === 'DEBATE'
            const isResearch = bookmark.prompt?.mode === 'RESEARCH'
            return (
              <div key={bookmark.id} className={styles.card}>
                <div className={styles.cardContent}>
                  <div className={styles.cardHeader}>
                    <span className={styles.category}>{bookmark.prompt.category}</span>
                    <span
                      className={`${styles.modeBadge} ${
                        isStory
                          ? styles.modeStory
                          : isDebate
                          ? styles.modeDebate
                          : isResearch
                          ? styles.modeResearch
                          : styles.modeOffTheCuff
                      }`}
                    >
                      {isStory ? 'Story' : isDebate ? 'Debate' : isResearch ? 'Research' : 'Off the Cuff'}
                    </span>
                  </div>
                  <p className={styles.text}>{bookmark.prompt.text}</p>
                </div>
                <div className={styles.cardActions}>
                  <button
                    className={styles.practiceBtn}
                    onClick={() => handlePractice(bookmark)}
                    title="Practice this topic"
                  >
                    Speak
                  </button>
                  <button
                    className={styles.removeBtn}
                    onClick={() => handleRemove(bookmark.prompt.id)}
                    title="Remove bookmark"
                  >
                    ✕
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
