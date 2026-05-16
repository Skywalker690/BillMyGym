import { Link } from "react-router-dom";
import { ShieldAlert } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "@/components/ui/button";

export default function Unauthorized() {
  const { user } = useAuth();
  const dashboardLink = user?.role === "SUPER_ADMIN" ? "/app/platform-overview" : "/app/dashboard";

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 items-center">
      <ShieldAlert className="h-16 w-16 text-red-500 mb-4" />
      <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight">Access Denied</h2>
      <p className="mt-2 text-slate-600 text-center max-w-md">
        You do not have permission to view this page. If you believe this is an error, please contact support.
      </p>
      <div className="mt-8">
        <Link to={dashboardLink}>
          <Button>Return to Dashboard</Button>
        </Link>
      </div>
    </div>
  );
}
