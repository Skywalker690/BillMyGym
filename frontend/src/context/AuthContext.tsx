import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import api, {
  clearStoredAuth,
  clearStoredUser,
  persistAuthToken,
  persistStoredUser,
  readAuthToken,
  readStoredUser,
} from "@/lib/api";
import { clearCachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";

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
const LEGAL_POLICY_STORAGE_KEY = "billmygym_legal_policy";
let legalPolicyRequest: Promise<LegalPolicyMetadata | null> | null = null;

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [legalPolicy, setLegalPolicy] = useState<LegalPolicyMetadata | null>(() => readStoredLegalPolicy());
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const bootstrapAuth = async () => {
      const storedToken = readAuthToken();
      const storedUser = readStoredUser();

      if (storedToken) {
        setToken(storedToken);
        persistAuthToken(storedToken);
      }
      if (storedUser) {
        try {
          setUser(JSON.parse(storedUser));
          persistStoredUser(storedUser);
        } catch {
          clearStoredUser();
        }
      }

      if (!readStoredLegalPolicy()) {
        const loadedPolicy = await loadLegalPolicyOnce();
        if (active) {
          setLegalPolicy(loadedPolicy);
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
    persistAuthToken(newToken);
    persistUser(newUser, setUser);
  };

  const updateUser = (nextUser: User | null) => {
    if (!nextUser) {
      setUser(null);
      clearStoredUser();
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
  persistStoredUser(JSON.stringify(nextUser));
}

function readStoredLegalPolicy() {
  if (!isBrowser()) {
    return null;
  }

  try {
    const serializedPolicy = window.localStorage.getItem(LEGAL_POLICY_STORAGE_KEY);
    return serializedPolicy ? JSON.parse(serializedPolicy) as LegalPolicyMetadata : null;
  } catch {
    window.localStorage.removeItem(LEGAL_POLICY_STORAGE_KEY);
    return null;
  }
}

function persistLegalPolicy(policy: LegalPolicyMetadata) {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.setItem(LEGAL_POLICY_STORAGE_KEY, JSON.stringify(policy));
  } catch {
    // Legal metadata is an optimization; the app can still use fallback text.
  }
}

function loadLegalPolicyOnce() {
  if (!legalPolicyRequest) {
    legalPolicyRequest = api.get("/auth/legal-policy")
      .then((response) => {
        persistLegalPolicy(response.data);
        return response.data as LegalPolicyMetadata;
      })
      .catch(() => null)
      .finally(() => {
        legalPolicyRequest = null;
      });
  }

  return legalPolicyRequest;
}

function isBrowser() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function clearAuth(
  setToken: (token: string | null) => void,
  setUser: (user: User | null) => void
) {
  setToken(null);
  setUser(null);
  clearStoredAuth();
  clearCachedSubscriptionStatus();
}
