import axiosClient from './axiosClient'

/**
 * Create a new speaking session.
 */
export async function createSession({ promptText, promptId, mode, durationSeconds }) {
  const response = await axiosClient.post('/sessions', {
    promptText,
    promptId,
    mode,
    durationSeconds,
  })
  return response.data
}

/**
 * Mark a session as completed.
 */
export async function completeSession(sessionId) {
  const response = await axiosClient.patch(`/sessions/${sessionId}/complete`)
  return response.data
}

/**
 * Mark a session as abandoned.
 */
export async function abandonSession(sessionId) {
  const response = await axiosClient.patch(`/sessions/${sessionId}/abandon`)
  return response.data
}

/**
 * Get a session by ID.
 */
export async function getSession(sessionId) {
  const response = await axiosClient.get(`/sessions/${sessionId}`)
  return response.data
}

/**
 * Get recent session history.
 */
export async function getRecentSessions() {
  const response = await axiosClient.get('/sessions/recent')
  return response.data
}

/**
 * Delete a session.
 */
export async function deleteSession(sessionId) {
  await axiosClient.delete(`/sessions/${sessionId}`)
}
