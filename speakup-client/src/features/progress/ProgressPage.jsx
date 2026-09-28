import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { getProgressAnalytics } from '../../api/analyticsApi'
import Button from '../../components/common/Button'
import styles from './ProgressPage.module.css'

function formatSpeakingDuration(totalSeconds) {
  if (!totalSeconds || totalSeconds <= 0) return '0s'
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const parts = []
  if (hours > 0) parts.push(`${hours}h`)
  if (minutes > 0) parts.push(`${minutes}m`)
  if (seconds > 0 || parts.length === 0) parts.push(`${seconds}s`)
  return parts.join(' ')
}

function formatScore(score) {
  if (score == null) return '—'
  return `${score} / 100`
}

function getPercentage(count, total) {
  if (!total || total <= 0) return 0
  return Math.round((count / total) * 100)
}

function formatDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

function formatMode(mode) {
  if (!mode) return ''
  const map = {
    OFF_THE_CUFF: 'Off the Cuff',
    RESEARCH: 'Research',
    DEBATE: 'Debate',
    STORY: 'Story',
  }
  return map[mode] || mode
}

// ---------------------------------------------------------------------------
// ScoreChart — dependency-free SVG line chart
// ---------------------------------------------------------------------------
const CHART_PAD = { top: 16, right: 16, bottom: 24, left: 36 }
const CHART_H = 160
const GUIDE_SCORES = [25, 50, 75, 100]

function ScoreChart({ scoreHistory }) {
  const containerRef = useRef(null)
  const [width, setWidth] = useState(600)
  const [hoveredIdx, setHoveredIdx] = useState(null)

  // Measure container width so chart is responsive
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        setWidth(entry.contentRect.width)
      }
    })
    ro.observe(el)
    setWidth(el.getBoundingClientRect().width)
    return () => ro.disconnect()
  }, [])

  if (!scoreHistory || scoreHistory.length === 0) {
    return (
      <div className={styles.chartEmpty}>
        <span className={styles.chartEmptyIcon} aria-hidden="true">📈</span>
        <p className={styles.chartEmptyText}>No score history yet. Complete a session and request AI feedback to see your trend.</p>
      </div>
    )
  }

  const innerW = Math.max(width - CHART_PAD.left - CHART_PAD.right, 10)
  const innerH = CHART_H - CHART_PAD.top - CHART_PAD.bottom

  // Map score 0-100 → y pixel (higher score = smaller y)
  const toY = (score) => CHART_PAD.top + (1 - score / 100) * innerH

  // Map index → x pixel
  const toX = (i) => {
    if (scoreHistory.length === 1) return CHART_PAD.left + innerW / 2
    return CHART_PAD.left + (i / (scoreHistory.length - 1)) * innerW
  }

  // Build polyline points string
  const polylinePoints = scoreHistory
    .map((pt, i) => `${toX(i)},${toY(pt.overallScore)}`)
    .join(' ')

  const hovered = hoveredIdx != null ? scoreHistory[hoveredIdx] : null

  return (
    <div ref={containerRef} className={styles.chartWrapper}>
      {/* Tooltip (DOM, not SVG, for correct positioning) */}
      {hovered && (
        <div
          className={styles.chartTooltip}
          style={{
            left: toX(hoveredIdx),
            top: toY(hovered.overallScore) - 12,
          }}
        >
          <span className={styles.chartTooltipScore}>{hovered.overallScore}</span>
          <span className={styles.chartTooltipDate}>{formatDate(hovered.date)}</span>
        </div>
      )}

      <svg
        viewBox={`0 0 ${width} ${CHART_H}`}
        width={width}
        height={CHART_H}
        aria-label="Score history chart"
        role="img"
        className={styles.chartSvg}
      >
        {/* Horizontal guide lines */}
        {GUIDE_SCORES.map((score) => {
          const y = toY(score)
          return (
            <g key={score}>
              <line
                x1={CHART_PAD.left}
                y1={y}
                x2={CHART_PAD.left + innerW}
                y2={y}
                className={styles.chartGuide}
              />
              <text
                x={CHART_PAD.left - 6}
                y={y + 4}
                className={styles.chartAxisLabel}
                textAnchor="end"
              >
                {score}
              </text>
            </g>
          )
        })}

        {/* Score line (only if >1 point) */}
        {scoreHistory.length > 1 && (
          <polyline
            points={polylinePoints}
            fill="none"
            stroke="var(--color-primary)"
            strokeWidth="2"
            strokeLinejoin="round"
            strokeLinecap="round"
          />
        )}

        {/* Data points */}
        {scoreHistory.map((pt, i) => (
          <circle
            key={pt.sessionId ?? i}
            cx={toX(i)}
            cy={toY(pt.overallScore)}
            r={hoveredIdx === i ? 6 : 4}
            fill={hoveredIdx === i ? '#f87171' : 'var(--color-primary)'}
            stroke="var(--color-bg)"
            strokeWidth="2"
            className={styles.chartDot}
            onMouseEnter={() => setHoveredIdx(i)}
            onMouseLeave={() => setHoveredIdx(null)}
          />
        ))}
      </svg>
    </div>
  )
}

export default function ProgressPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let isMounted = true

    async function loadProgress() {
      try {
        const response = await getProgressAnalytics()
        if (isMounted) {
          setData(response)
        }
      } catch (err) {
        if (isMounted) {
          setError(err.response?.data?.message || err.message || 'Failed to load progress data')
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadProgress()

    return () => {
      isMounted = false
    }
  }, [])

  const handleRetry = () => {
    setLoading(true)
    setError(null)
    getProgressAnalytics()
      .then((response) => setData(response))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load progress data'))
      .finally(() => setLoading(false))
  }

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.header}>
          <h1 className={styles.title}>Progress & Analytics</h1>
          <p className={styles.subtitle}>Loading your speaking progress…</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className={styles.page}>
        <div className={styles.header}>
          <h1 className={styles.title}>Progress & Analytics</h1>
        </div>
        <div className={styles.errorState}>
          <p className={styles.errorText}>⚠️ {error}</p>
          <Button variant="primary" size="sm" onClick={handleRetry}>
            Retry
          </Button>
        </div>
      </div>
    )
  }

  const summary = data?.summary || {
    totalCompletedSessions: 0,
    totalSpeakingTimeSeconds: 0,
    averageOverallScore: null,
    reviewedSessionsCount: 0,
  }

  const skills = data?.skills || {
    averageClarity: null,
    averageRelevance: null,
    averageStructure: null,
  }

  const modeBreakdown = data?.modeBreakdown || {
    offTheCuffCount: 0,
    researchCount: 0,
    debateCount: 0,
    storyCount: 0,
  }

  const scoreHistory = data?.scoreHistory || []
  const recentActivity = data?.recentActivity || []

  const hasSessions = summary.totalCompletedSessions > 0
  const hasReviewedSessions = summary.reviewedSessionsCount > 0

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1 className={styles.title}>Progress & Analytics</h1>
        <p className={styles.subtitle}>
          Track your speaking frequency, skill development, and AI scores.
        </p>
      </div>

      {!isAuthenticated && (
        <div className={styles.guestBanner}>
          <span className={styles.guestIcon} aria-hidden="true">💡</span>
          <div className={styles.guestContent}>
            <strong>Guest Mode:</strong> Your practice statistics are stored locally in this browser.{' '}
            <Link to="/register" className={styles.guestLink}>
              Create an account
            </Link>{' '}
            to sync your progress across devices.
          </div>
        </div>
      )}

      {!hasSessions ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon} aria-hidden="true">📊</div>
          <h2 className={styles.emptyTitle}>No Completed Sessions Yet</h2>
          <p className={styles.emptyText}>
            Complete your first practice session in any mode to start tracking your speaking frequency,
            skills, and AI scores.
          </p>
          <Button variant="primary" size="md" onClick={() => navigate('/')}>
            Start Speaking
          </Button>
        </div>
      ) : (
        <>
          {/* KPI Cards */}
          <div className={styles.kpiGrid}>
            <div className={styles.kpiCard}>
              <span className={styles.kpiLabel}>Practice Sessions</span>
              <span className={styles.kpiValue}>{summary.totalCompletedSessions}</span>
              <span className={styles.kpiUnit}>completed</span>
            </div>

            <div className={styles.kpiCard}>
              <span className={styles.kpiLabel}>Total Speaking Time</span>
              <span className={styles.kpiValue}>
                {formatSpeakingDuration(summary.totalSpeakingTimeSeconds)}
              </span>
              <span className={styles.kpiUnit}>recorded</span>
            </div>

            <div className={styles.kpiCard}>
              <span className={styles.kpiLabel}>Average AI Score</span>
              <span className={`${styles.kpiValue} ${summary.averageOverallScore != null ? styles.kpiValueAccent : ''}`}>
                {summary.averageOverallScore != null ? summary.averageOverallScore : '—'}
              </span>
              <span className={styles.kpiUnit}>
                {summary.averageOverallScore != null ? 'out of 100' : 'no reviews yet'}
              </span>
            </div>

            <div className={styles.kpiCard}>
              <span className={styles.kpiLabel}>Reviewed Sessions</span>
              <span className={styles.kpiValue}>{summary.reviewedSessionsCount}</span>
              <span className={styles.kpiUnit}>with AI feedback</span>
            </div>
          </div>

          {!hasReviewedSessions && (
            <div className={styles.noticeBanner}>
              <span className={styles.noticeIcon} aria-hidden="true">✦</span>
              <div>
                <strong>No AI feedback generated yet.</strong> Visit your{' '}
                <Link to="/history" className={styles.inlineLink}>
                  History
                </Link>{' '}
                and click &quot;Get AI Feedback&quot; on any completed session to unlock skill ratings.
              </div>
            </div>
          )}

          {/* Skill Metrics */}
          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Skill Metrics</h2>
              <span className={styles.sectionSubtitle}>
                Averages across your AI-reviewed sessions
              </span>
            </div>

            <div className={styles.skillsGrid}>
              <div className={styles.skillCard}>
                <div className={styles.skillTop}>
                  <span className={styles.skillName}>Clarity</span>
                  <span className={skills.averageClarity != null ? styles.skillScore : styles.skillScoreNull}>
                    {formatScore(skills.averageClarity)}
                  </span>
                </div>
                <div className={styles.progressBarTrack}>
                  <div
                    className={styles.progressBarFill}
                    style={{ width: `${skills.averageClarity ?? 0}%` }}
                  />
                </div>
              </div>

              <div className={styles.skillCard}>
                <div className={styles.skillTop}>
                  <span className={styles.skillName}>Relevance</span>
                  <span className={skills.averageRelevance != null ? styles.skillScore : styles.skillScoreNull}>
                    {formatScore(skills.averageRelevance)}
                  </span>
                </div>
                <div className={styles.progressBarTrack}>
                  <div
                    className={styles.progressBarFill}
                    style={{ width: `${skills.averageRelevance ?? 0}%` }}
                  />
                </div>
              </div>

              <div className={styles.skillCard}>
                <div className={styles.skillTop}>
                  <span className={styles.skillName}>Structure</span>
                  <span className={skills.averageStructure != null ? styles.skillScore : styles.skillScoreNull}>
                    {formatScore(skills.averageStructure)}
                  </span>
                </div>
                <div className={styles.progressBarTrack}>
                  <div
                    className={styles.progressBarFill}
                    style={{ width: `${skills.averageStructure ?? 0}%` }}
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Mode Distribution */}
          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Mode Distribution</h2>
              <span className={styles.sectionSubtitle}>
                Completed sessions per practice mode
              </span>
            </div>

            <div className={styles.modeGrid}>
              <div className={styles.modeCard}>
                <div className={styles.modeTop}>
                  <span className={styles.modeName}>Off the Cuff</span>
                  <span className={styles.modeCount}>
                    {modeBreakdown.offTheCuffCount}
                    <span className={styles.modePercentage}>
                      ({getPercentage(modeBreakdown.offTheCuffCount, summary.totalCompletedSessions)}%)
                    </span>
                  </span>
                </div>
                <div className={styles.modeTrack}>
                  <div
                    className={styles.modeFill}
                    style={{
                      width: `${getPercentage(modeBreakdown.offTheCuffCount, summary.totalCompletedSessions)}%`,
                    }}
                  />
                </div>
              </div>

              <div className={styles.modeCard}>
                <div className={styles.modeTop}>
                  <span className={styles.modeName}>Research</span>
                  <span className={styles.modeCount}>
                    {modeBreakdown.researchCount}
                    <span className={styles.modePercentage}>
                      ({getPercentage(modeBreakdown.researchCount, summary.totalCompletedSessions)}%)
                    </span>
                  </span>
                </div>
                <div className={styles.modeTrack}>
                  <div
                    className={styles.modeFill}
                    style={{
                      width: `${getPercentage(modeBreakdown.researchCount, summary.totalCompletedSessions)}%`,
                    }}
                  />
                </div>
              </div>

              <div className={styles.modeCard}>
                <div className={styles.modeTop}>
                  <span className={styles.modeName}>Debate</span>
                  <span className={styles.modeCount}>
                    {modeBreakdown.debateCount}
                    <span className={styles.modePercentage}>
                      ({getPercentage(modeBreakdown.debateCount, summary.totalCompletedSessions)}%)
                    </span>
                  </span>
                </div>
                <div className={styles.modeTrack}>
                  <div
                    className={styles.modeFill}
                    style={{
                      width: `${getPercentage(modeBreakdown.debateCount, summary.totalCompletedSessions)}%`,
                    }}
                  />
                </div>
              </div>

              <div className={styles.modeCard}>
                <div className={styles.modeTop}>
                  <span className={styles.modeName}>Story</span>
                  <span className={styles.modeCount}>
                    {modeBreakdown.storyCount}
                    <span className={styles.modePercentage}>
                      ({getPercentage(modeBreakdown.storyCount, summary.totalCompletedSessions)}%)
                    </span>
                  </span>
                </div>
                <div className={styles.modeTrack}>
                  <div
                    className={styles.modeFill}
                    style={{
                      width: `${getPercentage(modeBreakdown.storyCount, summary.totalCompletedSessions)}%`,
                    }}
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Score History */}
          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Score History</h2>
              <span className={styles.sectionSubtitle}>Your AI scores over time</span>
            </div>
            <div className={styles.chartCard}>
              <ScoreChart scoreHistory={scoreHistory} />
            </div>
          </section>

          {/* Recent Activity */}
          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Recent Activity</h2>
              <span className={styles.sectionSubtitle}>Your 5 most recent completed sessions</span>
            </div>

            {recentActivity.length === 0 ? (
              <div className={styles.activityEmpty}>
                <p className={styles.activityEmptyText}>No completed practice sessions yet.</p>
                <Button variant="primary" size="sm" onClick={() => navigate('/')}>
                  Start Speaking
                </Button>
              </div>
            ) : (
              <ul className={styles.activityList}>
                {recentActivity.map((item) => (
                  <li key={item.sessionId} className={styles.activityItem}>
                    <div className={styles.activityMain}>
                      <p className={styles.activityPrompt}>
                        {item.promptText
                          ? item.promptText.length > 100
                            ? `${item.promptText.slice(0, 100)}…`
                            : item.promptText
                          : '(no prompt)'}
                      </p>
                      <div className={styles.activityMeta}>
                        <span className={styles.activityBadge}>{formatMode(item.mode)}</span>
                        <span className={styles.activityMetaItem}>{formatDate(item.completedAt)}</span>
                        <span className={styles.activityMetaItem}>
                          {formatSpeakingDuration(item.durationSeconds)}
                        </span>
                      </div>
                    </div>
                    <div className={styles.activityRight}>
                      <span className={item.overallScore != null ? styles.activityScore : styles.activityScoreNull}>
                        {item.overallScore != null ? item.overallScore : '—'}
                      </span>
                      {item.hasFeedback ? (
                        <span className={styles.activityFeedbackBadge}>Reviewed</span>
                      ) : (
                        <span className={styles.activityFeedbackMissing}>No feedback</span>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
