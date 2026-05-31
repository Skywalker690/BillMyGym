import {
  Activity,
  BarChart3,
  ClipboardList,
  Bell,
  Building2,
  Clock3,
  CreditCard,
  LayoutDashboard,
  Gift,
  type LucideIcon,
  Settings,
  Users,
} from "lucide-react";

export type AppRole = "SUPER_ADMIN" | "ORG_ADMIN";

export interface NavItemConfig {
  label: string;
  href: string;
  icon: LucideIcon;
  section: string;
}

export const getSidebarNavItems = (
  role: AppRole | null | undefined,
  referralsEnabled: boolean = true,
  canViewReports: boolean = true,
): NavItemConfig[] => {
  if (role === "SUPER_ADMIN") {
    return [
      {
        label: "Platform Overview",
        href: "/app/platform-overview",
        icon: Activity,
        section: "Platform",
      },
      {
        label: "Organizations",
        href: "/app/organizations",
        icon: Building2,
        section: "Platform",
      },
      {
        label: "Reports",
        href: "/app/platform-reports",
        icon: BarChart3,
        section: "Platform",
      },
      {
        label: "Audit Logs",
        href: "/app/audit-logs",
        icon: ClipboardList,
        section: "Platform",
      },
    ];
  }

  const orgAdminItems: NavItemConfig[] = [
    {
      label: "Dashboard",
      href: "/app/dashboard",
      icon: LayoutDashboard,
      section: "Operations",
    },
    {
      label: "Members",
      href: "/app/members",
      icon: Users,
      section: "Operations",
    },
    {
      label: "Reminders",
      href: "/app/reminders",
      icon: Clock3,
      section: "Operations",
    },
    {
      label: "Billing",
      href: "/app/subscription",
      icon: CreditCard,
      section: "Billing",
    },
  ];

  if (canViewReports) {
    orgAdminItems.splice(2, 0, {
      label: "Reports",
      href: "/app/reports",
      icon: BarChart3,
      section: "Operations",
    });
  }

  return orgAdminItems;
};

export const getProfileNavItems = (
  role: AppRole | null | undefined,
  referralsEnabled: boolean = true,
): NavItemConfig[] => {
  if (role === "SUPER_ADMIN") {
    return [
      {
        label: "Organizations",
        href: "/app/organizations",
        icon: Building2,
        section: "Account",
      },
      {
        label: "Reports",
        href: "/app/platform-reports",
        icon: BarChart3,
        section: "Account",
      },
      {
        label: "Audit Logs",
        href: "/app/audit-logs",
        icon: ClipboardList,
        section: "Account",
      },
      {
        label: "Referrals",
        href: "/app/referrals/analytics",
        icon: Gift,
        section: "Account",
      },
    ];
  }

  const items: NavItemConfig[] = [
    {
      label: "Organization Profile",
      href: "/app/organization",
      icon: Building2,
      section: "Account",
    },
    {
      label: "Settings",
      href: "/app/settings",
      icon: Settings,
      section: "Account",
    },
  ];

  if (referralsEnabled) {
    items.push({
      label: "Referrals",
      href: "/app/referrals",
      icon: Gift,
      section: "Account",
    });
  }

  return items;
};

export const getPageContext = (pathname: string, role: AppRole | null | undefined) => {
  if (pathname.startsWith("/app/members/") && pathname.endsWith("/renew")) {
    return {
      eyebrow: "Renewal Desk",
      title: "Membership Renewal",
      description: "Renew an existing member without mixing profile edits into the billing workflow.",
    };
  }

  if (pathname.startsWith("/app/members/") && pathname.endsWith("/edit")) {
    return {
      eyebrow: "Member Workspace",
      title: "Edit Member",
      description: "Review and update only the selected member profile details.",
    };
  }

  const directMatches: Array<{ match: string; eyebrow: string; title: string; description: string }> = [
    {
      match: "/app/platform-reports",
      eyebrow: "Platform Reports",
      title: "Reports",
      description: "Year-wise organization joins and revenue in one monthly platform report.",
    },
    {
      match: "/app/platform-overview",
      eyebrow: "Platform Command",
      title: "Platform Overview",
      description: "Cross-organization visibility for growth, health, and system activity.",
    },
    {
      match: "/app/organizations/new",
      eyebrow: "Provisioning",
      title: "Create Organization",
      description: "Set up a new tenant workspace and first admin account.",
    },
    {
      match: "/app/organizations",
      eyebrow: "Platform Directory",
      title: "Organizations",
      description: "Manage tenants, ownership, and platform rollout from one place.",
    },
    {
      match: "/app/audit-logs",
      eyebrow: "Audit Command",
      title: "Audit Logs",
      description: "Review the latest organization-admin login activity from one dedicated audit log.",
    },
    {
      match: "/app/dashboard",
      eyebrow: "Revenue Command",
      title: "Dashboard",
      description: "Live business snapshot across memberships, collections, and reminders.",
    },
    {
      match: "/app/members/new",
      eyebrow: "Member Operations",
      title: "Create Member",
      description: "Register a member identity and start a new membership lifecycle cleanly.",
    },
    {
      match: "/app/members",
      eyebrow: "Member Operations",
      title: "Members",
      description: "Search, review, and update your member directory with payment-aware history.",
    },
    {
      match: "/app/reports",
      eyebrow: "Financial Reports",
      title: "Reports",
      description: "Month-wise revenue, admissions, dues, and export-ready financial summaries.",
    },
      {
        match: "/app/reminders",
        eyebrow: "Renewal Monitoring",
        title: "Reminders",
        description: "Track expired and overdue memberships before revenue slips through.",
      },
      {
        match: "/app/subscription",
        eyebrow: "SaaS Billing",
      title: "Subscription",
      description: "Review your BillMyGym access status, usage, billing period, and payment ledger.",
    },
    {
      match: "/app/settings",
      eyebrow: "BillMyGym Controls",
      title: "Settings",
      description: "Configure reminder windows, branding, and org-level operational defaults.",
    },
    {
      match: "/app/organization",
      eyebrow: "Workspace Identity",
      title: "Organization Profile",
      description: "Maintain the business details, contacts, and visible org profile data.",
    },
    {
      match: "/app/referrals/analytics",
      eyebrow: "Platform Command",
      title: "Referral Analytics",
      description: "Monitor platform-wide referral signups and distributed rewards.",
    },
    {
      match: "/app/referrals",
      eyebrow: "Growth",
      title: "Referrals",
      description: "Refer new organizations to GymFlow and earn subscription rewards.",
    },
  ];

  const exact = directMatches.find((item) => pathname.startsWith(item.match));
  if (exact) {
    return exact;
  }

  return role === "SUPER_ADMIN"
    ? {
        eyebrow: "Platform Command",
        title: "BillMyGym",
        description: "Manage tenants and monitor platform operations from a single command surface.",
      }
    : {
        eyebrow: "Operations Hub",
        title: "BillMyGym",
        description: "Run your membership, billing, and renewal workflow from one focused workspace.",
      };
};

