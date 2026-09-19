import { useEffect, useRef } from 'react'
import styles from './LiveTranscript.module.css'

/**
 * Live transcript view displaying streaming recognized speech,
 * listening pulse indicator, and graceful status states.
 */
export default function LiveTranscript({
  transcript,
  interimTranscript,
  isListening,
  isPaused,
  isSupported,
  error,
}) {
  const containerRef = useRef(null)

  // Auto-scroll to bottom as new speech arrives
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [transcript, interimTranscript])

  const hasText = Boolean(transcript || interimTranscript)

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.statusGroup}>
          <span
            className={`${styles.indicator} ${
              isListening ? styles.listening : isPaused ? styles.paused : ''
            }`}
          />
          <span className={styles.statusLabel}>
            {isListening
              ? 'Listening…'
              : isPaused
              ? 'Paused'
              : 'Live Transcript'}
          </span>
        </div>
        {!isSupported && (
          <span className={styles.unsupportedBadge}>
            Mic not supported in this browser
          </span>
        )}
      </div>

      <div ref={containerRef} className={styles.content}>
        {error ? (
          <p className={styles.errorMessage}>{error}</p>
        ) : hasText ? (
          <p className={styles.speechText}>
            {transcript && <span className={styles.final}>{transcript} </span>}
            {interimTranscript && (
              <span className={styles.interim}>{interimTranscript}</span>
            )}
          </p>
        ) : (
          <p className={styles.placeholder}>
            {isListening
              ? 'Speak clearly into your microphone…'
              : isPaused
              ? 'Session paused.'
              : isSupported
              ? 'Words will appear here as you speak.'
              : 'Speech recognition is not supported in this browser, but your timer session is fully functional.'}
          </p>
        )}
      </div>
    </div>
  )
}
