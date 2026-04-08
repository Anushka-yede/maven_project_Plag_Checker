/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        bg: '#0B0F1A',
        primary: '#3B82F6',
        secondary: '#8B5CF6',
        accent: '#22d3ee',
        card: 'rgba(255, 255, 255, 0.05)',
        textMain: '#f8fafc',
        textMuted: '#94a3b8',
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d7fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
        },
        danger:  { DEFAULT: '#ef4444', light: '#fef2f2', dark: '#dc2626' },
        warning: { DEFAULT: '#f59e0b', light: '#fffbeb', dark: '#d97706' },
        success: { DEFAULT: '#10b981', light: '#ecfdf5', dark: '#059669' },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      animation: {
        'fade-in':    'fadeIn 0.3s ease-out',
        'slide-up':   'slideUp 0.4s ease-out',
        'pulse-slow': 'pulse 3s infinite',
        'spin-slow':  'spin 2s linear infinite',
      },
      keyframes: {
        fadeIn:  { from: { opacity: '0' }, to: { opacity: '1' } },
        slideUp: { from: { opacity: '0', transform: 'translateY(20px)' },
                   to:   { opacity: '1', transform: 'translateY(0)' } },
      },
      backdropBlur: { xs: '2px' },
    },
  },
  plugins: [],
}
