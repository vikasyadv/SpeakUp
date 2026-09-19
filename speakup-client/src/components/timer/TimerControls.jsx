import Button from '../common/Button'
import styles from './TimerControls.module.css'

/**
 * Timer duration picker + start/pause/resume/finish controls.
 * Supports speaking states: READY, LISTENING, PAUSED, COMPLETED.
 */
export default function TimerControls({
  durations = [30, 60, 120],
  selectedDuration,
  onSelectDuration,
  speakingState = 'READY',
  onStart,
  onPause,
  onResume,
  onFinish,
  onReset,
}) {
  const isReady = speakingState === 'READY'
  const isListening = speakingState === 'LISTENING'
  const isPaused = speakingState === 'PAUSED'

  return (
    <div className={styles.controls}>
      {/* Duration picker — only show when READY */}
      {isReady && (
        <div className={styles.durations}>
          {durations.map((d) => (
            <button
              key={d}
              className={`${styles.durationBtn} ${
                d === selectedDuration ? styles.durationActive : ''
              }`}
              onClick={() => onSelectDuration(d)}
            >
              {d}s
            </button>
          ))}
        </div>
      )}

      {/* Action buttons */}
      <div className={styles.actions}>
        {isReady && (
          <Button variant="primary" size="lg" onClick={onStart}>
            Start Speaking
          </Button>
        )}

        {isListening && (
          <>
            <Button variant="secondary" size="md" onClick={onPause}>
              Pause
            </Button>
            <Button variant="primary" size="md" onClick={onFinish}>
              Finish Speaking
            </Button>
            <Button variant="ghost" size="sm" onClick={onReset}>
              Reset
            </Button>
          </>
        )}

        {isPaused && (
          <>
            <Button variant="primary" size="md" onClick={onResume}>
              Resume
            </Button>
            <Button variant="secondary" size="md" onClick={onFinish}>
              Finish Speaking
            </Button>
            <Button variant="ghost" size="sm" onClick={onReset}>
              Reset
            </Button>
          </>
        )}
      </div>
    </div>
  )
}
