import { useState, useEffect, useCallback } from 'react'
import PromptShuffle from '../../components/prompt/PromptShuffle'
import CategoryBadge from '../../components/prompt/CategoryBadge'
import SpeakingTimer from '../../components/timer/SpeakingTimer'
import TimerControls from '../../components/timer/TimerControls'
import Button from '../../components/common/Button'
import { getRandomPrompt, getCategories } from '../../api/promptApi'
import useTimer from '../../hooks/useTimer'
import { TIMER_OPTIONS } from '../../utils/constants'
import styles from './OffTheCuffPage.module.css'

export default function OffTheCuffPage() {
  const [prompt, setPrompt] = useState(null)
  const [categories, setCategories] = useState([])
  const [selectedCategory, setSelectedCategory] = useState(null)
  const [selectedDuration, setSelectedDuration] = useState(60)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const handleTimerComplete = useCallback(() => {
    // Timer finished — session complete
  }, [])

  const { timeLeft, isRunning, isComplete, start, pause, reset } = useTimer(
    selectedDuration,
    handleTimerComplete
  )

  // Fetch categories on mount
  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err))
  }, [])

  // Fetch initial prompt
  useEffect(() => {
    fetchPrompt()
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
    fetchPrompt(selectedCategory, prompt?.id)
  }

  function handleCategoryClick(categoryName) {
    const newCategory = categoryName === selectedCategory ? null : categoryName
    setSelectedCategory(newCategory)
    fetchPrompt(newCategory)
  }

  function handleSelectDuration(duration) {
    setSelectedDuration(duration)
    reset(duration)
  }

  function handleReset() {
    reset(selectedDuration)
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Off the Cuff</h1>
      <p className={styles.subtitle}>No prep. No notes. Just speak.</p>

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

      {/* Prompt card with shuffle animation */}
      {error ? (
        <div className={styles.error}>{error}</div>
      ) : (
        <PromptShuffle prompt={prompt} />
      )}

      {/* Shuffle button */}
      <div className={styles.shuffleRow}>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleShuffle}
          disabled={loading || isRunning}
        >
          ↻ New Prompt
        </Button>
      </div>

      {/* Timer */}
      <SpeakingTimer timeLeft={timeLeft} isRunning={isRunning} isComplete={isComplete} />

      {/* Timer controls */}
      <TimerControls
        durations={TIMER_OPTIONS}
        selectedDuration={selectedDuration}
        onSelectDuration={handleSelectDuration}
        isRunning={isRunning}
        isComplete={isComplete}
        onStart={start}
        onPause={pause}
        onReset={handleReset}
      />
    </div>
  )
}
