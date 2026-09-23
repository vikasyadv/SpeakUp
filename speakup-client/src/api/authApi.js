import axiosClient from './axiosClient.js'

/**
 * Register a new account.
 * Passes X-Guest-Id when present so unauthenticated guest sessions and bookmarks are claimed.
 *
 * @param {{ email: string, password: string, displayName?: string }} data
 * @param {string} [guestId]
 * @returns {Promise<{ token: string, tokenType: string, user: object }>}
 */
export async function register(data, guestId) {
  const headers = {}
  if (guestId) {
    headers['X-Guest-Id'] = guestId
  }

  const response = await axiosClient.post('/auth/register', data, { headers })
  return response.data
}

/**
 * Log in to an existing account.
 * Passes X-Guest-Id when present so unauthenticated guest sessions and bookmarks are claimed.
 *
 * @param {{ email: string, password: string }} data
 * @param {string} [guestId]
 * @returns {Promise<{ token: string, tokenType: string, user: object }>}
 */
export async function login(data, guestId) {
  const headers = {}
  if (guestId) {
    headers['X-Guest-Id'] = guestId
  }

  const response = await axiosClient.post('/auth/login', data, { headers })
  return response.data
}

/**
 * Get current authenticated user profile.
 * Bearer token is automatically attached by axiosClient interceptor.
 *
 * @returns {Promise<{ id: number, email: string, displayName: string, role: string, createdAt: string }>}
 */
export async function getCurrentUser() {
  const response = await axiosClient.get('/auth/me')
  return response.data
}

/**
 * Update authenticated user profile.
 * Bearer token is automatically attached by axiosClient interceptor.
 *
 * @param {{ displayName: string }} data
 * @returns {Promise<{ id: number, email: string, displayName: string, role: string, createdAt: string }>}
 */
export async function updateProfile(data) {
  const response = await axiosClient.patch('/auth/me', data)
  return response.data
}
