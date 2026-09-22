import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import CategoryBadge from '../../components/prompt/CategoryBadge'
import SpeakingTimer from '../../components/timer/SpeakingTimer'
import TimerControls from '../../components/timer/TimerControls'
import LiveTranscript from '../../components/speaking/LiveTranscript'
import Button from '../../components/common/Button'
import { getRandomPrompt, getCategories } from '../../api/promptApi'
import { createSession, completeSession, abandonSession } from '../../api/sessionApi'
import { bookmarkPrompt, removeBookmark, isBookmarked } from '../../api/bookmarkApi'
import useTimer from '../../hooks/useTimer'
import useSpeechRecognition from '../../hooks/useSpeechRecognition'
import { formatTime } from '../../utils/formatTime'
import styles from './StoryPage.module.css'

const PREP_TIME_SECONDS = 300 // 5 minutes
const DURATION_OPTIONS = [
  { label: '1 Minute', seconds: 60 },
  { label: '2 Minutes', seconds: 120 },
  { label: '3 Minutes', seconds: 180 },
]

export default function StoryPage() {
  const location = useLocation()
  const navigate = useNavigate()

  const [prompt, setPrompt] = useState(null)
  const [categories, setCategories] = useState([])
  const [selectedCategory, setSelectedCategory] = useState(null)
  const [speakingDuration, setSpeakingDuration] = useState(120) // Default 2 min
  const [prepNotes, setPrepNotes] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)
  const [togglingBookmark, setTogglingBookmark] = useState(false)

  // Speaking state: 'PREPARING' | 'LISTENING' | 'PAUSED' | 'COMPLETED'
  const [speakingState, setSpeakingState] = useState('PREPARING')

  const sessionIdRef = useRef(null)
  const prepTimeLeftRef = useRef(PREP_TIME_SECONDS)
  const speakingTimeLeftRef = useRef(speakingDuration)
  const prepDurationRef = useRef(0)

  // Speech Recognition (reusing Phase 3 engine)
  const {
    transcript,
    interimTranscript,
    isSupported: isSpeechSupported,
    error: speechError,
    startListening,
    stopListening,
    pauseListening,
    resumeListening,
    resetTranscript,
  } = useSpeechRecognition()

  // Finish session handler (shared between timer expiry and early finish)
  const handleFinishSpeaking = useCallback(async () => {
    const elapsedSpeaking = Math.max(1, speakingDuration - speakingTimeLeftRef.current)
    const finalSpeech = stopListening()
    const capturedTranscript = (finalSpeech || transcript || '').trim()

    const currentSessionId = sessionIdRef.current
    if (currentSessionId) {
      try {
        await completeSession(currentSessionId, {
          transcript: capturedTranscript,
          actualDurationSeconds: elapsedSpeaking,
          preparationNotes: prepNotes.trim() || null,
          preparationDurationSeconds: prepDurationRef.current,
        })
      } catch (err) {
        console.error('Failed to complete Story session:', err)
      }
      navigate(`/story/session/${currentSessionId}`)
      return
    }

    setSpeakingState('COMPLETED')
  }, [navigate, prepNotes, speakingDuration, stopListening, transcript])

  // Speaking countdown timer
  const handleSpeakingTimerComplete = useCallback(() => {
    handleFinishSpeaking()
  }, [handleFinishSpeaking])

  const {
    timeLeft: speakingTimeLeft,
    start: startSpeakingTimer,
    pause: pauseSpeakingTimer,
    reset: resetSpeakingTimer,
  } = useTimer(speakingDuration, handleSpeakingTimerComplete)

  // Prep timer (5:00 countdown)
  const handlePrepTimerComplete = useCallback(() => {
    // Prep timer reached 0:00
  }, [])

  const {
    timeLeft: prepTimeLeft,
    isRunning: isPrepRunning,
    isComplete: isPrepComplete,
    start: startPrepTimer,
    pause: pausePrepTimer,
    reset: resetPrepTimer,
  } = useTimer(PREP_TIME_SECONDS, handlePrepTimerComplete)

  // Sync timer refs
  useEffect(() => {
    prepTimeLeftRef.current = prepTimeLeft
  }, [prepTimeLeft])

  useEffect(() => {
    speakingTimeLeftRef.current = speakingTimeLeft
  }, [speakingTimeLeft])

  // Load Story categories on mount
  useEffect(() => {
    getCategories('STORY')
      .then((data) => {
        if (Array.isArray(data)) {
          setCategories(data)
        }
      })
      .catch((err) => console.error('Failed to load Story categories:', err))
  }, [])

  // Load initial prompt — from navigation state (Bookshelf) or fetch random
  useEffect(() => {
    if (location.state?.prompt) {
      setPrompt(location.state.prompt)
      if (location.state.prompt.category) {
        setSelectedCategory(location.state.prompt.category)
      }
      setLoading(false)
      window.history.replaceState({}, document.title)
    } else {
      fetchPrompt()
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

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

    return () => {
      cancelled = true
    }
  }, [prompt?.id])

  async function handleToggleBookmark(e) {
    e?.stopPropagation()
    if (!prompt?.id || togglingBookmark) return

    setTogglingBookmark(true)
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
      setTogglingBookmark(false)
    }
  }

  async function fetchPrompt(category = selectedCategory, excludeId = null) {
    setLoading(true)
    setError(null)
    try {
      const data = await getRandomPrompt('STORY', category, excludeId)
      setPrompt(data)
    } catch (err) {
      setError('Failed to load story scenario. Is the backend running?')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  function handleShuffle() {
    if (speakingState !== 'PREPARING') return
    resetPrepTimer(PREP_TIME_SECONDS)
    fetchPrompt(selectedCategory, prompt?.id)
  }

  function handleCategoryClick(categoryName) {
    if (speakingState !== 'PREPARING') return
    const newCategory = categoryName === selectedCategory ? null : categoryName
    setSelectedCategory(newCategory)
    resetPrepTimer(PREP_TIME_SECONDS)
    fetchPrompt(newCategory)
  }

  function handleSelectDuration(seconds) {
    if (speakingState !== 'PREPARING') return
    setSpeakingDuration(seconds)
    resetSpeakingTimer(seconds)
  }

  // Start speaking action
  async function handleStartSpeaking() {
    if (!prompt) return

    // Stop preparation timer and calculate exact prep duration in seconds
    pausePrepTimer()
    const elapsedPrep = PREP_TIME_SECONDS - prepTimeLeftRef.current
    const actualPrepSeconds = isPrepRunning || elapsedPrep > 0 ? Math.max(1, elapsedPrep) : 0
    prepDurationRef.current = actualPrepSeconds

    resetTranscript()
    resetSpeakingTimer(speakingDuration)
    setSpeakingState('LISTENING')

    // Create session in backend
    try {
      const session = await createSession({
        promptText: prompt.text,
        promptId: prompt.id,
        mode: 'STORY',
        durationSeconds: speakingDuration,
        preparationNotes: prepNotes.trim() || null,
      })
      sessionIdRef.current = session.id
    } catch (err) {
      console.error('Failed to create Story session:', err)
    }

    startListening()
    startSpeakingTimer()
  }

  function handlePause() {
    pauseSpeakingTimer()
    pauseListening()
    setSpeakingState('PAUSED')
  }

  function handleResume() {
    startSpeakingTimer()
    resumeListening()
    setSpeakingState('LISTENING')
  }

  function handleReset() {
    if (sessionIdRef.current) {
      abandonSession(sessionIdRef.current).catch((err) =>
        console.error('Failed to abandon Story session:', err)
      )
      sessionIdRef.current = null
    }

    stopListening()
    resetTranscript()
    resetSpeakingTimer(speakingDuration)
    setSpeakingState('PREPARING')
  }

  // Clean up if component unmounts while session is active
  useEffect(() => {
    return () => {
      if (sessionIdRef.current && speakingState !== 'COMPLETED') {
        abandonSession(sessionIdRef.current).catch(() => {})
      }
      stopListening()
    }
  }, [speakingState, stopListening])

  // Calculate notes word count
  const noteWords = prepNotes.trim() ? prepNotes.trim().split(/\s+/).length : 0

  // Active speaking view (LISTENING or PAUSED)
  if (speakingState === 'LISTENING' || speakingState === 'PAUSED') {
    return (
      <div className={styles.page}>
        <header className={styles.header}>
          <div className={styles.stageIndicator}>
            <span className={styles.stagePill}>Stage 2: Story Delivery</span>
            <span className={styles.metaPrepPill}>
              Prepared for {formatTime(prepDurationRef.current)}
            </span>
          </div>
          <h1 className={styles.title}>Tell Your Story</h1>
        </header>

        {/* Active Story Scenario Card */}
        <div className={styles.activeQuestionCard}>
          <div className={styles.cardHeader}>
            <div className={styles.headerBadges}>
              <span className={styles.storyTag}>Scenario</span>
              {prompt?.category && (
                <span className={styles.categoryPill}>{prompt.category}</span>
              )}
            </div>
          </div>
          <h2 className={styles.promptQuestion}>{prompt?.text}</h2>
        </div>

        {/* Speaker Notes reference during speech */}
        {prepNotes.trim() && (
          <div className={styles.activeSpeakerNotes}>
            <div className={styles.activeNotesHeader}>
              <span className={styles.activeNotesLabel}>Story Outline & Beats</span>
              <span className={styles.activeNotesWordCount}>{noteWords} words</span>
            </div>
            <div className={styles.activeNotesBody}>{prepNotes}</div>
          </div>
        )}

        {/* Speaking Timer */}
        <SpeakingTimer
          timeLeft={speakingTimeLeft}
          isRunning={speakingState === 'LISTENING'}
          isComplete={speakingTimeLeft === 0}
        />

        {/* Live Speech Recognition Transcript */}
        <LiveTranscript
          transcript={transcript}
          interimTranscript={interimTranscript}
          isListening={speakingState === 'LISTENING'}
          isPaused={speakingState === 'PAUSED'}
          isSupported={isSpeechSupported}
          error={speechError}
        />

        {/* Timer & Session Controls */}
        <TimerControls
          durations={[60, 120, 180]}
          selectedDuration={speakingDuration}
          onSelectDuration={handleSelectDuration}
          speakingState={speakingState}
          onStart={handleStartSpeaking}
          onPause={handlePause}
          onResume={handleResume}
          onFinish={handleFinishSpeaking}
          onReset={handleReset}
        />
      </div>
    )
  }

  // Preparation Stage View
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerBadge}>Story Mode</div>
        <h1 className={styles.title}>Get a Scenario. Tell the Story.</h1>
        <p className={styles.subtitle}>
          Read the scenario, outline your narrative arc, and deliver a compelling, structured story.
        </p>
      </header>

      {/* Category filter */}
      <div className={styles.categories}>
        <CategoryBadge
          category="All"
          active={selectedCategory === null}
          onClick={() => handleCategoryClick(null)}
        />
        {categories.map((cat) => (
          <CategoryBadge
            key={cat.id}
            category={cat.name}
            active={selectedCategory === cat.name}
            onClick={() => handleCategoryClick(cat.name)}
          />
        ))}
      </div>

      {/* Story Scenario Card */}
      <div className={styles.questionCard}>
        <div className={styles.cardHeader}>
          <div className={styles.headerBadges}>
            <span className={styles.storyTag}>Scenario</span>
            {prompt?.category && (
              <span className={styles.categoryPill}>{prompt.category}</span>
            )}
          </div>
          {prompt?.id && (
            <button
              className={`${styles.bookmarkBtn} ${saved ? styles.bookmarkActive : ''}`}
              onClick={handleToggleBookmark}
              disabled={togglingBookmark}
              title={saved ? 'Remove from Bookshelf' : 'Save to Bookshelf'}
              aria-label={saved ? 'Remove from Bookshelf' : 'Save to Bookshelf'}
            >
              {saved ? '★ Saved' : '☆ Save'}
            </button>
          )}
        </div>

        {error ? (
          <div className={styles.error}>{error}</div>
        ) : loading ? (
          <div className={styles.loadingQuestion}>Loading story scenario...</div>
        ) : (
          <h2 className={styles.promptQuestion}>&ldquo;{prompt?.text}&rdquo;</h2>
        )}

        <div className={styles.cardFooter}>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleShuffle}
            disabled={loading}
          >
            ↻ Shuffle Scenario
          </Button>
        </div>
      </div>

      {/* Preparation Section: Timer & Notes Grid */}
      <div className={styles.prepGrid}>
        {/* Left Column: 5-Min Timer & Duration Choice */}
        <div className={styles.timerColumn}>
          <div className={styles.prepTimerCard}>
            <div className={styles.timerHeader}>
              <span className={styles.prepLabel}>Preparation Timer</span>
              <span
                className={`${styles.prepStatusBadge} ${
                  isPrepRunning
                    ? styles.statusRunning
                    : isPrepComplete
                    ? styles.statusComplete
                    : ''
                }`}
              >
                {isPrepRunning
                  ? 'Counting Down'
                  : isPrepComplete
                  ? "Time's Up!"
                  : prepTimeLeft < PREP_TIME_SECONDS
                  ? 'Paused'
                  : '5:00 Allocated'}
              </span>
            </div>

            <div
              className={`${styles.timeDisplay} ${
                isPrepRunning ? styles.timeRunning : ''
              } ${prepTimeLeft <= 30 && prepTimeLeft > 0 ? styles.timeUrgent : ''}`}
            >
              {formatTime(prepTimeLeft)}
            </div>

            <div className={styles.timerControls}>
              {!isPrepRunning ? (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={startPrepTimer}
                  disabled={isPrepComplete}
                >
                  {prepTimeLeft < PREP_TIME_SECONDS ? '▶ Resume Prep' : '▶ Start 5:00 Prep'}
                </Button>
              ) : (
                <Button variant="ghost" size="sm" onClick={pausePrepTimer}>
                  ⏸ Pause
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={() => resetPrepTimer(PREP_TIME_SECONDS)}
                disabled={prepTimeLeft === PREP_TIME_SECONDS && !isPrepRunning}
              >
                ↺ Reset
              </Button>
            </div>
          </div>

          {/* Speaking Duration Selector */}
          <div className={styles.durationSelectorCard}>
            <span className={styles.durationLabel}>Speaking Duration Target</span>
            <div className={styles.durationOptions}>
              {DURATION_OPTIONS.map((opt) => (
                <button
                  key={opt.seconds}
                  type="button"
                  className={`${styles.durationBtn} ${
                    speakingDuration === opt.seconds ? styles.durationActive : ''
                  }`}
                  onClick={() => handleSelectDuration(opt.seconds)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Optional Notes */}
        <div className={styles.notesColumn}>
          <div className={styles.notesCard}>
            <div className={styles.notesHeader}>
              <label htmlFor="prepNotes" className={styles.notesLabel}>
                Story Outline & Beats (Optional)
              </label>
              <span className={styles.wordCount}>{noteWords} words</span>
            </div>
            <p className={styles.notesHint}>
              Outline your narrative arc: Hook → Setting/Character → Conflict → Climax → Resolution. Notes stay visible during speaking.
            </p>
            <textarea
              id="prepNotes"
              className={styles.notesTextarea}
              value={prepNotes}
              onChange={(e) => setPrepNotes(e.target.value)}
              placeholder="1. Hook: An intriguing opening line or sudden event...&#10;2. Setting & Character: Who is involved and where are we?&#10;3. Conflict / Complication: What goes wrong or raises the stakes?&#10;4. Climax: The turning point or moment of highest tension...&#10;5. Resolution: The aftermath, lesson, or lasting impact..."
              rows={7}
            />
          </div>
        </div>
      </div>

      {/* Start Speaking Action Bar */}
      <div className={styles.startSpeakingBar}>
        <Button
          variant="primary"
          size="lg"
          className={styles.startSpeakingBtn}
          onClick={handleStartSpeaking}
          disabled={loading || !prompt}
        >
          🎙️ Start Speaking
        </Button>
        <span className={styles.startSpeakingSubtext}>
          You can start speaking at any time — you do not need to wait for the preparation timer to finish.
        </span>
      </div>
    </div>
  )
}
