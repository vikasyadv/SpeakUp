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
 * @param {number|string} sessionId
 * @param {{ transcript?: string, actualDurationSeconds?: number }} [payload]
 */
export async function completeSession(sessionId, payload) {
  const response = await axiosClient.patch(`/sessions/${sessionId}/complete`, payload)
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

/**
 * Get paginated sessions with optional filters.
 * @param {{ mode?: string, status?: string, search?: string, page?: number, size?: number }} [params]
 * @returns {Promise<{ content: SessionDto[], totalElements: number, totalPages: number, number: number, size: number, empty: boolean }>}
 */
export async function getSessions({ mode, status, search, page = 0, size = 20 } = {}) {
  const params = {}
  if (mode && mode !== 'ALL') params.mode = mode
  if (status && status !== 'ALL') params.status = status
  if (search && search.trim()) params.search = search.trim()
  params.page = page
  params.size = size
  const response = await axiosClient.get('/sessions', { params })
  return response.data
}
