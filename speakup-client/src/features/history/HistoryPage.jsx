import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getRecentSessions, deleteSession } from '../../api/sessionApi'
import { formatTime } from '../../utils/formatTime'
import styles from './HistoryPage.module.css'

export default function HistoryPage() {
  const navigate = useNavigate()
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [expandedTranscripts, setExpandedTranscripts] = useState({})

  useEffect(() => {
    fetchSessions()
  }, [])

  async function fetchSessions() {
    setLoading(true)
    try {
      const data = await getRecentSessions()
      setSessions(data)
    } catch (err) {
      console.error('Failed to load sessions:', err)
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(sessionId) {
    try {
      await deleteSession(sessionId)
      setSessions((prev) => prev.filter((s) => s.id !== sessionId))
    } catch (err) {
      console.error('Failed to delete session:', err)
    }
  }

  function toggleTranscript(sessionId) {
    setExpandedTranscripts((prev) => ({
      ...prev,
      [sessionId]: !prev[sessionId],
    }))
  }

  function formatDate(isoString) {
    if (!isoString) return '—'
    const date = new Date(isoString)
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  function formatTimeOfDay(isoString) {
    if (!isoString) return ''
    const date = new Date(isoString)
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  function getStatusLabel(status) {
    switch (status) {
      case 'COMPLETED': return 'Completed'
      case 'IN_PROGRESS': return 'In Progress'
      case 'ABANDONED': return 'Abandoned'
      default: return status
    }
  }

  function getStatusClass(status) {
    switch (status) {
      case 'COMPLETED': return styles.statusCompleted
      case 'ABANDONED': return styles.statusAbandoned
      default: return styles.statusProgress
    }
  }

  if (loading) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>History</h1>
        <p className={styles.subtitle}>Loading your sessions…</p>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>History</h1>
      <p className={styles.subtitle}>
        {sessions.length === 0
          ? 'No sessions yet. Start speaking to build your history.'
          : `${sessions.length} recent session${sessions.length !== 1 ? 's' : ''}`}
      </p>

      {sessions.length > 0 && (
        <div className={styles.list}>
          {sessions.map((session) => {
            const hasTranscript = Boolean(session.transcript)
            const isExpanded = Boolean(expandedTranscripts[session.id])
            const speakingDuration = session.actualDurationSeconds ?? session.durationSeconds
            const isResearch = session.mode === 'RESEARCH'
            const sessionPath = isResearch
              ? `/research/session/${session.id}`
              : `/off-the-cuff/session/${session.id}`

            return (
              <div key={session.id} className={styles.card}>
                <div className={styles.cardMain}>
                  <div className={styles.topRow}>
                    <span className={`${styles.mode} ${isResearch ? styles.modeResearch : styles.modeOffTheCuff}`}>
                      {isResearch ? 'Research' : 'Off the Cuff'}
                    </span>
                    {session.category && (
                      <span className={styles.categoryBadge}>{session.category}</span>
                    )}
                    <span className={`${styles.status} ${getStatusClass(session.status)}`}>
                      {getStatusLabel(session.status)}
                    </span>
                  </div>

                  <p className={styles.promptText}>{session.promptText}</p>

                  <div className={styles.meta}>
                    <span className={styles.duration} title="Speaking Duration">
                      {formatTime(speakingDuration)}
                      {session.actualDurationSeconds &&
                        session.actualDurationSeconds !== session.durationSeconds && (
                          <span className={styles.targetDuration}>
                            {' '}/ {formatTime(session.durationSeconds)}
                          </span>
                        )}
                    </span>
                    {isResearch && session.preparationDurationSeconds != null && (
                      <>
                        <span className={styles.separator}>·</span>
                        <span className={styles.prepDuration} title="Preparation Time">
                          Prep: {formatTime(session.preparationDurationSeconds)}
                        </span>
                      </>
                    )}
                    {session.preparationNotes && (
                      <>
                        <span className={styles.separator}>·</span>
                        <span className={styles.notesIndicator} title="Includes preparation notes">
                          📝 Notes
                        </span>
                      </>
                    )}
                    <span className={styles.separator}>·</span>
                    <span className={styles.date}>{formatDate(session.startedAt)}</span>
                    <span className={styles.time}>{formatTimeOfDay(session.startedAt)}</span>
                  </div>

                  {session.status === 'COMPLETED' && (
                    <div className={styles.feedbackRow}>
                      {session.hasFeedback ? (
                        <button
                          className={styles.feedbackBadgeBtn}
                          onClick={() => navigate(sessionPath)}
                          title="View AI Speaking Feedback"
                        >
                          <span className={styles.feedbackIcon}>✦</span> AI Feedback
                        </button>
                      ) : (
                        <button
                          className={styles.getFeedbackBtn}
                          onClick={() => navigate(sessionPath)}
                          title="Review session and get AI feedback"
                        >
                          Get AI Feedback →
                        </button>
                      )}
                    </div>
                  )}

                  {hasTranscript && (
                    <div className={styles.transcriptSection}>
                      <button
                        className={styles.transcriptToggleBtn}
                        onClick={() => toggleTranscript(session.id)}
                      >
                        {isExpanded ? 'Hide Transcript ▲' : 'View Transcript ▼'}
                      </button>

                      {isExpanded && (
                        <div className={styles.transcriptContent}>
                          <p className={styles.transcriptText}>{session.transcript}</p>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <button
                  className={styles.deleteBtn}
                  onClick={() => handleDelete(session.id)}
                  title="Delete session"
                >
                  ✕
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
