import { LogOut, X } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useMemo } from "react";
import { Link, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import type { AppRole, NavItemConfig } from "./navigation";

interface SidebarProps {
  user: {
    name?: string;
    email: string;
    avatarUrl?: string | null;
    role?: AppRole | null;
  } | null;
  navItems: NavItemConfig[];
  collapsed: boolean;
  mobileOpen: boolean;
  onCloseMobile: () => void;
  onLogout: () => void;
}

function BillMyGymMark({ collapsed }: { collapsed: boolean }) {
  return (
    <div className="flex items-center gap-3.5 text-white">
      <div className="relative flex h-[3.4rem] w-[3.4rem] shrink-0 items-center justify-center rounded-[1.65rem] bg-[linear-gradient(145deg,rgba(255,255,255,0.98),rgba(236,253,245,0.94))] p-[7px] shadow-[0_20px_48px_-24px_rgba(255,255,255,0.95)] ring-1 ring-white/70">
        <div className="absolute inset-[7px] rounded-[1.18rem] bg-[radial-gradient(circle_at_top,rgba(255,255,255,0.95),rgba(220,252,231,0.62)_58%,rgba(255,251,235,0.42))]" />
        <img
          src="/web-app-manifest-512x512.png"
          alt="BillMyGym"
          className="relative z-10 h-full w-full rounded-[1.12rem] object-contain drop-shadow-[0_10px_18px_rgba(15,23,42,0.18)]"
        />
      </div>
      {!collapsed && (
        <div className="min-w-0">
          <div className="text-[1.68rem] font-black leading-none tracking-tight text-white">BillMyGym</div>
          <div className="mt-2 text-[10px] font-black uppercase tracking-[0.32em] text-amber-200/90">Revenue OS</div>
        </div>
      )}
    </div>
  );
}

function NavItem({
  icon: Icon,
  label,
  to,
  active,
  collapsed,
  onNavigate,
}: {
  icon: LucideIcon;
  label: string;
  to: string;
  active: boolean;
  collapsed: boolean;
  onNavigate: () => void;
}) {
  return (
    <Link
      to={to}
      onClick={onNavigate}
      className={cn(
        "relative group mb-2 flex items-center overflow-hidden rounded-[1.35rem] transition-all duration-300",
        collapsed ? "h-14 w-full justify-center px-0" : "w-full px-4 py-3.5",
        active
          ? "bg-white/11 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.13)] ring-1 ring-white/15"
          : "text-slate-400 hover:bg-white/[0.065] hover:text-white",
      )}
    >
      {active && <div className="absolute inset-0 bg-[linear-gradient(110deg,rgba(16,185,129,0.32),rgba(245,158,11,0.12)_55%,transparent)] opacity-95" />}
      <div className={cn("relative z-10 flex w-full items-center", collapsed ? "justify-center" : "gap-3.5")}>
        <Icon
          className={cn(
            collapsed ? "h-6 w-6" : "h-5 w-5",
            active ? "text-emerald-200" : "text-slate-500 group-hover:text-emerald-200",
          )}
        />
        {!collapsed && <span className={cn("truncate text-sm tracking-tight", active ? "font-bold" : "font-medium")}>{label}</span>}
        {active && !collapsed && <div className="ml-auto h-2 w-2 rounded-full bg-amber-200 shadow-[0_0_12px_rgba(253,230,138,0.74)]" />}
      </div>
    </Link>
  );
}

export function Sidebar({
  user,
  navItems,
  collapsed,
  mobileOpen,
  onCloseMobile,
  onLogout,
}: SidebarProps) {
  const location = useLocation();
  const groupedItems = useMemo<Record<string, NavItemConfig[]>>(() => {
    return navItems.reduce<Record<string, NavItemConfig[]>>((groups, item) => {
      groups[item.section] ??= [];
      groups[item.section].push(item);
      return groups;
    }, {});
  }, [navItems]);

  const displayName = (user?.name || user?.email || "BillMyGym").split(" ")[0];
  const initials = (user?.name || user?.email || "G").trim().charAt(0).toUpperCase();
  const hideOrgAdminMobileLabels = user?.role === "ORG_ADMIN";
  const compactMobileNav = hideOrgAdminMobileLabels && mobileOpen;

  return (
    <>
      {mobileOpen && <div className="fixed inset-0 z-[90] bg-slate-950/60 backdrop-blur-sm lg:hidden" onClick={onCloseMobile} />}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-[99] p-3 transition-all duration-500 ease-[cubic-bezier(0.25,1,0.5,1)]",
          mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
          collapsed ? "w-24 lg:w-28" : compactMobileNav ? "w-[5.75rem] lg:w-[19.5rem]" : "w-[90vw] max-w-[21.5rem] sm:w-[19.5rem]",
        )}
      >
        <div className="billmygym-dark-shell relative flex h-full flex-col overflow-hidden rounded-[2.35rem] border border-white/10">
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(34,197,94,0.16),transparent_45%),radial-gradient(circle_at_bottom_left,rgba(251,191,36,0.11),transparent_36%)]" />

          <div className={cn("relative z-10 flex items-center pb-6 pt-8", collapsed || compactMobileNav ? "justify-center px-0 lg:justify-between lg:px-6" : "justify-between px-6")}>
            <BillMyGymMark collapsed={collapsed || compactMobileNav} />
            {!collapsed && !compactMobileNav && (
              <button onClick={onCloseMobile} className="rounded-[1rem] p-2 text-slate-400 hover:bg-white/5 hover:text-white lg:hidden">
                <X className="h-5 w-5" />
              </button>
            )}
            {compactMobileNav && (
              <button onClick={onCloseMobile} className="absolute right-4 top-8 rounded-[1rem] p-2 text-slate-400 hover:bg-white/5 hover:text-white lg:hidden">
                <X className="h-5 w-5" />
              </button>
            )}
          </div>

          <div className="relative z-10 flex-1 overflow-y-auto px-3.5 pb-4">
            {Object.entries(groupedItems).map(([section, items]: [string, NavItemConfig[]]) => (
              <div key={section} className="mb-7 last:mb-0">
                {!collapsed && !compactMobileNav && (
                  <p className="mb-3.5 px-2 text-[10px] font-black uppercase tracking-[0.3em] text-slate-500">
                    {section}
                  </p>
                )}
                {items.map((item) => (
                  <div key={item.href}>
                    <NavItem
                      icon={item.icon}
                      label={item.label}
                      to={item.href}
                      active={location.pathname.startsWith(item.href)}
                      collapsed={collapsed || compactMobileNav}
                      onNavigate={onCloseMobile}
                    />
                  </div>
                ))}
              </div>
            ))}
          </div>

          <div className="relative z-10 mt-auto border-t border-white/5 p-3.5">
            <div className={cn("flex items-center rounded-[1.55rem] border border-white/7 bg-white/[0.055] shadow-[inset_0_1px_0_rgba(255,255,255,0.05)]", collapsed || compactMobileNav ? "justify-center p-2.5 lg:gap-3 lg:p-3" : "gap-3 p-3")}>
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={displayName} referrerPolicy="no-referrer" className="h-11 w-11 rounded-[1.15rem] object-cover" />
              ) : (
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[1.15rem] bg-gradient-to-br from-emerald-500 to-slate-700 text-sm font-black text-white">
                  {initials}
                </div>
              )}
              {!collapsed && !compactMobileNav && (
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-bold text-white">{displayName}</div>
                  <div className="mt-1 truncate text-[10px] font-black uppercase tracking-[0.2em] text-emerald-300/80">
                    {user?.role === "SUPER_ADMIN" ? "Super Admin" : "Org Admin"}
                  </div>
                </div>
              )}
            </div>

            <button
              onClick={onLogout}
              className={cn(
                "mt-3 flex w-full items-center rounded-[1.35rem] text-rose-300 transition-colors hover:bg-rose-50/10 hover:text-rose-200",
                collapsed || compactMobileNav ? "justify-center p-4 lg:gap-3 lg:px-4 lg:py-3.5" : "gap-3 px-4 py-3.5",
              )}
            >
              <LogOut className="h-5 w-5 shrink-0" />
              {!collapsed && !compactMobileNav && <span className="text-sm font-bold">Terminate Session</span>}
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}

