import axiosClient from './axiosClient'

/**
 * Generate or retrieve AI feedback for a completed session.
 * @param {number|string} sessionId
 * @returns {Promise<Object>} FeedbackDto
 */
export async function generateFeedback(sessionId) {
  const response = await axiosClient.post(`/sessions/${sessionId}/feedback`)
  return response.data
}

/**
 * Get already-persisted AI feedback for a session.
 * @param {number|string} sessionId
 * @returns {Promise<Object>} FeedbackDto
 */
export async function getFeedback(sessionId) {
  const response = await axiosClient.get(`/sessions/${sessionId}/feedback`)
  return response.data
}
