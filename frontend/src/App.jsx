import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import useAuthStore from './store/authStore'
import Layout from './components/common/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import StudentList from './pages/students/StudentList'
import StudentDetail from './pages/students/StudentDetail'
import GradeManagement from './pages/grades/GradeManagement'
import FeedbackManagement from './pages/feedback/FeedbackManagement'
import CounselingManagement from './pages/counseling/CounselingManagement'
import Reports from './pages/reports/Reports'

function PrivateRoute({ children }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
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
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
