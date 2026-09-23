import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Button from '../components/common/Button'
import styles from './RegisterPage.module.css'

export default function RegisterPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { register } = useAuth()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
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
    } else if (password.length < 8) {
      errs.password = 'Password must be at least 8 characters'
    }

    if (!confirmPassword) {
      errs.confirmPassword = 'Confirm your password'
    } else if (password !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match'
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
      await register({
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
      })
      const returnTo = location.state?.from || '/'
      navigate(returnTo, { replace: true })
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Registration failed'
      setApiError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>Keep your progress.</h1>
          <p className={styles.subtitle}>
            Create a free account to keep your speaking history and bookmarks across devices.
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
            <label htmlFor="reg-displayName" className={styles.label}>
              <span>Display Name</span>
              <span className={styles.optionalTag}>optional</span>
            </label>
            <input
              id="reg-displayName"
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="e.g. Alex"
              className={styles.input}
              autoComplete="name"
              disabled={isSubmitting}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="reg-email" className={styles.label}>
              Email
            </label>
            <input
              id="reg-email"
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
            <label htmlFor="reg-password" className={styles.label}>
              Password
            </label>
            <input
              id="reg-password"
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                if (errors.password) setErrors((prev) => ({ ...prev, password: null }))
              }}
              placeholder="At least 8 characters"
              className={`${styles.input} ${errors.password ? styles.inputError : ''}`}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
            {errors.password ? (
              <span className={styles.fieldError}>{errors.password}</span>
            ) : (
              <span className={styles.hint}>Minimum 8 characters</span>
            )}
          </div>

          <div className={styles.field}>
            <label htmlFor="reg-confirmPassword" className={styles.label}>
              Confirm Password
            </label>
            <input
              id="reg-confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value)
                if (errors.confirmPassword) setErrors((prev) => ({ ...prev, confirmPassword: null }))
              }}
              placeholder="Repeat your password"
              className={`${styles.input} ${errors.confirmPassword ? styles.inputError : ''}`}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
            {errors.confirmPassword && (
              <span className={styles.fieldError}>{errors.confirmPassword}</span>
            )}
          </div>

          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isSubmitting}
            className={styles.submitBtn}
          >
            {isSubmitting ? 'Creating account...' : 'Create Account'}
          </Button>
        </form>

        <div className={styles.footer}>
          <p className={styles.switchText}>
            Already have an account?{' '}
            <Link to="/login" state={location.state} className={styles.switchLink}>
              Sign in
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
