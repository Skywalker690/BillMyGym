export type OrganizationStatus = "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
export type OrganizationType = "REGULAR" | "PILOT" | "DEMO";
export type SubscriptionPlan = "STARTER" | "GROWTH" | "PRO" | "PILOT";

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
  if (type === "PILOT") return "Pilot";
  if (type === "DEMO") return "Demo";
  return "Standard Billing";
}

export function subscriptionPlanLabel(plan?: SubscriptionPlan): string {
  if (!plan) return "Starter";
  if (plan === "PILOT") return "Pilot";
  return plan.charAt(0) + plan.slice(1).toLowerCase();
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
  return "Pilot access has full features at Rs. 299/month. Demo keeps full features with a 3-member limit and no subscription fee.";
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
    .replaceAll("Pilot Access Ends", "Access Ends")
    .replaceAll("Trial Ends", "Free Trial Access Ends");
}
