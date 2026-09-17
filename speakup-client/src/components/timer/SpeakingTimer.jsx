import { formatTime } from '../../utils/formatTime'
import styles from './SpeakingTimer.module.css'

/**
 * Countdown timer display with visual urgency in the final 10 seconds.
 */
export default function SpeakingTimer({ timeLeft, isRunning, isComplete }) {
  const isUrgent = isRunning && timeLeft <= 10 && timeLeft > 0

  const classes = [
    styles.timer,
    isRunning ? styles.running : '',
    isUrgent ? styles.urgent : '',
    isComplete ? styles.complete : '',
  ].join(' ').trim()

  return (
    <div className={classes}>
      <span className={styles.time}>{formatTime(timeLeft)}</span>
      {isComplete && <span className={styles.label}>Time's up!</span>}
    </div>
  )
}
