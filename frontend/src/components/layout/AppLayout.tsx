import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { Sidebar } from "./Sidebar";
import { getPageContext, getProfileNavItems, getSidebarNavItems, type AppRole } from "./navigation";
import { Topbar } from "./Topbar";
import api from "../../lib/api";
import { DemoPlanPreviewProvider, useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const role = user?.role ?? null;
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const { data: referralsEnabled = true } = useQuery({
    queryKey: ["referralSettings"],
    queryFn: () => api.get("/referrals/settings").then((res) => Boolean(res.data)),
    staleTime: 30_000,
  });
  const { data: subscriptionStatus } = useQuery({
    queryKey: ["layoutSubscriptionStatus", user?.organizationId],
    queryFn: () => api.get("/subscription/status").then((res) => res.data),
    enabled: role === "ORG_ADMIN" && Boolean(user?.organizationId),
    staleTime: 60_000,
  });

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
  const isDemoWorkspace = subscriptionStatus?.organizationType === "DEMO";

  return (
    <DemoPlanPreviewProvider isDemoWorkspace={isDemoWorkspace}>
      <AppLayoutShell
        user={user}
        role={role}
        locationPath={location.pathname}
        referralsEnabled={referralsEnabled}
        sidebarCollapsed={sidebarCollapsed}
        mobileSidebarOpen={mobileSidebarOpen}
        onCloseMobile={() => setMobileSidebarOpen(false)}
        onToggleSidebar={() => setSidebarCollapsed((value) => !value)}
        onOpenMobileSidebar={() => setMobileSidebarOpen(true)}
        onLogout={handleLogout}
      />
    </DemoPlanPreviewProvider>
  );
}

function AppLayoutShell({
  user,
  role,
  locationPath,
  referralsEnabled,
  sidebarCollapsed,
  mobileSidebarOpen,
  onCloseMobile,
  onToggleSidebar,
  onOpenMobileSidebar,
  onLogout,
}: {
  user: ReturnType<typeof useAuth>["user"];
  role: AppRole | null;
  locationPath: string;
  referralsEnabled: boolean;
  sidebarCollapsed: boolean;
  mobileSidebarOpen: boolean;
  onCloseMobile: () => void;
  onToggleSidebar: () => void;
  onOpenMobileSidebar: () => void;
  onLogout: () => void;
}) {
  const { effectivePlanAccess } = useDemoPlanPreview();
  const previewPlanAccess = effectivePlanAccess();
  const sidebarItems = useMemo(
    () => getSidebarNavItems(role, referralsEnabled, Boolean(previewPlanAccess.canReceiveMonthlyReports)),
    [previewPlanAccess.canReceiveMonthlyReports, referralsEnabled, role]
  );
  const profileItems = useMemo(() => getProfileNavItems(role, referralsEnabled), [role, referralsEnabled]);
  const pageContext = useMemo(() => getPageContext(locationPath, role), [locationPath, role]);

  return (
    <div className="relative min-h-screen overflow-x-clip bg-[radial-gradient(circle_at_top_left,rgba(187,247,208,0.42),transparent_26%),radial-gradient(circle_at_top_right,rgba(255,247,237,0.72),transparent_28%),linear-gradient(180deg,#f6fbf8_0%,#eef4f0_46%,#f8fbf9_100%)]">
      <Sidebar
        user={user}
        navItems={sidebarItems}
        collapsed={sidebarCollapsed}
        mobileOpen={mobileSidebarOpen}
        onCloseMobile={onCloseMobile}
        onLogout={onLogout}
      />

      <div className={`min-h-screen min-w-0 overflow-x-clip transition-all duration-500 ${sidebarCollapsed ? "lg:pl-28" : "lg:pl-80"}`}>
        <Topbar
          user={user}
          role={role}
          pageContext={pageContext}
          profileNavItems={profileItems}
          sidebarCollapsed={sidebarCollapsed}
          onToggleSidebar={onToggleSidebar}
          onOpenMobileSidebar={onOpenMobileSidebar}
          onLogout={onLogout}
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
