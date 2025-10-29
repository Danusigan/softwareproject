import { useState } from 'react'
import LandingPage from './pages/landingpage'
import LoginPage from './pages/loginpage'
import ForgotPasswordPage from './pages/forgottenpasword'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'




function App() {
  const [count, setCount] = useState(0)

  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/loginpage" element={<LoginPage />} />
        <Route path="/forgottenpassword" element={<ForgotPasswordPage />} />
      </Routes>
    </Router>
  )
}

export default App
