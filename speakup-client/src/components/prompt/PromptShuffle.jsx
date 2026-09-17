import { useState, useEffect, useRef } from 'react'
import PromptCard from './PromptCard'
import styles from './PromptShuffle.module.css'

/**
 * Wraps PromptCard with a physical card-deck shuffle animation.
 * When `prompt` changes, the old card exits left while the new card enters from right.
 */
export default function PromptShuffle({ prompt }) {
  const [displayedPrompt, setDisplayedPrompt] = useState(prompt)
  const [animation, setAnimation] = useState('')
  const isFirstRender = useRef(true)

  useEffect(() => {
    // Don't animate on first render
    if (isFirstRender.current) {
      isFirstRender.current = false
      setDisplayedPrompt(prompt)
      return
    }

    if (!prompt) return

    // Start exit animation
    setAnimation(styles.exit)

    const exitTimer = setTimeout(() => {
      // Swap to new prompt and start enter animation
      setDisplayedPrompt(prompt)
      setAnimation(styles.enter)

      const enterTimer = setTimeout(() => {
        setAnimation('')
      }, 400)

      return () => clearTimeout(enterTimer)
    }, 300)

    return () => clearTimeout(exitTimer)
  }, [prompt])

  return (
    <div className={styles.container}>
      {/* Decorative "deck" cards behind the main card */}
      <div className={styles.deckCard} style={{ transform: 'rotate(-2deg) translateY(4px)', opacity: 0.15 }} />
      <div className={styles.deckCard} style={{ transform: 'rotate(1deg) translateY(2px)', opacity: 0.25 }} />

      {/* The main animated card */}
      <div className={`${styles.cardWrapper} ${animation}`}>
        <PromptCard prompt={displayedPrompt} />
      </div>
    </div>
  )
}
