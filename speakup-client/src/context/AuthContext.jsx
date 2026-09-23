import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import * as authApi from '../api/authApi.js'
import { AUTH_TOKEN_KEY } from '../api/axiosClient.js'
import { getGuestId } from '../utils/guestId.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => {
    try {
      return localStorage.getItem(AUTH_TOKEN_KEY) || null
    } catch {
      return null
    }
  })
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  // Verify token and restore user on startup
  useEffect(() => {
    let isMounted = true

    async function initAuth() {
      // Ensure guest identity exists immediately
      getGuestId()

      const storedToken = localStorage.getItem(AUTH_TOKEN_KEY)
      if (!storedToken) {
        if (isMounted) {
          setToken(null)
          setUser(null)
          setIsLoading(false)
        }
        return
      }

      try {
        const userData = await authApi.getCurrentUser()
        if (isMounted) {
          setUser(userData)
          setToken(storedToken)
        }
      } catch (err) {
        console.warn('Authentication token verification failed, falling back to guest mode:', err.message)
        try {
          localStorage.removeItem(AUTH_TOKEN_KEY)
        } catch {
          // Ignore
        }
        if (isMounted) {
          setToken(null)
          setUser(null)
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    initAuth()

    return () => {
      isMounted = false
    }
  }, [])

  const login = useCallback(async (credentials) => {
    const guestId = getGuestId()
    const data = await authApi.login(credentials, guestId)
    try {
      localStorage.setItem(AUTH_TOKEN_KEY, data.token)
    } catch (err) {
      console.warn('Could not persist auth token in localStorage:', err)
    }
    setToken(data.token)
    setUser(data.user)
    return data
  }, [])

  const register = useCallback(async (credentials) => {
    const guestId = getGuestId()
    const data = await authApi.register(credentials, guestId)
    try {
      localStorage.setItem(AUTH_TOKEN_KEY, data.token)
    } catch (err) {
      console.warn('Could not persist auth token in localStorage:', err)
    }
    setToken(data.token)
    setUser(data.user)
    return data
  }, [])

  const logout = useCallback(() => {
    try {
      localStorage.removeItem(AUTH_TOKEN_KEY)
    } catch {
      // Ignore
    }
    setToken(null)
    setUser(null)
  }, [])

  const updateProfile = useCallback(async (profileData) => {
    const updatedUser = await authApi.updateProfile(profileData)
    setUser(updatedUser)
    return updatedUser
  }, [])

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    register,
    logout,
    updateProfile,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
