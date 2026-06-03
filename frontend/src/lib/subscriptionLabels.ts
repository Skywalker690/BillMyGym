export type OrganizationStatus = "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
export type OrganizationType = "REGULAR" | "DEMO";
export type SubscriptionPlan = "BILLMYGYM";

export function organizationStatusLabel(status: OrganizationStatus): string {
  switch (status) {
    case "TRIAL":
      return "Free Trial Access";
    case "ACTIVE":
      return "Active Subscription";
    case "PAUSED":
      return "Subscription Renewal Required";
    case "DEACTIVATED":
      return "Account Inactive";
  }
}

export function organizationTypeLabel(type?: OrganizationType): string {
  if (type === "DEMO") return "Demo Workspace";
  return "Regular Workspace";
}

export function subscriptionPlanLabel(plan?: SubscriptionPlan): string {
  return "BillMyGym";
}

export function organizationStatusDescription(status: OrganizationStatus): string {
  switch (status) {
    case "TRIAL":
      return "Your workspace is currently in free trial mode.";
    case "ACTIVE":
      return "Your paid subscription is active and in good standing.";
    case "PAUSED":
      return "Your subscription needs renewal to keep access uninterrupted.";
    case "DEACTIVATED":
      return "This account is inactive until billing is restored.";
  }
}

export function foundingPartnerDescription(): string {
  return "Demo workspaces are meant for walkthroughs and showcases, with full workflow visibility, no billing, and a capped member sandbox.";
}

export function notificationTypeLabel(type: string): string {
  switch (type) {
    case "TRIAL_ENDING":
      return "Free Trial Access";
    case "SUBSCRIPTION_REMINDER":
      return "Subscription Renewal Reminder";
    case "SUBSCRIPTION_PAUSED":
      return "Subscription Renewal Required";
    case "SUBSCRIPTION_DEACTIVATED":
      return "Account Inactive";
    case "SUBSCRIPTION_PAYMENT_SUCCESS":
      return "Active Subscription";
    case "ORGANIZATION_WELCOME":
      return "Welcome Message";
    case "INDIVIDUAL_WHATSAPP_REMINDER":
      return "Member WhatsApp Reminder";
    default:
      return type.replaceAll("_", " ").toLowerCase();
  }
}

export function notificationMessageLabel(message: string): string {
  return message
    .replaceAll("Organization Paused", "Subscription Renewal Required")
    .replaceAll("Organization paused", "Subscription Renewal Required")
    .replaceAll("Organization Deactivated", "Account Inactive")
    .replaceAll("Organization deactivated", "Account Inactive")
    .replaceAll("Demo Access Ends", "Access Ends")
    .replaceAll("Trial Ends", "Free Trial Access Ends");
}
