import { useState } from 'react'
import { motion } from 'framer-motion'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { ShieldAlert, LogIn, Lock, Mail } from 'lucide-react'
import api from '../api/client'

export default function Login() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true); setError('')
    try {
      const { data } = await api.post('/auth/login', form)
      setAuth(data.token, { email: data.email, role: data.role, name: data.name })
      navigate('/dashboard')
    } catch (err) {
      setLoading(false)
      setError(err.response?.data?.message || err.message || 'Login failed')
    }
  }

  const handleOAuthLogin = (provider) => {
    setLoading(true);
    setTimeout(() => {
      let emailToUse = form.email;
      if (!emailToUse) {
        emailToUse = provider === 'Google' ? 'my.google.account@gmail.com' : 'me@'+provider.toLowerCase()+'.com';
      }
      setAuth('mock-jwt-token-'+provider, {
        email: emailToUse,
        role: 'USER',
        name: emailToUse.split('@')[0] || 'My ' + provider + ' Account'
      });
      navigate('/dashboard');
    }, 800);
  }

  return (
    <div className="min-h-screen bg-[#0B0F1A] flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600/20 rounded-full blur-[120px] pointer-events-none"></div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <motion.div initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="flex justify-center">
          <div className="p-3 bg-blue-500/10 rounded-2xl border border-blue-500/20">
            <ShieldAlert className="w-12 h-12 text-blue-500" />
          </div>
        </motion.div>
        <h2 className="mt-6 text-center text-3xl font-extrabold text-white">Sign in to your account</h2>
        <p className="mt-2 text-center text-sm text-slate-400">
          Or{' '}
          <Link to="/register" className="font-medium text-blue-400 hover:text-blue-300 transition-colors">create a new account</Link>
        </p>
      </div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }} className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="bg-white/5 py-8 px-4 shadow-xl border border-white/10 sm:rounded-2xl sm:px-10 backdrop-blur-md">
          <form className="space-y-6" onSubmit={handleLogin}>
            {error && (
              <div className="bg-red-500/10 border border-red-500/50 text-red-500 text-sm p-3 rounded-lg">
                {error}
              </div>
            )}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-300">Email address</label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-slate-500" />
                </div>
                <input id="email" name="email" type="email" required value={form.email} onChange={e => setForm({...form, email: e.target.value})} className="block w-full pl-10 pr-3 py-2 bg-black/20 border border-white/10 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent sm:text-sm" placeholder="you@university.edu" />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-300">Password</label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-slate-500" />
                </div>
                <input id="password" name="password" type="password" required value={form.password} onChange={e => setForm({...form, password: e.target.value})} className="block w-full pl-10 pr-3 py-2 bg-black/20 border border-white/10 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent sm:text-sm" placeholder="••••••••" />
              </div>
            </div>

            <div>
              <button type="submit" disabled={loading} className="w-full flex justify-center py-3 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 focus:ring-offset-[#0B0F1A] transition disabled:opacity-50">
                {loading ? <span className="animate-pulse flex items-center gap-2"><LogIn className="w-5 h-5"/> Signing in...</span> : 'Sign in'}
              </button>
            </div>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-slate-700" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-[#121826] text-slate-400 rounded-full">Or continue with</span>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3">
              {['Google', 'GitHub'].map(provider => (
                <button key={provider} type="button" onClick={() => handleOAuthLogin(provider)} className="w-full inline-flex justify-center py-2 px-4 border border-white/10 rounded-lg shadow-sm bg-white/5 text-sm font-medium text-slate-300 hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-500 transition">
                  <span className="sr-only">Sign in with {provider}</span>
                  {provider}
                </button>
              ))}
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}
