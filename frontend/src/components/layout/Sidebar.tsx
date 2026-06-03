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
    <div className="flex items-center gap-4 text-white">
      <div className="relative flex h-14 w-14 shrink-0 items-center justify-center rounded-[1.55rem] bg-[linear-gradient(145deg,rgba(255,255,255,0.98),rgba(236,253,245,0.92))] p-[7px] shadow-[0_18px_44px_-22px_rgba(255,255,255,0.95)] ring-1 ring-white/65">
        <div className="absolute inset-[7px] rounded-[1.15rem] bg-[radial-gradient(circle_at_top,rgba(255,255,255,0.92),rgba(220,252,231,0.55)_60%,rgba(255,255,255,0.4))]" />
        <img
          src="/web-app-manifest-512x512.png"
          alt="BillMyGym"
          className="relative z-10 h-full w-full rounded-[1.1rem] object-contain drop-shadow-[0_10px_18px_rgba(15,23,42,0.18)]"
        />
      </div>
      {!collapsed && (
        <div className="min-w-0">
          <div className="text-[1.72rem] font-black tracking-[-0.04em] leading-none text-white">BillMyGym</div>
          <div className="mt-2 text-[10px] font-black uppercase tracking-[0.34em] text-emerald-300/95">Revenue OS</div>
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
        "relative group flex items-center rounded-2xl transition-all duration-300 overflow-hidden mb-2",
        collapsed ? "justify-center h-14 w-full px-0" : "w-full px-5 py-4",
        active
          ? "bg-white/10 text-white shadow-sm ring-1 ring-white/15"
          : "text-slate-400 hover:text-white hover:bg-white/5",
      )}
    >
      {active && <div className="absolute inset-0 bg-gradient-to-r from-indigo-500/30 to-transparent opacity-90" />}
      <div className={cn("relative z-10 flex items-center w-full", collapsed ? "justify-center" : "gap-4")}>
        <Icon
          className={cn(
            collapsed ? "h-6 w-6" : "h-5 w-5",
            active ? "text-indigo-300" : "text-slate-500 group-hover:text-indigo-300",
          )}
        />
        {!collapsed && <span className={cn("truncate text-sm tracking-tight", active ? "font-bold" : "font-medium")}>{label}</span>}
        {active && !collapsed && <div className="ml-auto h-2 w-2 rounded-full bg-indigo-300 shadow-[0_0_10px_rgba(165,180,252,0.8)]" />}
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

  return (
    <>
      {mobileOpen && <div className="fixed inset-0 z-[90] bg-slate-950/60 backdrop-blur-sm lg:hidden" onClick={onCloseMobile} />}
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-[99] p-3 transition-all duration-500 ease-[cubic-bezier(0.25,1,0.5,1)]",
          mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
          collapsed ? "w-24 lg:w-28" : "w-[92vw] max-w-[22rem] sm:w-80",
        )}
      >
        <div className="billmygym-dark-shell relative flex h-full flex-col overflow-hidden rounded-[2rem] border border-white/10">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(34,197,94,0.12),transparent_46%),radial-gradient(circle_at_bottom_left,rgba(251,191,36,0.08),transparent_34%)] pointer-events-none" />

          <div className={cn("relative z-10 flex items-center pt-9 pb-7", collapsed ? "justify-center px-0" : "justify-between px-7")}>
            <BillMyGymMark collapsed={collapsed} />
            {!collapsed && (
              <button onClick={onCloseMobile} className="rounded-xl p-2 text-slate-400 hover:text-white lg:hidden">
                <X className="h-5 w-5" />
              </button>
            )}
          </div>

          <div className="relative z-10 flex-1 overflow-y-auto px-4 pb-4">
            {Object.entries(groupedItems).map(([section, items]: [string, NavItemConfig[]]) => (
              <div key={section} className="mb-8 last:mb-0">
                {!collapsed && (
                  <p className="mb-4 px-2 text-[10px] font-black uppercase tracking-[0.34em] text-slate-500">
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
                      collapsed={collapsed}
                      onNavigate={onCloseMobile}
                    />
                  </div>
                ))}
              </div>
            ))}
          </div>

          <div className="relative z-10 mt-auto border-t border-white/5 p-4">
            <div className={cn("flex items-center rounded-[1.4rem] border border-white/5 bg-white/5", collapsed ? "justify-center p-2.5" : "gap-3 p-3")}>
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={displayName} referrerPolicy="no-referrer" className="h-11 w-11 rounded-2xl object-cover" />
              ) : (
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-blue-500 text-sm font-black text-white">
                  {initials}
                </div>
              )}
              {!collapsed && (
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-bold text-white">{displayName}</div>
                  <div className="mt-1 truncate text-[10px] font-black uppercase tracking-[0.22em] text-indigo-400">
                    {user?.role === "SUPER_ADMIN" ? "Super Admin" : "Org Admin"}
                  </div>
                </div>
              )}
            </div>

            <button
              onClick={onLogout}
              className={cn(
                "mt-3 flex w-full items-center rounded-2xl text-rose-400 transition-colors hover:bg-rose-50/10",
                collapsed ? "justify-center p-4" : "gap-3 px-5 py-4",
              )}
            >
              <LogOut className="h-5 w-5 shrink-0" />
              {!collapsed && <span className="text-sm font-bold">Terminate Session</span>}
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}

