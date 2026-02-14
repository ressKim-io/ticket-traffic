import { create } from "zustand";

interface UIState {
  isMobileMenuOpen: boolean;
  isLoading: boolean;

  setMobileMenuOpen: (open: boolean) => void;
  toggleMobileMenu: () => void;
  setLoading: (loading: boolean) => void;
}

export const useUIStore = create<UIState>()((set) => ({
  isMobileMenuOpen: false,
  isLoading: false,

  setMobileMenuOpen: (open) => set({ isMobileMenuOpen: open }),
  toggleMobileMenu: () =>
    set((state) => ({ isMobileMenuOpen: !state.isMobileMenuOpen })),
  setLoading: (loading) => set({ isLoading: loading }),
}));
