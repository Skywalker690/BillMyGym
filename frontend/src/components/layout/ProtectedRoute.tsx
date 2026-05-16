import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function ProtectedRoute({ allowedRoles }: { allowedRoles: string[] }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user && !allowedRoles.includes(user.role as string)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
