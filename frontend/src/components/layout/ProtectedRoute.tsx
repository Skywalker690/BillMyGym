import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function ProtectedRoute({
  allowedRoles,
  allowPendingConsent = false,
}: {
  allowedRoles: string[];
  allowPendingConsent?: boolean;
}) {
  const { isAuthenticated, user, consentRequired, isLoading } = useAuth();

  if (isLoading) {
    return <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm font-medium text-slate-500">Loading BillMyGym...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (consentRequired && !allowPendingConsent) {
    return <Navigate to="/consent" replace />;
  }

  if (user && !allowedRoles.includes(user.role as string)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
