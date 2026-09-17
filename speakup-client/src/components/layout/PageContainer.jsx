import styles from './PageContainer.module.css'

export default function PageContainer({ children }) {
  return (
    <main className={styles.container}>
      {children}
    </main>
  )
}
