import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Button from '../components/common/Button'
import styles from './ProfilePage.module.css'

export default function ProfilePage() {
  const navigate = useNavigate()
  const { user, updateProfile } = useAuth()

  const [displayName, setDisplayName] = useState(user?.displayName || '')
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSuccessMessage(null)

    const trimmed = displayName.trim()
    if (!trimmed) {
      setError('Display name is required.')
      return
    }
    if (trimmed.length > 50) {
      setError('Display name cannot exceed 50 characters.')
      return
    }

    setIsSubmitting(true)
    try {
      await updateProfile({ displayName: trimmed })
      setDisplayName(trimmed)
      setSuccessMessage('Profile display name updated successfully.')
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to update profile.'
      setError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>Edit Profile</h1>
          <p className={styles.subtitle}>
            Update your display name and view your account details.
          </p>
        </div>

        {successMessage && (
          <div className={styles.successBanner} role="status">
            <span>✓</span>
            <span>{successMessage}</span>
          </div>
        )}

        {error && (
          <div className={styles.errorBanner} role="alert">
            <span>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className={styles.form} noValidate>
          <div className={styles.field}>
            <label htmlFor="profile-displayName" className={styles.label}>
              Display Name
            </label>
            <input
              id="profile-displayName"
              type="text"
              value={displayName}
              onChange={(e) => {
                setDisplayName(e.target.value)
                if (error) setError(null)
                if (successMessage) setSuccessMessage(null)
              }}
              placeholder="e.g. Vikas Yadav"
              className={`${styles.input} ${error ? styles.inputError : ''}`}
              maxLength={50}
              disabled={isSubmitting}
              autoComplete="name"
            />
            <span className={styles.hint}>
              This is the name shown in the top navigation bar. Max 50 characters.
            </span>
          </div>

          <div className={styles.field}>
            <label htmlFor="profile-email" className={styles.label}>
              Email Address
            </label>
            <input
              id="profile-email"
              type="email"
              value={user?.email || ''}
              readOnly
              className={styles.readOnlyInput}
              tabIndex={-1}
            />
            <span className={styles.hint}>
              Account email is read-only and cannot be changed.
            </span>
          </div>

          <div className={styles.actions}>
            <Button
              type="submit"
              variant="primary"
              size="md"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </Button>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className={styles.cancelBtn}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
