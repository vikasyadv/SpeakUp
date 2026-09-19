import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation } from 'react-router-dom'
import PromptShuffle from '../../components/prompt/PromptShuffle'
import CategoryBadge from '../../components/prompt/CategoryBadge'
import SpeakingTimer from '../../components/timer/SpeakingTimer'
import TimerControls from '../../components/timer/TimerControls'
import LiveTranscript from '../../components/speaking/LiveTranscript'
import SpeakingResult from './SpeakingResult'
import Button from '../../components/common/Button'
import { getRandomPrompt, getCategories } from '../../api/promptApi'
import { createSession, completeSession, abandonSession } from '../../api/sessionApi'
import useTimer from '../../hooks/useTimer'
import useSpeechRecognition from '../../hooks/useSpeechRecognition'
import { TIMER_OPTIONS } from '../../utils/constants'
import styles from './OffTheCuffPage.module.css'

export default function OffTheCuffPage() {
  const location = useLocation()
  const [prompt, setPrompt] = useState(null)
  const [categories, setCategories] = useState([])
  const [selectedCategory, setSelectedCategory] = useState(null)
  const [selectedDuration, setSelectedDuration] = useState(60)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Speaking state: 'READY' | 'LISTENING' | 'PAUSED' | 'COMPLETED'
  const [speakingState, setSpeakingState] = useState('READY')
  const [completedSessionData, setCompletedSessionData] = useState(null)

  const sessionIdRef = useRef(null)
  const timeLeftRef = useRef(selectedDuration)

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
    const elapsed = Math.max(1, selectedDuration - timeLeftRef.current)
    const finalSpeech = stopListening()
    const capturedTranscript = (finalSpeech || transcript || '').trim()

    if (sessionIdRef.current) {
      try {
        await completeSession(sessionIdRef.current, {
          transcript: capturedTranscript,
          actualDurationSeconds: elapsed,
        })
      } catch (err) {
        console.error('Failed to complete session:', err)
      }
    }

    setCompletedSessionData({
      prompt,
      durationSeconds: selectedDuration,
      actualDurationSeconds: elapsed,
      transcript: capturedTranscript,
    })

    setSpeakingState('COMPLETED')
  }, [prompt, selectedDuration, stopListening, transcript])

  const handleTimerComplete = useCallback(() => {
    handleFinishSpeaking()
  }, [handleFinishSpeaking])

  const { timeLeft, isRunning, isComplete, start, pause, reset } = useTimer(
    selectedDuration,
    handleTimerComplete
  )

  // Keep timeLeftRef in sync with current timeLeft
  useEffect(() => {
    timeLeftRef.current = timeLeft
  }, [timeLeft])

  // Fetch categories on mount
  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err))
  }, [])

  // Load initial prompt — from navigation state (Bookshelf) or fetch random
  useEffect(() => {
    if (location.state?.prompt) {
      setPrompt(location.state.prompt)
      setLoading(false)
      window.history.replaceState({}, document.title)
    } else {
      fetchPrompt()
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function fetchPrompt(category = selectedCategory, excludeId = null) {
    setLoading(true)
    setError(null)
    try {
      const data = await getRandomPrompt('OFF_THE_CUFF', category, excludeId)
      setPrompt(data)
    } catch (err) {
      setError('Failed to load prompt. Is the backend running?')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  function handleShuffle() {
    if (speakingState !== 'READY') return
    fetchPrompt(selectedCategory, prompt?.id)
  }

  function handleCategoryClick(categoryName) {
    if (speakingState !== 'READY') return
    const newCategory = categoryName === selectedCategory ? null : categoryName
    setSelectedCategory(newCategory)
    fetchPrompt(newCategory)
  }

  function handleSelectDuration(duration) {
    if (speakingState !== 'READY') return
    setSelectedDuration(duration)
    reset(duration)
  }

  async function handleStart() {
    if (!prompt) return

    resetTranscript()
    setSpeakingState('LISTENING')

    // Create session in backend
    try {
      const session = await createSession({
        promptText: prompt.text,
        promptId: prompt.id,
        mode: 'OFF_THE_CUFF',
        durationSeconds: selectedDuration,
      })
      sessionIdRef.current = session.id
    } catch (err) {
      console.error('Failed to create session:', err)
    }

    startListening()
    start()
  }

  function handlePause() {
    pause()
    pauseListening()
    setSpeakingState('PAUSED')
  }

  function handleResume() {
    start()
    resumeListening()
    setSpeakingState('LISTENING')
  }

  function handleReset() {
    if (sessionIdRef.current) {
      abandonSession(sessionIdRef.current).catch((err) =>
        console.error('Failed to abandon session:', err)
      )
      sessionIdRef.current = null
    }

    stopListening()
    resetTranscript()
    reset(selectedDuration)
    setSpeakingState('READY')
  }

  function handlePracticeAgain() {
    sessionIdRef.current = null
    resetTranscript()
    reset(selectedDuration)
    setCompletedSessionData(null)
    setSpeakingState('READY')
  }

  function handleBackToOffTheCuff() {
    sessionIdRef.current = null
    resetTranscript()
    reset(selectedDuration)
    setCompletedSessionData(null)
    setSpeakingState('READY')
    fetchPrompt(selectedCategory, prompt?.id)
  }

  // If in COMPLETED state, show the SpeakingResult review screen
  if (speakingState === 'COMPLETED' && completedSessionData) {
    return (
      <div className={styles.page}>
        <SpeakingResult
          prompt={completedSessionData.prompt}
          durationSeconds={completedSessionData.durationSeconds}
          actualDurationSeconds={completedSessionData.actualDurationSeconds}
          transcript={completedSessionData.transcript}
          onPracticeAgain={handlePracticeAgain}
          onBackToOffTheCuff={handleBackToOffTheCuff}
        />
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Off the Cuff</h1>
      <p className={styles.subtitle}>No prep. No notes. Just speak.</p>

      {/* Category filter */}
      <div
        className={`${styles.categories} ${
          speakingState !== 'READY' ? styles.categoriesDisabled : ''
        }`}
      >
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

      {/* Prompt card with shuffle animation */}
      {error ? (
        <div className={styles.error}>{error}</div>
      ) : (
        <PromptShuffle prompt={prompt} />
      )}

      {/* Shuffle button — only when READY */}
      {speakingState === 'READY' && (
        <div className={styles.shuffleRow}>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleShuffle}
            disabled={loading}
          >
            ↻ New Prompt
          </Button>
        </div>
      )}

      {/* Speaking Timer */}
      <SpeakingTimer
        timeLeft={timeLeft}
        isRunning={speakingState === 'LISTENING'}
        isComplete={isComplete}
      />

      {/* Live Transcript (during active speaking or paused) */}
      {(speakingState === 'LISTENING' || speakingState === 'PAUSED') && (
        <LiveTranscript
          transcript={transcript}
          interimTranscript={interimTranscript}
          isListening={speakingState === 'LISTENING'}
          isPaused={speakingState === 'PAUSED'}
          isSupported={isSpeechSupported}
          error={speechError}
        />
      )}

      {/* Timer Controls */}
      <TimerControls
        durations={TIMER_OPTIONS}
        selectedDuration={selectedDuration}
        onSelectDuration={handleSelectDuration}
        speakingState={speakingState}
        onStart={handleStart}
        onPause={handlePause}
        onResume={handleResume}
        onFinish={handleFinishSpeaking}
        onReset={handleReset}
      />
    </div>
  )
}
