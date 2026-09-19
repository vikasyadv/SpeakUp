import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getSession } from '../../api/sessionApi'
import SpeakingResult from './SpeakingResult'
import Button from '../../components/common/Button'
import styles from './SpeakingResultPage.module.css'

/**
 * Dedicated page route for reviewing a completed speaking session result.
 * Supports direct URLs (e.g. /off-the-cuff/session/:sessionId) and browser refresh.
 */
export default function SpeakingResultPage() {
  const { sessionId } = useParams()
  const navigate = useNavigate()

  const [session, setSession] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!sessionId) {
      setError('No session ID provided.')
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)

    getSession(sessionId)
      .then((data) => {
        if (cancelled) return
        if (!data) {
          setError('Session not found.')
        } else if (data.status !== 'COMPLETED') {
          setError('This session was not completed.')
          setSession(data)
        } else {
          setSession(data)
        }
      })
      .catch((err) => {
        if (cancelled) return
        console.error('Failed to load session:', err)
        setError(
          err.response?.status === 404
            ? 'Session not found.'
            : 'Failed to load session. Please try again.'
        )
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [sessionId])

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.loadingContainer}>
          <div className={styles.spinner} />
          <p className={styles.loadingText}>Loading session result…</p>
        </div>
      </div>
    )
  }

  if (error && (!session || session.status !== 'COMPLETED')) {
    return (
      <div className={styles.page}>
        <div className={styles.errorContainer}>
          <h2 className={styles.errorTitle}>Session Unavailable</h2>
          <p className={styles.errorMessage}>{error}</p>
          <div className={styles.actionRow}>
            <Button variant="primary" size="md" onClick={() => navigate('/off-the-cuff')}>
              Go to Off the Cuff
            </Button>
            <Button variant="ghost" size="md" onClick={() => navigate('/history')}>
              View History
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <SpeakingResult
        sessionId={session.id}
        prompt={{
          id: session.promptId,
          text: session.promptText,
          category: session.category || 'General',
        }}
        durationSeconds={session.durationSeconds}
        actualDurationSeconds={session.actualDurationSeconds}
        transcript={session.transcript}
        onPracticeAgain={() => navigate('/off-the-cuff')}
        onBackToOffTheCuff={() => navigate('/off-the-cuff')}
        onBackToModes={() => navigate('/')}
      />
    </div>
  )
}
