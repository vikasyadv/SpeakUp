import { useState, useRef, useEffect, useCallback } from 'react'

/**
 * Hook for browser Speech Recognition API with continuous transcription,
 * pause/resume support, silence auto-recovery, and graceful fallback.
 */
export default function useSpeechRecognition() {
  const [transcript, setTranscript] = useState('')
  const [interimTranscript, setInterimTranscript] = useState('')
  const [isListening, setIsListening] = useState(false)
  const [error, setError] = useState(null)

  const recognitionRef = useRef(null)
  const isListeningRef = useRef(false)
  const transcriptRef = useRef('')
  const isPausedRef = useRef(false)

  const isSupported = typeof window !== 'undefined' &&
    !!(window.SpeechRecognition || window.webkitSpeechRecognition)

  // Keep transcriptRef synced with state
  useEffect(() => {
    transcriptRef.current = transcript
  }, [transcript])

  // Initialize SpeechRecognition instance
  useEffect(() => {
    if (!isSupported) return

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    const recognition = new SpeechRecognition()

    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = 'en-US'

    recognition.onresult = (event) => {
      let currentInterim = ''
      let newFinal = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const item = event.results[i]
        const text = item[0].transcript
        if (item.isFinal) {
          newFinal += (newFinal ? ' ' : '') + text.trim()
        } else {
          currentInterim += text
        }
      }

      if (newFinal) {
        setTranscript((prev) => {
          const updated = prev ? `${prev} ${newFinal}` : newFinal
          transcriptRef.current = updated
          return updated
        })
      }

      setInterimTranscript(currentInterim)
    }

    recognition.onerror = (event) => {
      // 'no-speech' is a normal transient event when the speaker pauses
      if (event.error === 'no-speech') {
        return
      }

      if (event.error === 'not-allowed') {
        setError('Microphone access denied. You can still practice with the timer.')
        isListeningRef.current = false
        setIsListening(false)
      } else {
        console.warn('[SpeechRecognition Error]', event.error)
      }
    }

    recognition.onend = () => {
      setInterimTranscript('')
      // If we should still be listening (e.g. browser automatically timed out after brief silence),
      // and we are not explicitly paused or stopped, restart listening automatically.
      if (isListeningRef.current && !isPausedRef.current) {
        try {
          recognition.start()
        } catch {
          // Ignore error if already restarting or stopped
        }
      }
    }

    recognitionRef.current = recognition

    return () => {
      isListeningRef.current = false
      try {
        recognition.abort()
      } catch {
        // cleanup safe
      }
    }
  }, [isSupported])

  const startListening = useCallback(() => {
    if (!isSupported || !recognitionRef.current) return

    setError(null)
    setInterimTranscript('')
    isListeningRef.current = true
    isPausedRef.current = false
    setIsListening(true)

    try {
      recognitionRef.current.start()
    } catch {
      // Instance might already be starting
    }
  }, [isSupported])

  const pauseListening = useCallback(() => {
    if (!isSupported || !recognitionRef.current) return

    isListeningRef.current = false
    isPausedRef.current = true
    setIsListening(false)
    setInterimTranscript('')

    try {
      recognitionRef.current.stop()
    } catch {
      // Safe stop
    }
  }, [isSupported])

  const resumeListening = useCallback(() => {
    if (!isSupported || !recognitionRef.current) return

    isListeningRef.current = true
    isPausedRef.current = false
    setIsListening(true)

    try {
      recognitionRef.current.start()
    } catch {
      // Safe start
    }
  }, [isSupported])

  const stopListening = useCallback(() => {
    isListeningRef.current = false
    isPausedRef.current = false
    setIsListening(false)
    setInterimTranscript('')

    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop()
      } catch {
        // Safe stop
      }
    }

    return transcriptRef.current
  }, [])

  const resetTranscript = useCallback(() => {
    setTranscript('')
    setInterimTranscript('')
    transcriptRef.current = ''
    setError(null)
  }, [])

  return {
    transcript,
    interimTranscript,
    isListening,
    isSupported,
    error,
    startListening,
    stopListening,
    pauseListening,
    resumeListening,
    resetTranscript,
  }
}
