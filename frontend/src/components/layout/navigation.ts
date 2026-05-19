import {
  Activity,
  Bell,
  Building2,
  Clock3,
  CreditCard,
  LayoutDashboard,
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

export const getSidebarNavItems = (role: AppRole | null | undefined): NavItemConfig[] => {
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
    ];
  }

  return [
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
      label: "Notifications",
      href: "/app/notifications",
      icon: Bell,
      section: "Operations",
    },
    {
      label: "Subscription",
      href: "/app/subscription",
      icon: CreditCard,
      section: "Billing",
    },
  ];
};

export const getProfileNavItems = (role: AppRole | null | undefined): NavItemConfig[] => {
  if (role === "SUPER_ADMIN") {
    return [
      {
        label: "Organizations",
        href: "/app/organizations",
        icon: Building2,
        section: "Account",
      },
    ];
  }

  return [
    {
      label: "Organization Profile",
      href: "/app/organization",
      icon: Building2,
      section: "Account",
    },
    {
      label: "Subscription",
      href: "/app/subscription",
      icon: CreditCard,
      section: "Account",
    },
    {
      label: "Settings",
      href: "/app/settings",
      icon: Settings,
      section: "Account",
    },
  ];
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
      match: "/app/reminders",
      eyebrow: "Renewal Monitoring",
      title: "Reminders",
      description: "Track expired and overdue memberships before revenue slips through.",
    },
    {
      match: "/app/notifications",
      eyebrow: "Admin Inbox",
      title: "Notifications",
      description: "Review digests and system notices delivered to organization admins.",
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

