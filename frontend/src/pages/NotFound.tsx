import { Link } from "react-router-dom";
import { FileQuestion } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 items-center">
      <FileQuestion className="h-16 w-16 text-slate-400 mb-4" />
      <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight">Page Not Found</h2>
      <p className="mt-2 text-slate-600 text-center max-w-md">
        The page you are looking for does not exist or has been moved.
      </p>
      <div className="mt-8">
        <Link to="/">
          <Button>Return Home</Button>
        </Link>
      </div>
    </div>
  );
}
