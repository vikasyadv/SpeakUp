import axios from 'axios'
import { getGuestId } from '../utils/guestId.js'

export const AUTH_TOKEN_KEY = 'speakup_auth_token'

const axiosClient = axios.create({
  baseURL: import.meta.env?.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: attach Bearer token if authenticated, or X-Guest-Id if guest
axiosClient.interceptors.request.use((config) => {
  const isAuthRequest = config.url?.includes('/auth/register') || config.url?.includes('/auth/login')
  const token = localStorage.getItem(AUTH_TOKEN_KEY)

  if (token && !isAuthRequest) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    // Prevent attaching stale/invalid Authorization header to register/login requests
    delete config.headers.Authorization
  }

  // Ensure X-Guest-Id is attached for guests and register/login calls
  const guestId = getGuestId()
  if (guestId && (!token || isAuthRequest)) {
    if (!config.headers['X-Guest-Id']) {
      config.headers['X-Guest-Id'] = guestId
    }
  }

  return config
})

// Response interceptor: handle 401 safely without redirect loops
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear token so subsequent requests revert seamlessly to guest mode
      try {
        localStorage.removeItem(AUTH_TOKEN_KEY)
      } catch {
        // Ignore storage errors
      }
    }
    const message = error.response?.data?.message || error.message || 'Something went wrong'
    console.error('[API Error]', message)
    return Promise.reject(error)
  }
)

export default axiosClient
