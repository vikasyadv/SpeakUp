import { useState, useEffect, useCallback } from 'react'
import { generateFeedback, getFeedback } from '../../api/feedbackApi'
import Button from '../common/Button'
import styles from './AiFeedbackCard.module.css'

const MIN_WORDS = 5

/**
 * AI Speaking Coach feedback card supporting 5 distinct states:
 * 1. Not generated yet
 * 2. Loading / Analyzing
 * 3. Success / Feedback display
 * 4. Error / Retry
 * 5. Insufficient transcript
 */
export default function AiFeedbackCard({ sessionId, transcript }) {
  const [feedback, setFeedback] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const wordCount = transcript ? transcript.trim().split(/\s+/).filter(Boolean).length : 0
  const isTranscriptTooShort = wordCount < MIN_WORDS

  // Check if feedback already exists on mount for this session
  useEffect(() => {
    if (!sessionId) return

    let cancelled = false
    getFeedback(sessionId)
      .then((data) => {
        if (!cancelled && data) {
          setFeedback(data)
        }
      })
      .catch(() => {
        // 404 is normal if feedback has not been generated yet
      })

    return () => {
      cancelled = true
    }
  }, [sessionId])

  const handleGenerateFeedback = useCallback(async () => {
    if (!sessionId || loading || isTranscriptTooShort) return

    setLoading(true)
    setError(null)

    try {
      const data = await generateFeedback(sessionId)
      setFeedback(data)
    } catch (err) {
      console.error('Failed to generate AI feedback:', err)
      const message =
        err.response?.data?.message ||
        'Unable to generate feedback right now. Please check your connection and try again.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }, [sessionId, loading, isTranscriptTooShort])

  // STATE 5: Insufficient transcript
  if (isTranscriptTooShort) {
    return (
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.titleGroup}>
            <span className={styles.badge}>AI Speaking Coach</span>
          </div>
        </div>
        <div className={styles.emptyState}>
          <p className={styles.noticeText}>
            Your speech was too brief ({wordCount} word{wordCount !== 1 ? 's' : ''}). Please speak at least {MIN_WORDS} words to receive AI feedback.
          </p>
        </div>
      </div>
    )
  }

  // STATE 2: Loading
  if (loading) {
    return (
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.titleGroup}>
            <span className={styles.badge}>AI Speaking Coach</span>
          </div>
        </div>
        <div className={styles.loadingContainer}>
          <div className={styles.spinner} />
          <p className={styles.loadingText}>Analyzing your speech…</p>
          <span className={styles.loadingSubtext}>Evaluating clarity, relevance, and structure</span>
        </div>
      </div>
    )
  }

  // STATE 4: Error
  if (error) {
    return (
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.titleGroup}>
            <span className={styles.badge}>AI Speaking Coach</span>
          </div>
        </div>
        <div className={styles.errorContainer}>
          <p className={styles.errorMessage}>{error}</p>
          <Button variant="primary" size="md" onClick={handleGenerateFeedback}>
            Try Again
          </Button>
        </div>
      </div>
    )
  }

  // STATE 3: Success
  if (feedback) {
    return (
      <div className={`${styles.card} ${styles.successCard}`}>
        <div className={styles.header}>
          <div className={styles.titleGroup}>
            <span className={styles.badge}>AI Speaking Coach</span>
            <h3 className={styles.cardTitle}>Performance Assessment</h3>
          </div>
          <div className={styles.overallScoreBox}>
            <span className={styles.overallScoreLabel}>Overall</span>
            <span className={styles.overallScoreValue}>{feedback.overallScore}</span>
            <span className={styles.scoreMax}>/10</span>
          </div>
        </div>

        {/* Breakdown scores */}
        <div className={styles.scoreGrid}>
          <div className={styles.scoreMetric}>
            <span className={styles.metricLabel}>Clarity</span>
            <span className={styles.metricValue}>{feedback.clarityScore} <span className={styles.scoreMax}>/10</span></span>
          </div>
          <div className={styles.scoreMetric}>
            <span className={styles.metricLabel}>Relevance</span>
            <span className={styles.metricValue}>{feedback.relevanceScore} <span className={styles.scoreMax}>/10</span></span>
          </div>
          <div className={styles.scoreMetric}>
            <span className={styles.metricLabel}>Structure</span>
            <span className={styles.metricValue}>{feedback.structureScore} <span className={styles.scoreMax}>/10</span></span>
          </div>
        </div>

        {/* Coach summary */}
        <div className={styles.summarySection}>
          <p className={styles.summaryText}>{feedback.summary}</p>
        </div>

        {/* Strengths & Improvements */}
        <div className={styles.analysisGrid}>
          {feedback.strengths && feedback.strengths.length > 0 && (
            <div className={styles.analysisBox}>
              <h4 className={styles.boxTitleGreen}>Key Strengths</h4>
              <ul className={styles.bulletList}>
                {feedback.strengths.map((item, idx) => (
                  <li key={idx} className={styles.strengthItem}>{item}</li>
                ))}
              </ul>
            </div>
          )}

          {feedback.improvements && feedback.improvements.length > 0 && (
            <div className={styles.analysisBox}>
              <h4 className={styles.boxTitleAmber}>Areas to Improve</h4>
              <ul className={styles.bulletList}>
                {feedback.improvements.map((item, idx) => (
                  <li key={idx} className={styles.improvementItem}>{item}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    )
  }

  // STATE 1: Not generated yet
  return (
    <div className={styles.card}>
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <span className={styles.badge}>AI Speaking Coach</span>
          <h3 className={styles.cardTitle}>Instant Speech Analysis</h3>
        </div>
      </div>
      <p className={styles.promptText}>
        Get constructive feedback on your clarity, topic relevance, and response structure.
      </p>
      <div className={styles.ctaRow}>
        <Button variant="primary" size="md" onClick={handleGenerateFeedback}>
          Get AI Feedback
        </Button>
      </div>
    </div>
  )
}
