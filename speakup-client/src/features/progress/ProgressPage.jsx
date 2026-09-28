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
// Insight helpers — pure functions, no side effects, no new API calls
// ---------------------------------------------------------------------------

/**
 * Derives the strongest and weakest skill from the three skill averages.
 *
 * Rules:
 * - Returns null for both if all three averages are null (no feedback yet).
 * - Considers only the skills that are non-null.
 * - Ties (same score) for strongest: clarity beats relevance beats structure.
 * - Ties for weakest: structure beats relevance beats clarity (reverse order).
 * - If only one skill is non-null, strongest and weakest both point to that skill.
 *
 * @param {{ averageClarity: number|null, averageRelevance: number|null, averageStructure: number|null }} skills
 * @returns {{ strongest: { label: string, value: number }|null, weakest: { label: string, value: number }|null }}
 */
function computeSkillInsights(skills) {
  const candidates = [
    { label: 'Clarity', value: skills?.averageClarity },
    { label: 'Relevance', value: skills?.averageRelevance },
    { label: 'Structure', value: skills?.averageStructure },
  ].filter((s) => s.value != null)

  if (candidates.length === 0) return { strongest: null, weakest: null }

  // Strongest: highest value; ties resolved by insertion order (first wins)
  const strongest = candidates.reduce((best, s) => (s.value > best.value ? s : best))

  // Weakest: lowest value; ties resolved by reverse insertion order (last wins)
  const weakest = candidates.reduce((worst, s) => (s.value < worst.value ? s : worst))

  return { strongest, weakest }
}

/**
 * Computes a performance trend from the chronological score history.
 *
 * Algorithm:
 * - Requires at least 4 scored sessions. Fewer → 'insufficient'.
 * - Splits the array into two equal halves (first half = earlier, second half = recent).
 * - Computes the mean overall score of each half.
 * - Difference threshold: ±3 points determines improving vs declining vs stable.
 *
 * Returns one of: 'improving' | 'declining' | 'stable' | 'insufficient'
 *
 * @param {Array<{ overallScore: number }>} scoreHistory - chronological ASC
 * @returns {'improving'|'declining'|'stable'|'insufficient'}
 */
function computeScoreTrend(scoreHistory) {
  if (!scoreHistory || scoreHistory.length < 4) return 'insufficient'

  const mid = Math.floor(scoreHistory.length / 2)
  const early = scoreHistory.slice(0, mid)
  const recent = scoreHistory.slice(scoreHistory.length - mid)

  const avg = (arr) => arr.reduce((sum, p) => sum + p.overallScore, 0) / arr.length

  const earlyAvg = avg(early)
  const recentAvg = avg(recent)
  const delta = recentAvg - earlyAvg

  if (delta > 3) return 'improving'
  if (delta < -3) return 'declining'
  return 'stable'
}

const TREND_CONFIG = {
  improving: { symbol: '↑', label: 'Improving', className: 'trendImproving' },
  declining: { symbol: '↓', label: 'Declining', className: 'trendDeclining' },
  stable: { symbol: '→', label: 'Stable', className: 'trendStable' },
  insufficient: { symbol: '→', label: 'Not enough data', className: 'trendStable' },
}

/**
 * Computes a deterministic recommended practice action based on skill insights.
 *
 * Mapping rules:
 * - If no reviewed sessions: neutral "Get Your First Insight" action pointing to Off the Cuff.
 * - If weakest skill is Structure: recommend Research mode (dedicated preparation & structured outlines).
 * - If weakest skill is Relevance: recommend Debate mode (defending a specific stance on-topic).
 * - If weakest skill is Clarity: recommend Off the Cuff mode (concise, spontaneous articulation).
 * - If all skills are even (or tie where strongest === weakest): recommend Story mode (balanced narrative delivery).
 *
 * @param {{
 *   hasReviewedSessions: boolean,
 *   strongestSkill: { label: string, value: number }|null,
 *   weakestSkill: { label: string, value: number }|null
 * }} params
 * @returns {{
 *   badge: string,
 *   icon: string,
 *   title: string,
 *   reason: string,
 *   modeName: string,
 *   actionText: string,
 *   buttonLabel: string,
 *   path: string
 * }}
 */
function computeRecommendation({ hasReviewedSessions, strongestSkill, weakestSkill }) {
  if (!hasReviewedSessions) {
    return {
      badge: 'Getting Started',
      icon: '💡',
      title: 'Get Your First Insight',
      reason:
        'Complete a practice session and generate AI feedback to see which skills you can focus on.',
      modeName: 'Off the Cuff',
      actionText: 'Practice impromptu speaking without preparation.',
      buttonLabel: 'Start Off the Cuff Practice',
      path: '/off-the-cuff',
    }
  }

  // If skills are tied or single skill
  const hasDistinctWeakest =
    weakestSkill &&
    strongestSkill &&
    weakestSkill.label !== strongestSkill.label

  const targetSkill = hasDistinctWeakest ? weakestSkill.label : 'Balanced'

  switch (targetSkill) {
    case 'Structure':
      return {
        badge: 'Recommended Action',
        icon: '🔍',
        title: 'Focus on Structure',
        reason:
          'Your recent scores show Structure as the area with the most room to develop.',
        modeName: 'Research',
        actionText:
          'Practice organizing ideas and supporting your response with preparation notes.',
        buttonLabel: 'Start Research Practice',
        path: '/research',
      }
    case 'Relevance':
      return {
        badge: 'Recommended Action',
        icon: '⚔️',
        title: 'Focus on Relevance',
        reason:
          'Your recent scores show Relevance as the area with the most room to develop.',
        modeName: 'Debate',
        actionText:
          'Practice defending a clear stance and keeping your arguments directly tied to the topic.',
        buttonLabel: 'Start Debate Practice',
        path: '/debate',
      }
    case 'Clarity':
      return {
        badge: 'Recommended Action',
        icon: '⚡',
        title: 'Focus on Clarity',
        reason:
          'Your recent scores show Clarity as the area with the most room to develop.',
        modeName: 'Off the Cuff',
        actionText:
          'Practice expressing ideas concisely and articulating thoughts without over-preparing.',
        buttonLabel: 'Start Off the Cuff Practice',
        path: '/off-the-cuff',
      }
    default:
      // When all skills are even or tied
      return {
        badge: 'Recommended Action',
        icon: '📖',
        title: 'Develop Narrative Flow',
        reason:
          'Your skill scores are well-balanced across clarity, relevance, and structure.',
        modeName: 'Story',
        actionText:
          'Practice storytelling to combine structure, creativity, and expressive delivery.',
        buttonLabel: 'Start Story Practice',
        path: '/story',
      }
  }
}

// ---------------------------------------------------------------------------
// SkillSparkline — lightweight per-skill SVG trendline
// ---------------------------------------------------------------------------
const SPARKLINE_H = 32
const SPARKLINE_PAD = { top: 4, right: 6, bottom: 4, left: 6 }

function SkillSparkline({ data, skillName }) {
  const containerRef = useRef(null)
  const [width, setWidth] = useState(120)

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const ro = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.contentRect.width > 0) {
          setWidth(entry.contentRect.width)
        }
      }
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  if (!data || data.length === 0) return null

  const innerW = Math.max(width - SPARKLINE_PAD.left - SPARKLINE_PAD.right, 10)
  const innerH = SPARKLINE_H - SPARKLINE_PAD.top - SPARKLINE_PAD.bottom

  const clamp = (val) => Math.max(0, Math.min(100, val))
  const toY = (score) => SPARKLINE_PAD.top + (1 - clamp(score) / 100) * innerH

  const toX = (i) => {
    if (data.length === 1) return SPARKLINE_PAD.left + innerW / 2
    return SPARKLINE_PAD.left + (i / (data.length - 1)) * innerW
  }

  const polylinePoints = data.map((v, i) => `${toX(i).toFixed(1)},${toY(v).toFixed(1)}`).join(' ')
  const midY = toY(50)

  return (
    <div ref={containerRef} className={styles.sparklineContainer}>
      <div className={styles.sparklineHeader}>
        <span className={styles.sparklineLabel}>{skillName ? `${skillName} Trend` : 'Trend'}</span>
        <span className={styles.sparklineSessionCount}>
          {data.length} session{data.length !== 1 ? 's' : ''}
        </span>
      </div>
      <svg
        className={styles.sparklineSvg}
        width={width}
        height={SPARKLINE_H}
        viewBox={`0 0 ${width} ${SPARKLINE_H}`}
        aria-hidden="true"
        focusable="false"
      >
        {/* Subtle 50% guideline for reference */}
        <line
          x1={SPARKLINE_PAD.left}
          y1={midY}
          x2={width - SPARKLINE_PAD.right}
          y2={midY}
          className={styles.sparklineGuide}
        />

        {data.length === 1 ? (
          /* Single point: centered dot without misleading line */
          <circle
            cx={toX(0)}
            cy={toY(data[0])}
            r="3.5"
            className={styles.sparklineDot}
          />
        ) : (
          <>
            {/* Trend line */}
            <polyline
              points={polylinePoints}
              className={styles.sparklineLine}
            />
            {/* End dot showing latest score */}
            <circle
              cx={toX(data.length - 1)}
              cy={toY(data[data.length - 1])}
              r="3"
              className={styles.sparklineDot}
            />
          </>
        )}
      </svg>
    </div>
  )
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
  const activeDaysLast7 = data?.activeDaysLast7 ?? 0
  const activeDaysLast14 = data?.activeDaysLast14 ?? 0

  const hasSessions = summary.totalCompletedSessions > 0
  const hasReviewedSessions = summary.reviewedSessionsCount > 0

  // Personalized Insights — computed from existing API data, no new fetch
  const { strongest: strongestSkill, weakest: weakestSkill } = computeSkillInsights(skills)
  const trendKey = computeScoreTrend(scoreHistory)
  const trend = TREND_CONFIG[trendKey]

  // Recommended Practice Action — deterministic mapping from skills to existing practice modes
  const recommendation = computeRecommendation({
    hasReviewedSessions,
    strongestSkill,
    weakestSkill,
  })

  // Per-skill chronological score histories for sparklines
  const clarityHistory = scoreHistory
    .map((p) => p.clarityScore ?? p.clarity)
    .filter((v) => typeof v === 'number' && !isNaN(v))
  const relevanceHistory = scoreHistory
    .map((p) => p.relevanceScore ?? p.relevance)
    .filter((v) => typeof v === 'number' && !isNaN(v))
  const structureHistory = scoreHistory
    .map((p) => p.structureScore ?? p.structure)
    .filter((v) => typeof v === 'number' && !isNaN(v))

  const handlePracticeAgain = (item) => {
    if (!item?.promptId) return

    const promptObj = {
      id: item.promptId,
      text: item.promptText,
      mode: item.mode,
    }

    const modeRoutes = {
      STORY: '/story',
      DEBATE: '/debate',
      RESEARCH: '/research',
      OFF_THE_CUFF: '/off-the-cuff',
    }

    const targetRoute = modeRoutes[item.mode] || '/off-the-cuff'
    navigate(targetRoute, { state: { prompt: promptObj } })
  }

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

          {/* Practice Activity / Streak Row */}
          <div className={styles.activityStatsGrid}>
            <div className={styles.activityStatCard}>
              <div className={styles.activityStatTop}>
                <span className={styles.activityStatLabel}>7-Day Practice Activity</span>
                <span className={styles.activityStatBadge}>Last 7 Days</span>
              </div>
              <div className={styles.activityStatValueRow}>
                <span className={styles.activityStatValue}>{activeDaysLast7}</span>
                <span className={styles.activityStatTotal}>/ 7 days active</span>
              </div>
              <span className={styles.activityStatHint}>
                {activeDaysLast7 === 0
                  ? 'No practice sessions in the last 7 days.'
                  : `${activeDaysLast7} distinct day${activeDaysLast7 !== 1 ? 's' : ''} practiced this week`}
              </span>
            </div>

            <div className={styles.activityStatCard}>
              <div className={styles.activityStatTop}>
                <span className={styles.activityStatLabel}>14-Day Practice Activity</span>
                <span className={styles.activityStatBadge}>Last 14 Days</span>
              </div>
              <div className={styles.activityStatValueRow}>
                <span className={styles.activityStatValue}>{activeDaysLast14}</span>
                <span className={styles.activityStatTotal}>/ 14 days active</span>
              </div>
              <span className={styles.activityStatHint}>
                {activeDaysLast14 === 0
                  ? 'No practice sessions in the last 14 days.'
                  : `${activeDaysLast14} distinct day${activeDaysLast14 !== 1 ? 's' : ''} practiced in the past 2 weeks`}
              </span>
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

          {/* Personalized Insights */}
          {hasReviewedSessions && (
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <h2 className={styles.sectionTitle}>Personalized Insights</h2>
                <span className={styles.sectionSubtitle}>
                  Based on your AI-reviewed sessions
                </span>
              </div>

              <div className={styles.insightGrid}>
                {/* Strongest Skill */}
                <div className={styles.insightCard}>
                  <span className={styles.insightLabel}>Strongest Skill</span>
                  {strongestSkill ? (
                    <>
                      <span className={styles.insightValue}>{strongestSkill.label}</span>
                      <span className={styles.insightScore}>{strongestSkill.value} / 100</span>
                    </>
                  ) : (
                    <span className={styles.insightEmpty}>Not enough feedback yet</span>
                  )}
                </div>

                {/* Area to focus on */}
                <div className={styles.insightCard}>
                  <span className={styles.insightLabel}>Area to Focus On</span>
                  {weakestSkill &&
                  strongestSkill &&
                  weakestSkill.label !== strongestSkill.label ? (
                    <>
                      <span className={styles.insightValue}>{weakestSkill.label}</span>
                      <span className={styles.insightScore}>{weakestSkill.value} / 100</span>
                    </>
                  ) : weakestSkill ? (
                    /* All skills tied or only one skill — no meaningful distinction */
                    <span className={styles.insightEmpty}>All skills are even</span>
                  ) : (
                    <span className={styles.insightEmpty}>Not enough feedback yet</span>
                  )}
                </div>

                {/* Performance Trend */}
                <div className={styles.insightCard}>
                  <span className={styles.insightLabel}>Performance Trend</span>
                  <span className={`${styles.insightTrend} ${styles[trend.className]}`}>
                    {trend.symbol} {trend.label}
                  </span>
                  {trendKey === 'insufficient' && (
                    <span className={styles.insightTrendHint}>
                      Need {Math.max(0, 4 - scoreHistory.length)} more reviewed session
                      {4 - scoreHistory.length !== 1 ? 's' : ''}
                    </span>
                  )}
                </div>
              </div>
            </section>
          )}

          {/* Recommended Practice Action */}
          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle}>Recommended Practice</h2>
              <span className={styles.sectionSubtitle}>
                Targeted practice based on your skill analysis
              </span>
            </div>

            <div className={styles.recommendationCard}>
              <div className={styles.recommendationTop}>
                <span className={styles.recommendationIcon} aria-hidden="true">
                  {recommendation.icon}
                </span>
                <div className={styles.recommendationContent}>
                  <div className={styles.recommendationHeaderRow}>
                    <span className={styles.recommendationBadge}>
                      {recommendation.badge}
                    </span>
                    <h3 className={styles.recommendationTitle}>
                      {recommendation.title}
                    </h3>
                  </div>
                  <p className={styles.recommendationReason}>
                    {recommendation.reason}
                  </p>
                </div>
              </div>

              <div className={styles.recommendationProposal}>
                <span className={styles.recommendationModeLabel}>
                  Try {recommendation.modeName}
                </span>
                <span className={styles.recommendationActionText}>
                  {recommendation.actionText}
                </span>
              </div>

              <div className={styles.recommendationActions}>
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => navigate(recommendation.path)}
                >
                  {recommendation.buttonLabel}
                </Button>
              </div>
            </div>
          </section>

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
                <SkillSparkline data={clarityHistory} skillName="Clarity" />
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
                <SkillSparkline data={relevanceHistory} skillName="Relevance" />
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
                <SkillSparkline data={structureHistory} skillName="Structure" />
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
                      {item.promptId && (
                        <button
                          type="button"
                          className={styles.practiceAgainBtn}
                          onClick={() => handlePracticeAgain(item)}
                          title={`Practice again in ${formatMode(item.mode)}`}
                        >
                          Practice Again →
                        </button>
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
