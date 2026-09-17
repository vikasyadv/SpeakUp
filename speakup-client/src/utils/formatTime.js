/**
 * Formats seconds into MM:SS display.
 * @param {number} totalSeconds
 * @returns {string} formatted time like "1:30"
 */
export function formatTime(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}
