import { useState, useEffect } from 'react'
import Button from '../../components/common/Button'
import AiFeedbackCard from '../../components/feedback/AiFeedbackCard'
import { bookmarkPrompt, removeBookmark, isBookmarked } from '../../api/bookmarkApi'
import { formatTime } from '../../utils/formatTime'
import styles from './SpeakingResult.module.css'

/**
 * Clean result & review screen shown after completing a speaking session.
 */
export default function SpeakingResult({
  sessionId,
  prompt,
  mode,
  stance,
  durationSeconds,
  actualDurationSeconds,
  preparationDurationSeconds,
  preparationNotes,
  transcript,
  onPracticeAgain,
  onBackToOffTheCuff,
  onBackToModes,
}) {
  const [saved, setSaved] = useState(false)
  const [toggling, setToggling] = useState(false)
  const displayDuration = actualDurationSeconds ?? durationSeconds
  const isStory = mode === 'STORY' || prompt?.mode === 'STORY'
  const isDebate = mode === 'DEBATE' || prompt?.mode === 'DEBATE'
  const isResearch = mode === 'RESEARCH' || prompt?.mode === 'RESEARCH'
  const backToModeLabel = isStory
    ? 'Back to Story'
    : isDebate
    ? 'Back to Debate'
    : isResearch
    ? 'Back to Research'
    : 'Back to Off the Cuff'

  useEffect(() => {
    if (!prompt?.id) return

    let cancelled = false
    isBookmarked(prompt.id)
      .then((result) => {
        if (!cancelled) setSaved(result)
      })
      .catch(() => {
        if (!cancelled) setSaved(false)
      })

    return () => {
      cancelled = true
    }
  }, [prompt?.id])

  async function handleToggleBookmark() {
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

  return (
    <div className={styles.container}>
      <div className={styles.headerBadge}>Session Complete</div>

      <div className={styles.promptSection}>
        <div className={styles.promptMetaRow}>
          <span className={styles.category}>{prompt?.category || 'General'}</span>
          {isStory && <span className={styles.modeBadgeStory}>Story</span>}
          {isDebate && <span className={styles.modeBadgeDebate}>Debate</span>}
          {stance && (
            <span
              className={`${styles.stanceBadge} ${
                stance === 'FOR' ? styles.stanceFor : styles.stanceAgainst
              }`}
            >
              Stance: {stance}
            </span>
          )}
        </div>
        <h2 className={styles.promptText}>{prompt?.text}</h2>
        {prompt?.id && (
          <button
            className={`${styles.bookmarkBtn} ${saved ? styles.bookmarkActive : ''}`}
            onClick={handleToggleBookmark}
            disabled={toggling}
            title={saved ? 'Remove from Bookshelf' : 'Save to Bookshelf'}
          >
            {saved ? '★ Saved to Bookshelf' : '☆ Save to Bookshelf'}
          </button>
        )}
      </div>

      <div className={styles.statsRow}>
        <div className={styles.statCard}>
          <span className={styles.statLabel}>Speaking Time</span>
          <span className={styles.statValue}>{formatTime(displayDuration)}</span>
        </div>
        <div className={styles.statCard}>
          <span className={styles.statLabel}>Target Timer</span>
          <span className={styles.statValue}>{formatTime(durationSeconds)}</span>
        </div>
        {(isStory || isDebate || isResearch || preparationDurationSeconds != null) && (
          <div className={styles.statCard}>
            <span className={styles.statLabel}>Prep Time</span>
            <span className={styles.statValue}>{formatTime(preparationDurationSeconds || 0)}</span>
          </div>
        )}
        {stance && (
          <div className={styles.statCard}>
            <span className={styles.statLabel}>Stance</span>
            <span
              className={`${styles.statValue} ${
                stance === 'FOR' ? styles.stanceForText : styles.stanceAgainstText
              }`}
            >
              {stance}
            </span>
          </div>
        )}
        <div className={styles.statCard}>
          <span className={styles.statLabel}>Status</span>
          <span className={styles.statusCompleted}>Completed</span>
        </div>
      </div>

      {preparationNotes && (
        <div className={styles.prepNotesCard}>
          <div className={styles.prepNotesHeader}>
            <span className={styles.prepNotesTitle}>Preparation Notes</span>
            <span className={styles.prepNotesWordCount}>
              {preparationNotes.trim().split(/\s+/).filter(Boolean).length} words
            </span>
          </div>
          <div className={styles.prepNotesBody}>
            <p className={styles.prepNotesText}>{preparationNotes}</p>
          </div>
        </div>
      )}

      <div className={styles.transcriptCard}>
        <div className={styles.transcriptHeader}>
          <span className={styles.transcriptTitle}>Transcript</span>
          <span className={styles.wordCount}>
            {transcript ? `${transcript.trim().split(/\s+/).filter(Boolean).length} words` : '0 words'}
          </span>
        </div>
        <div className={styles.transcriptBody}>
          {transcript ? (
            <p className={styles.transcriptText}>{transcript}</p>
          ) : (
            <p className={styles.emptyTranscript}>
              No speech recognized. You can still review your session duration in History.
            </p>
          )}
        </div>
      </div>

      {/* AI Speaking Coach Feedback Section */}
      <AiFeedbackCard sessionId={sessionId} transcript={transcript} />

      <div className={styles.actions}>
        <Button variant="primary" size="lg" onClick={onPracticeAgain}>
          Practice Again
        </Button>
        <Button variant="secondary" size="md" onClick={onBackToOffTheCuff}>
          {backToModeLabel}
        </Button>
        {onBackToModes && (
          <Button variant="ghost" size="md" onClick={onBackToModes}>
            Back to Modes
          </Button>
        )}
      </div>
    </div>
  )
}
