import { Routes, Route } from 'react-router-dom'
import Header from './components/layout/Header'
import PageContainer from './components/layout/PageContainer'
import HomePage from './features/home/HomePage'
import OffTheCuffPage from './features/offTheCuff/OffTheCuffPage'
import SpeakingResultPage from './features/offTheCuff/SpeakingResultPage'
import ResearchPage from './features/research/ResearchPage'
import DebatePage from './features/debate/DebatePage'
import StoryPage from './features/story/StoryPage'
import BookshelfPage from './features/bookshelf/BookshelfPage'
import HistoryPage from './features/history/HistoryPage'

export default function App() {
  return (
    <>
      <Header />
      <PageContainer>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/off-the-cuff" element={<OffTheCuffPage />} />
          <Route path="/off-the-cuff/session/:sessionId" element={<SpeakingResultPage />} />
          <Route path="/research" element={<ResearchPage />} />
          <Route path="/research/session/:sessionId" element={<SpeakingResultPage />} />
          <Route path="/debate" element={<DebatePage />} />
          <Route path="/debate/session/:sessionId" element={<SpeakingResultPage />} />
          <Route path="/story" element={<StoryPage />} />
          <Route path="/story/session/:sessionId" element={<SpeakingResultPage />} />
          <Route path="/bookshelf" element={<BookshelfPage />} />
          <Route path="/history" element={<HistoryPage />} />
        </Routes>
      </PageContainer>
    </>
  )
}
