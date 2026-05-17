import { useEffect, useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { Sidebar } from "./Sidebar";
import { getPageContext, getProfileNavItems, getSidebarNavItems } from "./navigation";
import { Topbar } from "./Topbar";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  const role = user?.role ?? null;
  const sidebarItems = useMemo(() => getSidebarNavItems(role), [role]);
  const profileItems = useMemo(() => getProfileNavItems(role), [role]);
  const pageContext = useMemo(() => getPageContext(location.pathname, role), [location.pathname, role]);

  useEffect(() => {
    setMobileSidebarOpen(false);
  }, [location.pathname]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,rgba(187,247,208,0.42),transparent_26%),radial-gradient(circle_at_top_right,rgba(255,247,237,0.72),transparent_28%),linear-gradient(180deg,#f6fbf8_0%,#eef4f0_46%,#f8fbf9_100%)]">
      <Sidebar
        user={user}
        navItems={sidebarItems}
        collapsed={sidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        onCloseMobile={() => setMobileSidebarOpen(false)}
        onLogout={handleLogout}
      />

      <div className={`min-h-screen transition-all duration-500 ${sidebarCollapsed ? "lg:pl-28" : "lg:pl-80"}`}>
        <Topbar
          user={user}
          role={role}
          pageContext={pageContext}
          profileNavItems={profileItems}
          sidebarCollapsed={sidebarCollapsed}
          onToggleSidebar={() => setSidebarCollapsed((value) => !value)}
          onOpenMobileSidebar={() => setMobileSidebarOpen(true)}
          onLogout={handleLogout}
        />

        <main className="px-2 pb-10 pt-1 sm:px-4 lg:px-8">
          <div className="mx-auto max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
