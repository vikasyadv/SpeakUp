import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Button from '../components/common/Button'
import styles from './LoginPage.module.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const validate = () => {
    const errs = {}
    if (!email.trim()) {
      errs.email = 'Email is required'
    } else if (!/\S+@\S+\.\S+/.test(email.trim())) {
      errs.email = 'Enter a valid email address'
    }

    if (!password) {
      errs.password = 'Password is required'
    }

    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setApiError(null)

    if (!validate()) {
      return
    }

    setIsSubmitting(true)
    try {
      await login({
        email: email.trim(),
        password,
      })
      const returnTo = location.state?.from || '/'
      navigate(returnTo, { replace: true })
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Invalid email or password'
      setApiError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>Welcome back.</h1>
          <p className={styles.subtitle}>
            Sign in to keep your speaking history and bookmarks with you.
          </p>
        </div>

        {apiError && (
          <div className={styles.errorBanner} role="alert">
            <span>⚠️</span>
            <span>{apiError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className={styles.form} noValidate>
          <div className={styles.field}>
            <label htmlFor="login-email" className={styles.label}>
              Email
            </label>
            <input
              id="login-email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                if (errors.email) setErrors((prev) => ({ ...prev, email: null }))
              }}
              placeholder="you@example.com"
              className={`${styles.input} ${errors.email ? styles.inputError : ''}`}
              autoComplete="email"
              disabled={isSubmitting}
            />
            {errors.email && <span className={styles.fieldError}>{errors.email}</span>}
          </div>

          <div className={styles.field}>
            <label htmlFor="login-password" className={styles.label}>
              Password
            </label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                if (errors.password) setErrors((prev) => ({ ...prev, password: null }))
              }}
              placeholder="••••••••"
              className={`${styles.input} ${errors.password ? styles.inputError : ''}`}
              autoComplete="current-password"
              disabled={isSubmitting}
            />
            {errors.password && <span className={styles.fieldError}>{errors.password}</span>}
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isSubmitting}
            className={styles.submitBtn}
          >
            {isSubmitting ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>

        <div className={styles.footer}>
          <p className={styles.switchText}>
            Don't have an account?{' '}
            <Link to="/register" state={location.state} className={styles.switchLink}>
              Create one free
            </Link>
          </p>

          <p className={styles.guestNote}>
            Practice is always free and open without an account.
          </p>

          <Link to="/" className={styles.backLink}>
            ← Continue practicing as guest
          </Link>
        </div>
      </div>
    </div>
  )
}
