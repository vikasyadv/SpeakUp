import { Routes, Route } from 'react-router-dom'
import Header from './components/layout/Header'
import PageContainer from './components/layout/PageContainer'
import HomePage from './features/home/HomePage'
import OffTheCuffPage from './features/offTheCuff/OffTheCuffPage'
import ResearchPage from './features/research/ResearchPage'
import DebatePage from './features/debate/DebatePage'
import StoryPage from './features/story/StoryPage'

export default function App() {
  return (
    <>
      <Header />
      <PageContainer>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/off-the-cuff" element={<OffTheCuffPage />} />
          <Route path="/research" element={<ResearchPage />} />
          <Route path="/debate" element={<DebatePage />} />
          <Route path="/story" element={<StoryPage />} />
        </Routes>
      </PageContainer>
    </>
  )
}
