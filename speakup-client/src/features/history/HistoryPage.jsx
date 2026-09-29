import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSessions, deleteSession } from '../../api/sessionApi'
import { formatTime } from '../../utils/formatTime'
import styles from './HistoryPage.module.css'

const PAGE_SIZE = 20

const MODE_OPTIONS = [
  { value: 'ALL', label: 'All Modes' },
  { value: 'OFF_THE_CUFF', label: 'Off the Cuff' },
  { value: 'RESEARCH', label: 'Research' },
  { value: 'DEBATE', label: 'Debate' },
  { value: 'STORY', label: 'Story' },
]

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'ABANDONED', label: 'Abandoned' },
]

export default function HistoryPage() {
  const navigate = useNavigate()

  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [expandedTranscripts, setExpandedTranscripts] = useState({})

  // Filters & pagination state
  // `search` = live input value; `committedSearch` = debounced value sent to API
  const [search, setSearch] = useState('')
  const [committedSearch, setCommittedSearch] = useState('')
  const [modeFilter, setModeFilter] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)

  // Page metadata from API
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  // Debounce timer ref
  const searchDebounceRef = useRef(null)
  // Request counter for race-condition protection — ignore stale responses
  const fetchIdRef = useRef(0)

  // hasActiveFilters: use live `search` (not debounced) so the Clear button
  // appears immediately when the user starts typing.
  const hasActiveFilters =
    modeFilter !== 'ALL' || statusFilter !== 'ALL' || search.trim() !== ''

  // ── Search input handler ──────────────────────────────────────────────────
  function handleSearchChange(e) {
    const val = e.target.value
    setSearch(val)
    clearTimeout(searchDebounceRef.current)
    searchDebounceRef.current = setTimeout(() => {
      setCommittedSearch(val)
      setPage(0)
    }, 350)
  }

  function clearSearch() {
    clearTimeout(searchDebounceRef.current)
    setSearch('')
    setCommittedSearch('')
    setPage(0)
  }

  // ── Filter handlers ───────────────────────────────────────────────────────
  function handleModeChange(e) {
    setModeFilter(e.target.value)
    setPage(0)
  }

  function handleStatusChange(e) {
    setStatusFilter(e.target.value)
    setPage(0)
  }

  function handleClearFilters() {
    clearTimeout(searchDebounceRef.current)
    setSearch('')
    setCommittedSearch('')
    setModeFilter('ALL')
    setStatusFilter('ALL')
    setPage(0)
  }

  // ── Fetch ─────────────────────────────────────────────────────────────────
  const fetchSessions = useCallback(async () => {
    const fetchId = ++fetchIdRef.current
    setLoading(true)
    setError(null)
    try {
      const data = await getSessions({
        mode: modeFilter,
        status: statusFilter,
        search: committedSearch,
        page,
        size: PAGE_SIZE,
      })
      // Ignore stale responses from earlier in-flight requests
      if (fetchId !== fetchIdRef.current) return
      setSessions(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
      setTotalElements(data.totalElements ?? 0)
    } catch (err) {
      if (fetchId !== fetchIdRef.current) return
      console.error('Failed to load sessions:', err)
      setError('Failed to load your history. Please try again.')
      setSessions([])
      setTotalPages(0)
      setTotalElements(0)
    } finally {
      // Only clear loading if this is still the latest request
      if (fetchId === fetchIdRef.current) {
        setLoading(false)
      }
    }
  }, [modeFilter, statusFilter, committedSearch, page])

  useEffect(() => {
    fetchSessions()
  }, [fetchSessions])

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => clearTimeout(searchDebounceRef.current)
  }, [])

  // ── Delete ────────────────────────────────────────────────────────────────
  async function handleDelete(sessionId) {
    try {
      await deleteSession(sessionId)
      // If deleting the last item on a non-first page, move back one page
      // (setPage triggers fetchSessions via the dependency). Otherwise re-fetch
      // the current page to reflect the updated total count.
      if (sessions.length === 1 && page > 0) {
        setPage((p) => p - 1)
      } else {
        fetchSessions()
      }
    } catch (err) {
      console.error('Failed to delete session:', err)
    }
  }

  // ── Transcript toggle ─────────────────────────────────────────────────────
  function toggleTranscript(sessionId) {
    setExpandedTranscripts((prev) => ({
      ...prev,
      [sessionId]: !prev[sessionId],
    }))
  }

  // ── Formatters ────────────────────────────────────────────────────────────
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
      case 'COMPLETED':
        return 'Completed'
      case 'IN_PROGRESS':
        return 'In Progress'
      case 'ABANDONED':
        return 'Abandoned'
      default:
        return status
    }
  }

  function getStatusClass(status) {
    switch (status) {
      case 'COMPLETED':
        return styles.statusCompleted
      case 'ABANDONED':
        return styles.statusAbandoned
      default:
        return styles.statusProgress
    }
  }

  // ── Derived subtitle ──────────────────────────────────────────────────────
  function getSubtitle() {
    if (loading) return '\u00a0'
    if (error) return '\u00a0'
    if (totalElements > 0) {
      return `${totalElements} session${totalElements !== 1 ? 's' : ''} found`
    }
    if (hasActiveFilters) {
      return 'No sessions match your filters.'
    }
    return 'No sessions yet. Start speaking to build your history.'
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className={styles.page}>
      <h1 className={styles.title}>History</h1>
      <p className={styles.subtitle}>{getSubtitle()}</p>

      {/* ── Search & Filter Bar ── */}
      <div className={styles.filterBar}>
        <div className={styles.searchWrapper}>
          <span className={styles.searchIcon} aria-hidden="true">⌕</span>
          <input
            type="search"
            className={styles.searchInput}
            placeholder="Search prompts, transcripts, notes…"
            value={search}
            onChange={handleSearchChange}
            aria-label="Search sessions"
          />
          {search && (
            <button
              className={styles.searchClearBtn}
              onClick={clearSearch}
              aria-label="Clear search"
            >
              ✕
            </button>
          )}
        </div>

        <div className={styles.filterGroup}>
          <select
            className={styles.filterSelect}
            value={modeFilter}
            onChange={handleModeChange}
            aria-label="Filter by mode"
          >
            {MODE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          <select
            className={styles.filterSelect}
            value={statusFilter}
            onChange={handleStatusChange}
            aria-label="Filter by status"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>

          {hasActiveFilters && (
            <button
              className={styles.clearFiltersBtn}
              onClick={handleClearFilters}
              aria-label="Clear all filters"
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {/* ── Loading State ── */}
      {loading && (
        <div className={styles.loadingRow}>
          <div className={styles.spinner} />
          <span className={styles.loadingText}>Loading sessions…</span>
        </div>
      )}

      {/* ── Error State ── */}
      {!loading && error && (
        <div className={styles.errorBanner}>
          <span>{error}</span>
          <button className={styles.retryBtn} onClick={fetchSessions}>Retry</button>
        </div>
      )}

      {/* ── Session List ── */}
      {!loading && !error && sessions.length > 0 && (
        <div className={styles.list}>
          {sessions.map((session) => {
            const hasTranscript = Boolean(session.transcript)
            const isExpanded = Boolean(expandedTranscripts[session.id])
            const speakingDuration = session.actualDurationSeconds ?? session.durationSeconds
            const isStory = session.mode === 'STORY'
            const isDebate = session.mode === 'DEBATE'
            const isResearch = session.mode === 'RESEARCH'
            const sessionPath = isStory
              ? `/story/session/${session.id}`
              : isDebate
              ? `/debate/session/${session.id}`
              : isResearch
              ? `/research/session/${session.id}`
              : `/off-the-cuff/session/${session.id}`

            return (
              <div key={session.id} className={styles.card}>
                <div className={styles.cardMain}>
                  <div className={styles.topRow}>
                    <span
                      className={`${styles.mode} ${
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
                    {session.stance && (
                      <span
                        className={`${styles.stanceBadge} ${
                          session.stance === 'FOR' ? styles.stanceFor : styles.stanceAgainst
                        }`}
                      >
                        {session.stance}
                      </span>
                    )}
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
                      {speakingDuration != null ? formatTime(speakingDuration) : '—'}
                      {session.actualDurationSeconds != null &&
                        session.actualDurationSeconds !== session.durationSeconds && (
                          <span className={styles.targetDuration}>
                            {' '}/ {formatTime(session.durationSeconds)}
                          </span>
                        )}
                    </span>
                    {(isStory || isDebate || isResearch) && session.preparationDurationSeconds != null && (
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

      {/* ── Empty State: no sessions at all (no filters active) ── */}
      {!loading && !error && sessions.length === 0 && !hasActiveFilters && (
        <div className={styles.emptyState}>
          <p className={styles.emptyText}>You haven&apos;t recorded any practice sessions yet.</p>
          <button
            className={styles.emptyCtaBtn}
            onClick={() => navigate('/')}
          >
            Start Practicing
          </button>
        </div>
      )}

      {/* ── Empty State: filters/search produced zero results ── */}
      {!loading && !error && sessions.length === 0 && hasActiveFilters && (
        <div className={styles.emptyState}>
          <p className={styles.emptyText}>No sessions match your current filters or search.</p>
          <button className={styles.clearFiltersBtn} onClick={handleClearFilters}>
            Clear Filters
          </button>
        </div>
      )}

      {/* ── Pagination ── */}
      {!loading && !error && totalPages > 1 && (
        <nav className={styles.pagination} aria-label="Session history pages">
          <button
            className={styles.pageBtn}
            onClick={() => setPage(0)}
            disabled={page === 0}
            aria-label="First page"
          >
            «
          </button>
          <button
            className={styles.pageBtn}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            aria-label="Previous page"
          >
            ‹
          </button>

          {Array.from({ length: totalPages }, (_, i) => i)
            .filter((i) => Math.abs(i - page) <= 2)
            .map((i) => (
              <button
                key={i}
                className={`${styles.pageBtn} ${i === page ? styles.pageBtnActive : ''}`}
                onClick={() => setPage(i)}
                aria-label={`Page ${i + 1}`}
                aria-current={i === page ? 'page' : undefined}
              >
                {i + 1}
              </button>
            ))}

          <button
            className={styles.pageBtn}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            aria-label="Next page"
          >
            ›
          </button>
          <button
            className={styles.pageBtn}
            onClick={() => setPage(totalPages - 1)}
            disabled={page >= totalPages - 1}
            aria-label="Last page"
          >
            »
          </button>
        </nav>
      )}

      {/* ── Page info ── */}
      {!loading && !error && totalElements > 0 && (
        <p className={styles.pageInfo}>
          Page {page + 1} of {totalPages} &mdash; {totalElements} total session{totalElements !== 1 ? 's' : ''}
        </p>
      )}
    </div>
  )
}
