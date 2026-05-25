import { useEffect, useRef, useState } from "react";
import {
  Building2,
  ChevronDown,
  CreditCard,
  FolderPlus,
  LogOut,
  Menu,
  PanelLeft,
  Settings,
  Shield,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { cn } from "@/lib/utils";
import type { AppRole, NavItemConfig } from "./navigation";

interface TopbarProps {
  user: {
    name?: string;
    email: string;
    avatarUrl?: string | null;
    role?: AppRole | null;
  } | null;
  role: AppRole | null | undefined;
  pageContext: {
    eyebrow: string;
    title: string;
    description: string;
  };
  profileNavItems: NavItemConfig[];
  sidebarCollapsed: boolean;
  onToggleSidebar: () => void;
  onOpenMobileSidebar: () => void;
  onLogout: () => void;
}

export function Topbar({
  user,
  role,
  pageContext,
  profileNavItems,
  sidebarCollapsed,
  onToggleSidebar,
  onOpenMobileSidebar,
  onLogout,
}: TopbarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    setProfileOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const primaryName = (user?.name || user?.email || "BillMyGym").split(" ")[0];
  const initials = (user?.name || user?.email || "G").trim().charAt(0).toUpperCase();
  const isSuperAdmin = role === "SUPER_ADMIN";
  const quickLinks = isSuperAdmin
    ? [
        {
          to: "/app/platform-overview",
          label: "Platform",
          icon: Shield,
          iconClassName: "text-emerald-700",
        },
        {
          to: "/app/organizations",
          label: "Organizations",
          icon: Building2,
          iconClassName: "text-emerald-600",
        },
        {
          to: "/app/organizations/new",
          label: "Add Org",
          icon: FolderPlus,
          iconClassName: "text-emerald-700",
        },
      ]
    : [
        {
          to: "/app/organization",
          label: "Organization",
          icon: Building2,
          iconClassName: "text-emerald-600",
        },
        {
          to: "/app/settings",
          label: "Settings",
          icon: Settings,
          iconClassName: "text-emerald-700",
        }
      ];

  return (
    <header
      className={cn(
        "sticky top-0 z-40 overflow-x-clip px-4 py-3 lg:px-8 lg:py-4",
      )}
    >
      <div
        className={cn(
          "billmygym-shell pointer-events-auto flex min-h-[84px] min-w-0 flex-wrap items-center justify-between gap-4 overflow-visible rounded-[2rem] border border-transparent px-4 py-4 shadow-[0_18px_50px_-24px_rgba(15,23,42,0.28)] backdrop-blur-xl transition-[border-color,background-color,box-shadow] duration-300",
          scrolled
            ? "border-white/80 bg-white/92 shadow-[0_20px_45px_-28px_rgba(15,23,42,0.35)]"
            : "bg-white/78 shadow-[0_18px_50px_-24px_rgba(15,23,42,0.22)]",
        )}
      >
        <div className="flex min-w-0 flex-1 items-center gap-3 lg:gap-6">
          <button
            onClick={onOpenMobileSidebar}
            className="rounded-2xl border border-white/60 bg-white/70 p-3 text-slate-600 shadow-sm transition-colors hover:text-indigo-600 lg:hidden"
          >
            <Menu className="h-5 w-5" />
          </button>

          <button
            onClick={onToggleSidebar}
            className="hidden rounded-2xl border border-white/70 bg-white/80 p-3 text-slate-600 shadow-sm transition-colors hover:text-emerald-700 lg:inline-flex"
            aria-label={sidebarCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            <PanelLeft className={cn("h-5 w-5 transition-transform duration-500", sidebarCollapsed && "rotate-180")} />
          </button>

          <div className="min-w-0 flex-1">
            <div className="mb-1 flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-emerald-600 shadow-[0_0_6px_rgba(22,163,74,0.5)]" />
              <span className="truncate text-[10px] font-black uppercase tracking-[0.28em] text-emerald-700">
                {pageContext.eyebrow}
              </span>
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-2xl font-black tracking-tight text-slate-900 lg:text-3xl">{pageContext.title}</h1>
              <p className="mt-1 hidden truncate text-sm font-medium text-slate-500 lg:block">{pageContext.description}</p>
            </div>
          </div>

        </div>

        <div className="flex max-w-full flex-wrap items-center justify-end gap-2 sm:w-auto sm:gap-3">
          {quickLinks.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className="inline-flex shrink-0 items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50"
            >
              <item.icon className={cn("h-4 w-4", item.iconClassName)} />
              <span className="hidden sm:inline">{item.label}</span>
            </Link>
          ))}

          <div className="relative z-50" ref={profileRef}>
            <button
              onClick={() => setProfileOpen((open) => !open)}
              className={cn(
                "group flex items-center gap-3 rounded-full border border-white/60 bg-white/85 p-2 pr-4 shadow-sm transition-colors",
                profileOpen ? "ring-2 ring-emerald-200" : "hover:bg-white",
              )}
            >
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={primaryName} referrerPolicy="no-referrer" className="h-11 w-11 rounded-full object-cover" />
              ) : (
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-blue-500 text-sm font-black text-white">
                  {initials}
                </div>
              )}
              <div className="hidden text-left 2xl:block">
                <p className="text-sm font-black leading-none text-slate-900">{primaryName}</p>
                <p className="mt-1.5 text-[10px] font-black uppercase tracking-[0.22em] text-slate-400">
                  {role === "SUPER_ADMIN" ? "SUPER ADMIN" : "ORG ADMIN"}
                </p>
              </div>
              <ChevronDown className={cn("hidden h-4 w-4 text-slate-400 transition-transform 2xl:block", profileOpen && "rotate-180")} />
            </button>

            {profileOpen && (
              <div className="absolute right-0 top-full z-[60] mt-4 w-[min(21rem,calc(100vw-1rem))] overflow-hidden rounded-[2rem] border border-white/70 bg-white/95 p-2 shadow-[0_22px_60px_-24px_rgba(15,23,42,0.32)] backdrop-blur-xl">
                <div className="mb-2 flex items-center gap-4 rounded-[1.6rem] border border-white/80 bg-white/70 p-5 shadow-sm">
                  {user?.avatarUrl ? (
                    <img src={user.avatarUrl} alt={primaryName} referrerPolicy="no-referrer" className="h-14 w-14 rounded-[1.1rem] object-cover" />
                  ) : (
                    <div className="flex h-14 w-14 items-center justify-center rounded-[1.1rem] bg-slate-900 text-lg font-black text-white">
                      {initials}
                    </div>
                  )}
                  <div className="min-w-0">
                    <h4 className="truncate text-sm font-black text-slate-900">{user?.name || primaryName}</h4>
                    <p className="truncate text-[10px] font-bold uppercase tracking-[0.22em] text-slate-400">{user?.email}</p>
                    <div className="mt-2 inline-flex items-center gap-1.5 rounded-full border border-emerald-100 bg-emerald-50 px-2 py-1">
                      <div className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                      <span className="text-[9px] font-black uppercase tracking-[0.2em] text-emerald-600">Session Active</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-1 p-1">
                  {profileNavItems.map((item) => (
                    <button
                      key={item.href}
                      onClick={() => {
                        setProfileOpen(false);
                        navigate(item.href);
                      }}
                      className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-white/70 px-5 py-3 text-slate-800 transition-colors hover:bg-white"
                    >
                      <span className="text-xs font-black uppercase tracking-[0.2em]">{item.label}</span>
                      <item.icon className="h-4 w-4 text-slate-500" />
                    </button>
                  ))}
                </div>

                <div className="mt-2 p-2">
                  <button
                    onClick={onLogout}
                    className="flex w-full items-center justify-between rounded-[1.6rem] bg-slate-950 px-6 py-4 text-white shadow-xl transition-colors hover:bg-slate-900"
                  >
                    <span className="text-[10px] font-black uppercase tracking-[0.2em]">Terminate Session</span>
                    <LogOut className="h-4 w-4 text-slate-400" />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

