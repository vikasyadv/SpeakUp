import styles from './CategoryBadge.module.css'

export default function CategoryBadge({ category, active = false, onClick }) {
  const classes = [
    styles.badge,
    active ? styles.active : '',
    onClick ? styles.clickable : '',
  ].join(' ').trim()

  const handleClick = () => {
    if (onClick) onClick(category)
  }

  return (
    <span
      className={classes}
      onClick={handleClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {category}
    </span>
  )
}
