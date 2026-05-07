import { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import useAuthStore from './store/authStore'
import Layout from './components/common/Layout'
import Login from './pages/Login'
import Register from './pages/Register'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const StudentList = lazy(() => import('./pages/students/StudentList'))
const StudentDetail = lazy(() => import('./pages/students/StudentDetail'))
const GradeManagement = lazy(() => import('./pages/grades/GradeManagement'))
const FeedbackManagement = lazy(() => import('./pages/feedback/FeedbackManagement'))
const CounselingManagement = lazy(() => import('./pages/counseling/CounselingManagement'))
const Reports = lazy(() => import('./pages/reports/Reports'))
const Settings = lazy(() => import('./pages/settings/Settings'))
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'))

function PrivateRoute({ children }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div className="min-h-screen bg-gray-50" />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <Layout />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="students" element={<StudentList />} />
            <Route path="students/:id" element={<StudentDetail />} />
            <Route path="grades" element={<GradeManagement />} />
            <Route path="feedback" element={<FeedbackManagement />} />
            <Route path="counseling" element={<CounselingManagement />} />
            <Route path="reports" element={<Reports />} />
            <Route path="settings" element={<Settings />} />
            <Route path="admin" element={<AdminUsers />} />
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
