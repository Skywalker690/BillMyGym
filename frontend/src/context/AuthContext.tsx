import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import api from "@/lib/api";

type Role = "SUPER_ADMIN" | "ORG_ADMIN" | null;

export interface User {
  id: number;
  name?: string;
  email: string;
  role: Role;
  organizationId?: number | null;
  avatarUrl?: string | null;
  termsAccepted?: boolean | null;
  privacyAccepted?: boolean | null;
  policyVersion?: string | null;
  policyAcceptedAt?: string | null;
  consentRequired?: boolean | null;
  consentMessage?: string | null;
}

export interface LegalPolicyMetadata {
  applicationName: string;
  currentVersion: string;
  effectiveDate: string;
  supportEmail: string;
  legalEmail: string;
  privacyEmail: string;
  companyAddress: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  legalPolicy: LegalPolicyMetadata | null;
  login: (token: string, user: User) => void;
  updateUser: (user: User | null) => void;
  refreshUser: () => Promise<User | null>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
  consentRequired: boolean;
  consentMessage: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);
const AUTH_TOKEN_KEY = "billmygym_token";
const AUTH_USER_KEY = "billmygym_user";
const LEGACY_AUTH_TOKEN_KEY = "gymflow_token";
const LEGACY_AUTH_USER_KEY = "gymflow_user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [legalPolicy, setLegalPolicy] = useState<LegalPolicyMetadata | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const bootstrapAuth = async () => {
      const storedToken = localStorage.getItem(AUTH_TOKEN_KEY) || localStorage.getItem(LEGACY_AUTH_TOKEN_KEY);
      const storedUser = localStorage.getItem(AUTH_USER_KEY) || localStorage.getItem(LEGACY_AUTH_USER_KEY);

      if (storedToken) {
        setToken(storedToken);
        localStorage.setItem(AUTH_TOKEN_KEY, storedToken);
        localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
      }
      if (storedUser) {
        try {
          setUser(JSON.parse(storedUser));
          localStorage.setItem(AUTH_USER_KEY, storedUser);
          localStorage.removeItem(LEGACY_AUTH_USER_KEY);
        } catch {
          localStorage.removeItem(AUTH_USER_KEY);
          localStorage.removeItem(LEGACY_AUTH_USER_KEY);
        }
      }

      try {
        const legalResponse = await api.get("/auth/legal-policy");
        if (active) {
          setLegalPolicy(legalResponse.data);
        }
      } catch {
        if (active) {
          setLegalPolicy(null);
        }
      }

      if (!storedToken) {
        if (active) {
          setIsLoading(false);
        }
        return;
      }

      try {
        const meResponse = await api.get("/auth/me");
        if (active) {
          persistUser(meResponse.data, setUser);
        }
      } catch {
        if (active) {
          clearAuth(setToken, setUser);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    void bootstrapAuth();

    return () => {
      active = false;
    };
  }, []);

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    localStorage.setItem(AUTH_TOKEN_KEY, newToken);
    localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
    persistUser(newUser, setUser);
  };

  const updateUser = (nextUser: User | null) => {
    if (!nextUser) {
      setUser(null);
      localStorage.removeItem(AUTH_USER_KEY);
      localStorage.removeItem(LEGACY_AUTH_USER_KEY);
      return;
    }
    persistUser(nextUser, setUser);
  };

  const refreshUser = async () => {
    if (!token) {
      return null;
    }
    const response = await api.get("/auth/me");
    persistUser(response.data, setUser);
    return response.data;
  };

  const logout = () => {
    clearAuth(setToken, setUser);
  };

  const consentRequired = useMemo(() => {
    if (!user) {
      return false;
    }
    if (!user.termsAccepted || !user.privacyAccepted) {
      return true;
    }
    if (legalPolicy && user.policyVersion !== legalPolicy.currentVersion) {
      return true;
    }
    return Boolean(user.consentRequired);
  }, [legalPolicy, user]);

  const consentMessage = useMemo(() => {
    if (user?.consentMessage) {
      return user.consentMessage;
    }
    if (legalPolicy && user?.policyVersion && user.policyVersion !== legalPolicy.currentVersion) {
      return "Our legal documents have been updated. Please review and accept the latest version to continue.";
    }
    return "Please review and accept our Terms & Conditions and Privacy Policy to access the application.";
  }, [legalPolicy, user]);

  if (isLoading) {
    return <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm font-medium text-slate-500">Loading BillMyGym...</div>;
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        legalPolicy,
        login,
        updateUser,
        refreshUser,
        logout,
        isAuthenticated: !!token,
        isLoading,
        consentRequired,
        consentMessage,
      }}
    >
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

function persistUser(nextUser: User, setUser: (user: User | null) => void) {
  setUser(nextUser);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser));
  localStorage.removeItem(LEGACY_AUTH_USER_KEY);
}

function clearAuth(
  setToken: (token: string | null) => void,
  setUser: (user: User | null) => void
) {
  setToken(null);
  setUser(null);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
  localStorage.removeItem(LEGACY_AUTH_USER_KEY);
}
