import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { Sidebar } from "./Sidebar";
import { getPageContext, getProfileNavItems, getSidebarNavItems } from "./navigation";
import { Topbar } from "./Topbar";
import api from "../../lib/api";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const { data: referralsEnabled = true } = useQuery({
    queryKey: ["referralSettings"],
    queryFn: () => api.get("/referrals/settings").then((res) => Boolean(res.data)),
    staleTime: 30_000,
  });

  const role = user?.role ?? null;
  const sidebarItems = useMemo(() => getSidebarNavItems(role, referralsEnabled), [role, referralsEnabled]);
  const profileItems = useMemo(() => getProfileNavItems(role), [role]);
  const pageContext = useMemo(() => getPageContext(location.pathname, role), [location.pathname, role]);

  useEffect(() => {
    setMobileSidebarOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (role === "ORG_ADMIN" && !referralsEnabled && location.pathname.startsWith("/app/referrals")) {
      navigate("/app/dashboard", { replace: true });
    }
  }, [location.pathname, navigate, referralsEnabled, role]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="relative min-h-screen overflow-x-clip bg-[radial-gradient(circle_at_top_left,rgba(187,247,208,0.42),transparent_26%),radial-gradient(circle_at_top_right,rgba(255,247,237,0.72),transparent_28%),linear-gradient(180deg,#f6fbf8_0%,#eef4f0_46%,#f8fbf9_100%)]">
      <Sidebar
        user={user}
        navItems={sidebarItems}
        collapsed={sidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        onCloseMobile={() => setMobileSidebarOpen(false)}
        onLogout={handleLogout}
      />

      <div className={`min-h-screen min-w-0 overflow-x-clip transition-all duration-500 ${sidebarCollapsed ? "lg:pl-28" : "lg:pl-80"}`}>
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

        <main className="min-w-0 overflow-x-clip px-2 pb-10 pt-1 sm:px-4 lg:px-8">
          <div className="mx-auto min-w-0 max-w-[1600px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
