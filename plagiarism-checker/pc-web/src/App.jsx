import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import Layout from './components/Layout.jsx'
import Home from './pages/Home.jsx'
import Register from './pages/Register.jsx'
import Login from './pages/Login.jsx'
import Upload from './pages/Upload.jsx'
import Dashboard from './pages/Dashboard.jsx'
import DiffViewer from './pages/DiffViewer.jsx'
import Humanizer from './pages/Humanizer.jsx'
import AdminPanel from './pages/AdminPanel.jsx'
import CohortView from './pages/CohortView.jsx'

function PrivateRoute({ children, role }) {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated()) return <Navigate to="/login" replace />
  if (role && user?.role !== role) return <Navigate to="/dashboard" replace />
  return children
}

export default function App() {
  const { isAuthenticated } = useAuthStore()
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/register" element={
        isAuthenticated() ? <Navigate to="/dashboard" replace /> : <Register />
      } />
      <Route path="/login" element={
        isAuthenticated() ? <Navigate to="/dashboard" replace /> : <Login />
      } />
      <Route element={<Layout />}>
        <Route path="/dashboard"   element={<PrivateRoute><Dashboard /></PrivateRoute>} />
        <Route path="/upload"      element={<PrivateRoute><Upload /></PrivateRoute>} />
        <Route path="/report/:id"  element={<PrivateRoute><DiffViewer /></PrivateRoute>} />
        <Route path="/humanizer"   element={<PrivateRoute><Humanizer /></PrivateRoute>} />
        <Route path="/cohort/:id"  element={<PrivateRoute><CohortView /></PrivateRoute>} />
        <Route path="/admin"       element={
          <PrivateRoute role="ADMIN"><AdminPanel /></PrivateRoute>
        } />
      </Route>
    </Routes>
  )
}
