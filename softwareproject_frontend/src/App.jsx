import { useState } from 'react'
import LandingPage from './pages/landingpage'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'




function App() {
  const [count, setCount] = useState(0)

  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
      </Routes>
    </Router>
  )
}

export default App
