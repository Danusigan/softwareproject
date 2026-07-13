import { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom'
import LandingPage from './pages/landingpage'
import LoginPage from './pages/loginpage'
import ForgotPasswordPage from './pages/forgottenpasword'
import ResetPasswordPage from './pages/resetpassword'
import AdminDashboard from './pages/admindashboard'
import SuperAdminDashboard from './pages/superadmindashboard'
import LecturerDashboard from './pages/lecturerdashboard'
import ModulesPage from './pages/modulespage'
import ComparisonPage from './pages/ComparisonPage'
import ProgramOutcomesPage from './pages/ProgramOutcomesPage'
import CreateLOWithMappingPage from './pages/CreateLOWithMappingPage'
import LOPOMappingManagementPage from './pages/LOPOMappingManagementPage'
import MarksWorkbenchPage from './pages/MarksWorkbenchPage'
import CqiReviewPage from './pages/CqiReviewPage'
import MyCqiPlansPage from './pages/MyCqiPlansPage'
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
        path="/lo-detail/:loId/comparisons"
        element={
          <ProtectedRoute>
            <ComparisonPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/marks-workbench/:moduleId"
        element={
          <ProtectedRoute>
            <MarksWorkbenchPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/program-outcomes"
        element={
          <ProtectedRoute requiredRole="admin">
            <ProgramOutcomesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/create-lo-mapping/:moduleId"
        element={
          <ProtectedRoute>
            <CreateLOWithMappingPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lo-po-mappings"
        element={
          <ProtectedRoute>
            <LOPOMappingManagementPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/cqi-review"
        element={
          <ProtectedRoute requiredRole="admin">
            <CqiReviewPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-cqi-plans"
        element={
          <ProtectedRoute requiredRole="lecture">
            <MyCqiPlansPage />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <Router>
<<<<<<< Updated upstream
      <AppRoutes />
=======
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/loginpage" element={<LoginPage />} />
        <Route path="/forgottenpassword" element={<ForgotPasswordPage />} />
        <Route path="/resetpassword" element={<ResetPasswordPage />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />
        <Route path="/super-admin-dashboard" element={<SuperAdminDashboard />} />
        <Route path="/lecturer-dashboard" element={<LecturerDashboard />} />
        <Route path="/modules" element={<ModulesPage />} />
        <Route path="/lo-detail/:loId" element={<LODetailPage />} />
        <Route path="/lo-detail/:loId/add-results" element={<AddResultsPage />} />
        <Route path="/lo-detail/:loId/comparisons" element={<ComparisonPage />} />
      </Routes>
>>>>>>> Stashed changes
    </Router>
  )
}

export default App
