import { useState } from 'react'
import LandingPage from './pages/landingpage'
import LoginPage from './pages/loginpage'
import ForgotPasswordPage from './pages/forgottenpasword'
import AdminDashboard from './pages/admindashboard'
import SuperAdminDashboard from './pages/superadmindashboard'
import LecturerDashboard from './pages/lecturerdashboard'
import ModulesPage from './pages/modulespage'
import LODetailPage from './pages/LODetailPage'
import AddResultsPage from './pages/AddResultsPage'
import ComparisonPage from './pages/ComparisonPage'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'


function App() {
  const [count, setCount] = useState(0)

  return (
    <Router>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/loginpage" element={<LoginPage />} />
        <Route path="/forgottenpassword" element={<ForgotPasswordPage />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />
        <Route path="/super-admin-dashboard" element={<SuperAdminDashboard />} />
        <Route path="/lecturer-dashboard" element={<LecturerDashboard />} />
        <Route path="/modules" element={<ModulesPage />} />
        <Route path="/lo-detail/:loId" element={<LODetailPage />} />
        <Route path="/lo-detail/:loId/add-results" element={<AddResultsPage />} />
        <Route path="/lo-detail/:loId/comparisons" element={<ComparisonPage />} />
      </Routes>
    </Router>
  )
}

export default App
