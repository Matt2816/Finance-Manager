"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { getAuthApiUrl } from "@/lib/api-config";
import {
  authenticatedJson,
  getStoredToken,
  setStoredToken,
} from "@/lib/authenticated-fetch";

export interface AuthUser {
  id: number;
  username: string;
  email: string;
}

interface AuthResponse {
  token: string;
  user: AuthUser;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    setStoredToken(null);
    setToken(null);
    setUser(null);
  }, []);

  const applyAuth = useCallback((response: AuthResponse) => {
    setStoredToken(response.token);
    setToken(response.token);
    setUser(response.user);
  }, []);

  useEffect(() => {
    const stored = getStoredToken();
    if (!stored) {
      setIsLoading(false);
      return;
    }

    setToken(stored);
    authenticatedJson<AuthUser>(`${getAuthApiUrl()}/me`, {}, stored)
      .then((me) => setUser(me))
      .catch(() => logout())
      .finally(() => setIsLoading(false));
  }, [logout]);

  const login = useCallback(
    async (username: string, password: string) => {
      const response = await fetch(`${getAuthApiUrl()}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Login failed");
      }
      applyAuth((await response.json()) as AuthResponse);
    },
    [applyAuth]
  );

  const register = useCallback(
    async (username: string, email: string, password: string) => {
      const response = await fetch(`${getAuthApiUrl()}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, email, password }),
      });
      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Registration failed");
      }
      applyAuth((await response.json()) as AuthResponse);
    },
    [applyAuth]
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: !!user && !!token,
      isLoading,
      login,
      register,
      logout,
    }),
    [user, token, isLoading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
