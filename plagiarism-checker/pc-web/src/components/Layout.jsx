import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuthStore } from '../store/authStore'
import { ShieldAlert, LayoutDashboard, UploadCloud, FileText, Bot, PenTool, LogOut, Settings } from 'lucide-react'

const navItems = [
  { to: '/dashboard',  icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
  { to: '/upload',     icon: <UploadCloud size={20} />, label: 'Plagiarism Scan' },
  { to: '/humanizer',  icon: <PenTool size={20} />, label: 'AI Tools & Humanizer' },
]

export default function Layout() {
  const { user, clearAuth, isAdmin, isInstructor } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => { clearAuth(); navigate('/login') }

  return (
    <div className="flex h-screen bg-[#0B0F1A] text-slate-200 overflow-hidden relative">
      {/* Background glow effects */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-blue-600/10 rounded-full blur-[150px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] left-[-10%] w-[400px] h-[400px] bg-purple-600/10 rounded-full blur-[150px] pointer-events-none"></div>

      {/* Sidebar */}
      <aside className="w-72 flex-shrink-0 flex flex-col border-r border-white/10 bg-[#0B0F1A]/80 backdrop-blur-xl relative z-10">
        <div className="px-6 py-8 border-b border-white/10">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-500/10 rounded-lg border border-blue-500/20">
              <ShieldAlert className="w-8 h-8 text-blue-500" />
            </div>
            <div>
              <p className="text-xl font-bold tracking-tight text-white leading-tight">AuthentiText</p>
              <p className="text-sm text-blue-400 font-semibold tracking-wider">AI PLATFORM</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 px-2">Menu</div>
          {navItems.map(item => (
            <NavLink key={item.to} to={item.to}
                     className={({ isActive }) => `flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 font-medium ${isActive ? 'bg-blue-600 shadow-lg shadow-blue-600/25 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'}`}>
              {item.icon}
              {item.label}
            </NavLink>
          ))}
          {isInstructor() && (
            <NavLink to="/cohort/default"
                     className={({ isActive }) => `flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 font-medium ${isActive ? 'bg-blue-600 shadow-lg shadow-blue-600/25 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'}`}>
              <FileText size={20} /> Cohort Heatmap
            </NavLink>
          )}

          {isAdmin() && (
            <>
              <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mt-8 mb-4 px-2">Admin</div>
              <NavLink to="/admin"
                     className={({ isActive }) => `flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 font-medium ${isActive ? 'bg-purple-600 shadow-lg shadow-purple-600/25 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'}`}>
                <Settings size={20} /> Admin Panel
              </NavLink>
            </>
          )}
        </nav>

        <div className="p-4 border-t border-white/10 bg-white/5 backdrop-blur-md shrink-0">
          <div className="flex items-center gap-3 px-2 mb-4">
            <div className="w-10 h-10 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center text-white font-bold shadow-lg">
              {user?.name?.[0]?.toUpperCase() || user?.email?.[0]?.toUpperCase() || 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-white truncate">{user?.name || user?.email || 'User'}</p>
              <p className="text-xs text-slate-400 truncate capitalize">{user?.email}</p>
            </div>
          </div>
          <button onClick={handleLogout} className="w-full flex items-center justify-center gap-2 px-4 py-2 hover:bg-red-500/10 hover:text-red-400 rounded-lg text-slate-400 transition-colors text-sm font-medium border border-transparent hover:border-red-500/20">
            <LogOut size={16} /> Logout
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto relative z-0">
        <div className="w-full max-w-7xl mx-auto px-8 py-8 animate-fade-in relative z-10">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
