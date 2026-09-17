import axiosClient from './axiosClient'

/**
 * Fetch a random prompt.
 * @param {string} mode - e.g. 'OFF_THE_CUFF'
 * @param {string|null} category - e.g. 'Technology'
 * @param {number|null} excludeId - ID to exclude (for shuffle)
 */
export async function getRandomPrompt(mode = 'OFF_THE_CUFF', category = null, excludeId = null) {
  const params = { mode }
  if (category) params.category = category
  if (excludeId) params.excludeId = excludeId

  const response = await axiosClient.get('/prompts/random', { params })
  return response.data
}

/**
 * Fetch all categories.
 */
export async function getCategories() {
  const response = await axiosClient.get('/prompts/categories')
  return response.data
}

/**
 * Fetch a specific prompt by ID.
 */
export async function getPromptById(id) {
  const response = await axiosClient.get(`/prompts/${id}`)
  return response.data
}
