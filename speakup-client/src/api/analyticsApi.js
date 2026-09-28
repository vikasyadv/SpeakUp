import axiosClient from './axiosClient'

/**
 * Fetch progress and analytics metrics for the current caller.
 * Scoped automatically to JWT User or X-Guest-Id via axiosClient interceptors.
 */
export async function getProgressAnalytics() {
  const response = await axiosClient.get('/analytics/progress')
  return response.data
}
