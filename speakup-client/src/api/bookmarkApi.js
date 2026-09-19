import axiosClient from './axiosClient'

/**
 * Bookmark a prompt.
 */
export async function bookmarkPrompt(promptId) {
  const response = await axiosClient.post(`/bookmarks/${promptId}`)
  return response.data
}

/**
 * Remove a bookmark by prompt ID.
 */
export async function removeBookmark(promptId) {
  await axiosClient.delete(`/bookmarks/${promptId}`)
}

/**
 * Check if a prompt is bookmarked.
 */
export async function isBookmarked(promptId) {
  const response = await axiosClient.get(`/bookmarks/check/${promptId}`)
  return response.data.bookmarked
}

/**
 * Get all bookmarks.
 */
export async function getAllBookmarks() {
  const response = await axiosClient.get('/bookmarks')
  return response.data
}
