import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { ShieldAlert, Bot, PenTool, CheckCircle, ArrowRight } from 'lucide-react'

export default function Home() {
  const containerVariants = {
    hidden: { opacity: 0},
    visible: { 
      opacity: 1,
      transition: { staggerChildren: 0.2 }
    }
  }

  const itemVariants = {
    hidden: { y: 20, opacity: 0 },
    visible: { y: 0, opacity: 1, transition: { duration: 0.5 } }
  }

  return (
    <div className="min-h-screen bg-[#0B0F1A] text-slate-200 overflow-hidden relative">
      {/* Background Glow */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-600/20 rounded-full blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600/20 rounded-full blur-[120px] pointer-events-none"></div>

      <nav className="relative z-10 flex items-center justify-between px-8 py-6 max-w-7xl mx-auto">
        <div className="flex items-center gap-2">
          <ShieldAlert className="w-8 h-8 text-blue-500" />
          <span className="text-xl font-bold tracking-tight text-white">AuthentiText <span className="text-blue-500">AI</span></span>
        </div>
        <div className="flex gap-4">
          <Link to="/login" className="px-5 py-2 font-medium text-slate-300 hover:text-white transition">Sign In</Link>
          <Link to="/register" className="px-5 py-2 font-medium text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-500/25 transition">Get Started</Link>
        </div>
      </nav>

      <main className="relative z-10 max-w-7xl mx-auto px-8 pt-20 pb-32 flex flex-col items-center text-center">
        <motion.div initial="hidden" animate="visible" variants={containerVariants} className="max-w-4xl">
          <motion.h1 variants={itemVariants} className="text-5xl md:text-7xl font-extrabold text-white leading-tight mb-6">
            Advanced AI-Powered <br/> <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-500">Content Authenticity</span>
          </motion.h1>
          <motion.p variants={itemVariants} className="text-lg md:text-xl text-slate-400 mb-10 max-w-2xl mx-auto">
            Detect plagiarism with semantic search, identify AI-generated content, and leverage smart rewriting tools in one academic integrity platform.
          </motion.p>
          <motion.div variants={itemVariants} className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link to="/register" className="flex items-center gap-2 px-8 py-4 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-semibold shadow-lg shadow-blue-500/30 transition-all text-lg w-full sm:w-auto justify-center">
              Try Demo <ArrowRight className="w-5 h-5" />
            </Link>
            <a href="#features" className="px-8 py-4 bg-white/5 border border-white/10 hover:bg-white/10 text-white rounded-xl font-semibold transition-all text-lg w-full sm:w-auto justify-center flex">
              Explore Features
            </a>
          </motion.div>
        </motion.div>

        {/* Features Section */}
        <motion.div 
          id="features"
          initial="hidden" whileInView="visible" viewport={{ once: true, margin: "-100px" }}
          variants={containerVariants}
          className="mt-32 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 w-full"
        >
          <FeatureCard 
            icon={<ShieldAlert className="w-8 h-8 text-blue-400" />}
            title="Plagiarism Detection"
            desc="TF-IDF & N-Gram analysis against billions of sources to find exact and paraphrased matches."
          />
          <FeatureCard 
            icon={<Bot className="w-8 h-8 text-purple-400" />}
            title="AI Content Detection"
            desc="Identify LLM-generated text using burstiness, perplexity, and AI classification models."
          />
          <FeatureCard 
            icon={<PenTool className="w-8 h-8 text-emerald-400" />}
            title="Smart Rewriting"
            desc="Humanize and rewrite detected content specifically to maintain academic integrity."
          />
          <FeatureCard 
            icon={<CheckCircle className="w-8 h-8 text-amber-400" />}
            title="Citation Correction"
            desc="Automatically fix and suggest proper formatting for missing or incorrect academic citations."
          />
        </motion.div>

        {/* Pipeline & Credibility */}
        <motion.div 
          initial="hidden" whileInView="visible" viewport={{ once: true }} variants={containerVariants}
          className="mt-32 bg-white/5 border border-white/10 rounded-3xl p-10 w-full backdrop-blur-sm"
        >
          <h2 className="text-3xl font-bold text-white mb-10">How It Works</h2>
          <div className="flex flex-col md:flex-row items-center justify-between gap-4 text-slate-300">
            <PipelineStep num="1" title="Upload" />
            <ArrowRight className="w-6 h-6 hidden md:block text-slate-600" />
            <PipelineStep num="2" title="Analyze" />
            <ArrowRight className="w-6 h-6 hidden md:block text-slate-600" />
            <PipelineStep num="3" title="Detect" />
            <ArrowRight className="w-6 h-6 hidden md:block text-slate-600" />
            <PipelineStep num="4" title="Compare" />
            <ArrowRight className="w-6 h-6 hidden md:block text-slate-600" />
            <PipelineStep num="5" title="Report" />
          </div>
        </motion.div>

        {/* Stats */}
        <motion.div 
          initial="hidden" whileInView="visible" viewport={{ once: true }} variants={containerVariants}
          className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8 w-full"
        >
          <div className="text-center">
            <div className="text-4xl font-extrabold text-blue-400 mb-2">95%+</div>
            <div className="text-slate-400 font-medium">Detection Accuracy</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-extrabold text-purple-400 mb-2">10K+</div>
            <div className="text-slate-400 font-medium">Documents Analyzed</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-extrabold text-emerald-400 mb-2">&lt;5s</div>
            <div className="text-slate-400 font-medium">Average Processing Time</div>
          </div>
        </motion.div>

      </main>

      <footer className="border-t border-white/10 bg-[#0B0F1A]/80 backdrop-blur-md py-8 absolute w-full bottom-0">
        <div className="max-w-7xl mx-auto px-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="text-slate-500 font-medium text-sm">
            © 2026 AuthentiText AI. Created for research and academic integrity.
          </div>
          <div className="flex gap-6 text-sm font-medium text-slate-400">
            <a href="#" className="hover:text-white transition">About</a>
            <a href="#" className="hover:text-white transition">Contact</a>
            <a href="#" className="hover:text-white transition">GitHub</a>
          </div>
        </div>
      </footer>
    </div>
  )
}

function FeatureCard({ icon, title, desc }) {
  return (
    <motion.div 
      variants={{ hidden: { y: 20, opacity: 0 }, visible: { y: 0, opacity: 1 } }}
      className="bg-white/5 border border-white/10 rounded-2xl p-6 flex flex-col items-start text-left hover:bg-white/10 transition-colors cursor-pointer group"
    >
      <div className="p-3 bg-white/5 rounded-xl mb-4 group-hover:scale-110 transition-transform">
        {icon}
      </div>
      <h3 className="text-xl font-bold text-white mb-2">{title}</h3>
      <p className="text-slate-400 text-sm leading-relaxed">{desc}</p>
    </motion.div>
  )
}

function PipelineStep({ num, title }) {
  return (
    <div className="flex flex-col items-center gap-3">
      <div className="w-12 h-12 rounded-full border-2 border-blue-500/30 flex items-center justify-center text-blue-400 font-bold bg-blue-500/10">
        {num}
      </div>
      <span className="font-semibold text-white">{title}</span>
    </div>
  )
}
