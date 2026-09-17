import { useState, useRef, useCallback, useEffect } from 'react'

/**
 * Custom hook for a countdown timer.
 *
 * @param {number} initialSeconds - Starting time in seconds
 * @param {function} onComplete - Called when timer reaches 0
 * @returns {{ timeLeft, isRunning, isComplete, start, pause, reset }}
 */
export default function useTimer(initialSeconds, onComplete) {
  const [timeLeft, setTimeLeft] = useState(initialSeconds)
  const [isRunning, setIsRunning] = useState(false)
  const [isComplete, setIsComplete] = useState(false)
  const intervalRef = useRef(null)
  const onCompleteRef = useRef(onComplete)

  // Keep the callback ref updated
  useEffect(() => {
    onCompleteRef.current = onComplete
  }, [onComplete])

  // Clear interval on unmount
  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [])

  const start = useCallback(() => {
    if (isComplete) return
    setIsRunning(true)

    intervalRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current)
          intervalRef.current = null
          setIsRunning(false)
          setIsComplete(true)
          onCompleteRef.current?.()
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }, [isComplete])

  const pause = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
    setIsRunning(false)
  }, [])

  const reset = useCallback((newSeconds) => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
    setTimeLeft(newSeconds ?? initialSeconds)
    setIsRunning(false)
    setIsComplete(false)
  }, [initialSeconds])

  return { timeLeft, isRunning, isComplete, start, pause, reset }
}
