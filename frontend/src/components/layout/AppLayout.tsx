import { useEffect, useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { FlaskConical } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { Sidebar } from "./Sidebar";
import { getPageContext, getProfileNavItems, getSidebarNavItems, type AppRole } from "./navigation";
import { ScrollToTopButton } from "./ScrollToTopButton";
import { Topbar } from "./Topbar";
import { DemoPlanPreviewProvider } from "@/context/DemoPlanPreviewContext";
import { readCachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";
import { isDemoOrganizationContext } from "@/lib/demoWorkspaceData";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const role = user?.role ?? null;
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const referralsEnabled = true;
  const subscriptionStatus = readCachedSubscriptionStatus();

  useEffect(() => {
    setMobileSidebarOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (role !== "ORG_ADMIN" || !subscriptionStatus) {
      return;
    }
    if (subscriptionStatus.organizationType === "DEMO") {
      return;
    }
    const inactive = subscriptionStatus.organizationStatus === "PAUSED" || subscriptionStatus.organizationStatus === "DEACTIVATED";
    if (!inactive) {
      return;
    }
    if (location.pathname.startsWith("/app/subscription")) {
      return;
    }
    navigate("/app/subscription/recovery", { replace: true });
  }, [location.pathname, navigate, role, subscriptionStatus]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };
  const isDemoWorkspace = isDemoOrganizationContext({
    organizationId: user?.organizationId ?? subscriptionStatus?.organizationId,
    organizationType: subscriptionStatus?.organizationType,
  });


  return (
    <DemoPlanPreviewProvider isDemoWorkspace={isDemoWorkspace}>
      <AppLayoutShell
        user={user}
        role={role}
        locationPath={location.pathname}
        referralsEnabled={referralsEnabled}
        sidebarCollapsed={sidebarCollapsed}
        mobileSidebarOpen={mobileSidebarOpen}
        isDemoWorkspace={isDemoWorkspace}
        canUseSupportAutomation={(subscriptionStatus?.canUseSupportAutomation as boolean | undefined) ?? true}
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
  isDemoWorkspace,
  canUseSupportAutomation,
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
  isDemoWorkspace: boolean;
  canUseSupportAutomation: boolean;
  onCloseMobile: () => void;
  onToggleSidebar: () => void;
  onOpenMobileSidebar: () => void;
  onLogout: () => void;
}) {
  const sidebarItems = useMemo(
    () => getSidebarNavItems(role, referralsEnabled, canUseSupportAutomation),
    [referralsEnabled, role, canUseSupportAutomation]
  );
  const profileItems = useMemo(() => getProfileNavItems(role, referralsEnabled, canUseSupportAutomation), [role, referralsEnabled, canUseSupportAutomation]);
  const pageContext = useMemo(() => getPageContext(locationPath, role), [locationPath, role]);
  const showScrollToTopButton = role === "ORG_ADMIN";

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

      <div className={`min-h-screen min-w-0 overflow-x-clip transition-all duration-500 ${sidebarCollapsed ? "lg:pl-28" : "lg:pl-[19.5rem]"}`}>
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
            {role === "ORG_ADMIN" && isDemoWorkspace && (
              <div className="mx-auto mt-2 max-w-7xl rounded-[1.1rem] border border-sky-200 bg-sky-50 px-3 py-2 text-sky-900 shadow-sm sm:rounded-[1.35rem] sm:px-4 sm:py-3">
                <div className="flex items-start gap-2.5">
                  <div className="mt-0.5 rounded-xl bg-sky-100 p-1.5 text-sky-700">
                    <FlaskConical className="h-4 w-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-black uppercase tracking-[0.18em]">Demo Workspace</div>
                    <div className="mt-0.5 text-xs font-semibold leading-5 sm:text-sm">
                      This workspace contains demonstration data and is not intended for production use.
                    </div>
                  </div>
                </div>
              </div>
            )}
            <Outlet />
          </div>
        </main>
        <ScrollToTopButton visible={showScrollToTopButton} />
      </div>
    </div>
  );
}
