import { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom'
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
import ProtectedRoute from './components/ProtectedRoute'
import { setupAxiosInterceptors } from './services/axiosSetup'


function AppRoutes() {
  const navigate = useNavigate();

  useEffect(() => {
    // ✅ Setup axios interceptors for token management
    setupAxiosInterceptors(navigate);
  }, [navigate]);

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/loginpage" element={<LoginPage />} />
      <Route path="/forgottenpassword" element={<ForgotPasswordPage />} />

      {/* ✅ Protected Routes - Require login */}
      <Route
        path="/admin-dashboard"
        element={
          <ProtectedRoute requiredRole="admin">
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/super-admin-dashboard"
        element={
          <ProtectedRoute requiredRole="superadmin">
            <SuperAdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lecturer-dashboard"
        element={
          <ProtectedRoute requiredRole="lecture">
            <LecturerDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/modules"
        element={
          <ProtectedRoute>
            <ModulesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lo-detail/:loId"
        element={
          <ProtectedRoute>
            <LODetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lo-detail/:loId/add-results"
        element={
          <ProtectedRoute>
            <AddResultsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lo-detail/:loId/comparisons"
        element={
          <ProtectedRoute>
            <ComparisonPage />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <Router>
      <AppRoutes />
    </Router>
  )
}

export default App
