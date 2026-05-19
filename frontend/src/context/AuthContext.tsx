import { createContext, useContext, useState, useEffect } from "react";
import type { ReactNode } from "react";

type Role = "SUPER_ADMIN" | "ORG_ADMIN" | null;

interface User {
  id: string;
  name?: string;
  email: string;
  role: Role;
  organizationId?: number | null;
  avatarUrl?: string | null;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, user: User) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);
const AUTH_TOKEN_KEY = "billmygym_token";
const AUTH_USER_KEY = "billmygym_user";
const LEGACY_AUTH_TOKEN_KEY = "gymflow_token";
const LEGACY_AUTH_USER_KEY = "gymflow_user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem(AUTH_TOKEN_KEY) || localStorage.getItem(LEGACY_AUTH_TOKEN_KEY);
    const storedUser = localStorage.getItem(AUTH_USER_KEY) || localStorage.getItem(LEGACY_AUTH_USER_KEY);
    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      localStorage.setItem(AUTH_TOKEN_KEY, storedToken);
      localStorage.setItem(AUTH_USER_KEY, storedUser);
      localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
      localStorage.removeItem(LEGACY_AUTH_USER_KEY);
    }
    setIsLoading(false);
  }, []);

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem(AUTH_TOKEN_KEY, newToken);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(newUser));
    localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_AUTH_USER_KEY);
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_AUTH_USER_KEY);
  };

  if (isLoading) {
    return <div>Loading...</div>; // Could be a nicer loading state
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
