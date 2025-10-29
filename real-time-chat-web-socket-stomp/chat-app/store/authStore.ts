import { create } from 'zustand';
import { User as FirebaseUser } from 'firebase/auth';

interface AuthStore {
  user: FirebaseUser | null;
  firebaseToken: string | null;
  userData: any | null;
  isAuthenticated: boolean;

  setUser: (user: FirebaseUser | null) => void;
  setFirebaseToken: (token: string | null) => void;
  setUserData: (data: any) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  firebaseToken: null,
  userData: null,
  isAuthenticated: false,

  setUser: (user) => set({ user, isAuthenticated: !!user }),
  setFirebaseToken: (token) => {
    if (token) {
      localStorage.setItem('firebaseToken', token);
    } else {
      localStorage.removeItem('firebaseToken');
    }
    set({ firebaseToken: token });
  },
  setUserData: (data) => set({ userData: data }),
  logout: () => {
    localStorage.removeItem('firebaseToken');
    set({
      user: null,
      firebaseToken: null,
      userData: null,
      isAuthenticated: false
    });
  },
}));