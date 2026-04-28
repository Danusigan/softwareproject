import { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import { setupAxiosInterceptors } from './services/axiosSetup'
import authService from './services/authService'

import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import SuperAdminDashboard from './pages/SuperAdminDashboard'
import AdminDashboard from './pages/AdminDashboard'
import LecturerDashboard from './pages/LecturerDashboard'
import ModulesPage from './pages/ModulesPage'
import LODetailPage from './pages/LODetailPage'
import ProgramOutcomesPage from './pages/ProgramOutcomesPage'
import LOPOMappingPage from './pages/LOPOMappingPage'

function ProtectedRoute({ children, requiredRole }) {
  if (!authService.isLoggedIn()) return <Navigate to="/login" replace />
  if (requiredRole) {
    const { userType } = authService.getUserInfo() || {}
    const role = (userType || '').toLowerCase().trim()
    if (requiredRole === 'superadmin' && role !== 'superadmin') return <Navigate to="/" replace />
    if (requiredRole === 'admin' && role !== 'admin' && role !== 'superadmin') return <Navigate to="/" replace />
    if (requiredRole === 'lecture' && role !== 'lecture' && role !== 'admin' && role !== 'superadmin') return <Navigate to="/" replace />
  }
  return children
}

function AppRoutes() {
  const navigate = useNavigate()
  useEffect(() => { setupAxiosInterceptors(navigate) }, [navigate])

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/super-admin" element={<ProtectedRoute requiredRole="superadmin"><SuperAdminDashboard /></ProtectedRoute>} />
      <Route path="/admin" element={<ProtectedRoute requiredRole="admin"><AdminDashboard /></ProtectedRoute>} />
      <Route path="/lecturer" element={<ProtectedRoute requiredRole="lecture"><LecturerDashboard /></ProtectedRoute>} />
      <Route path="/modules" element={<ProtectedRoute><ModulesPage /></ProtectedRoute>} />
      <Route path="/lo/:loId" element={<ProtectedRoute><LODetailPage /></ProtectedRoute>} />
      <Route path="/program-outcomes" element={<ProtectedRoute requiredRole="admin"><ProgramOutcomesPage /></ProtectedRoute>} />
      <Route path="/lo-po-mappings" element={<ProtectedRoute><LOPOMappingPage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <Router>
      <AppRoutes />
    </Router>
  )
}
