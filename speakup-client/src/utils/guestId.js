const GUEST_ID_KEY = 'speakup_guest_id'

/**
 * Returns the persistent guest ID from localStorage.
 * If none exists, generates a new UUID, persists it, and returns it.
 */
export function getGuestId() {
  try {
    let guestId = localStorage.getItem(GUEST_ID_KEY)
    if (!guestId) {
      guestId = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : 'guest-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now().toString(36)
      localStorage.setItem(GUEST_ID_KEY, guestId)
    }
    return guestId
  } catch (err) {
    console.warn('Unable to access localStorage for guest ID:', err)
    return null
  }
}
