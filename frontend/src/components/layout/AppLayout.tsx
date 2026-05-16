import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { 
  LayoutDashboard, 
  Users, 
  Settings as SettingsIcon, 
  Bell, 
  Building,
  LogOut,
  Dumbbell,
  Activity,
  Clock
} from "lucide-react";
import { cn } from "@/lib/utils";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const navItems = user?.role === "SUPER_ADMIN" 
    ? [
        { name: "Platform Overview", href: "/app/platform-overview", icon: Activity },
        { name: "Organizations", href: "/app/organizations", icon: Building },
      ]
    : [
        { name: "Dashboard", href: "/app/dashboard", icon: LayoutDashboard },
        { name: "Members", href: "/app/members", icon: Users },
        { name: "Reminders", href: "/app/reminders", icon: Clock },
        { name: "Notifications", href: "/app/notifications", icon: Bell },
        { name: "Settings", href: "/app/settings", icon: SettingsIcon },
        { name: "Organization Profile", href: "/app/organization", icon: Building },
      ];

  return (
    <div className="flex h-screen bg-transparent overflow-hidden">
      {/* Floating Sidebar */}
      <div className="w-[260px] my-4 ml-4 rounded-3xl glass-sidebar flex flex-col flex-shrink-0 z-10 transition-all duration-300">
        <div className="h-20 flex items-center px-8 border-b border-white/10 dark:border-white/5">
          <Dumbbell className="h-7 w-7 text-indigo-600 dark:text-indigo-400 mr-3" />
          <span className="font-bold text-xl tracking-wider text-slate-900 dark:text-white uppercase text-[15px]">GymFlow</span>
        </div>
        
        <nav className="flex-1 py-6 px-4 space-y-2 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = location.pathname.startsWith(item.href);
            return (
              <Link
                key={item.name}
                to={item.href}
                className={cn(
                  "flex items-center px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200 group relative",
                  isActive 
                    ? "bg-slate-900 text-white shadow-md shadow-slate-900/20 dark:bg-white dark:text-slate-900" 
                    : "text-slate-600 hover:bg-white/50 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-white/10 dark:hover:text-white"
                )}
              >
                {isActive && (
                  <span className="absolute left-0 w-1 h-6 bg-indigo-500 rounded-r-full -ml-4" />
                )}
                <item.icon className={cn(
                  "mr-3 h-5 w-5 flex-shrink-0 transition-transform duration-200 group-hover:scale-110", 
                  isActive ? "text-indigo-400 dark:text-indigo-600" : "text-slate-400"
                )} />
                {item.name}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-white/10 dark:border-white/5">
          <div className="flex items-center w-full px-4 py-3 text-sm font-medium text-slate-700 dark:text-slate-300 rounded-xl bg-white/40 dark:bg-slate-800/40 backdrop-blur-sm border border-white/40 dark:border-white/10 mb-2 shadow-sm">
            <span className="truncate">{user?.name ?? user?.email}</span>
          </div>
          <button
            onClick={handleLogout}
            className="flex w-full items-center px-4 py-3 text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-xl transition-colors"
          >
            <LogOut className="mr-3 h-5 w-5 flex-shrink-0" />
            Sign Out
          </button>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto w-full p-4 pl-8">
        <div className="h-full rounded-3xl pb-20">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
