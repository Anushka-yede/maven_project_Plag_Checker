import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

/**
 * Global auth store — JWT token and user info.
 * Stored in sessionStorage (not localStorage) for security.
 */
export const useAuthStore = create(
  persist(
    (set, get) => ({
      token: null,
      user: null,   // { email, role }

      setAuth: (token, user) => set({ token, user }),
      clearAuth: () => set({ token: null, user: null }),

      isAuthenticated: () => Boolean(get().token),
      isAdmin: () => get().user?.role === 'ADMIN',
      isInstructor: () => ['INSTRUCTOR', 'ADMIN'].includes(get().user?.role),
    }),
    {
      name: 'pc-auth',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
)
