import Button from '../common/Button'
import styles from './TimerControls.module.css'

/**
 * Timer duration picker + start/pause/reset controls.
 */
export default function TimerControls({
  durations = [30, 60, 120],
  selectedDuration,
  onSelectDuration,
  isRunning,
  isComplete,
  onStart,
  onPause,
  onReset,
}) {
  return (
    <div className={styles.controls}>
      {/* Duration picker — only show when timer hasn't started */}
      {!isRunning && !isComplete && (
        <div className={styles.durations}>
          {durations.map((d) => (
            <button
              key={d}
              className={`${styles.durationBtn} ${d === selectedDuration ? styles.durationActive : ''}`}
              onClick={() => onSelectDuration(d)}
            >
              {d}s
            </button>
          ))}
        </div>
      )}

      {/* Action buttons */}
      <div className={styles.actions}>
        {!isRunning && !isComplete && (
          <Button variant="primary" size="lg" onClick={onStart}>
            Start Speaking
          </Button>
        )}

        {isRunning && (
          <Button variant="secondary" size="md" onClick={onPause}>
            Pause
          </Button>
        )}

        {(isRunning || isComplete) && (
          <Button variant="ghost" size="md" onClick={onReset}>
            Reset
          </Button>
        )}
      </div>
    </div>
  )
}
