import { createContext, useContext, useMemo, type ReactNode } from "react";

export type DemoPreviewPlan = "BILLMYGYM";

type PlanAccess = {
  canCreateMembers?: boolean;
  canViewAdvancedAnalytics?: boolean;
  canReceiveMonthlyReports?: boolean;
  canExportReports?: boolean;
  canUsePremiumFeatures?: boolean;
};

type DemoPlanPreviewContextValue = {
  isDemoWorkspace: boolean;
  previewPlan: DemoPreviewPlan;
  setPreviewPlan: (plan: DemoPreviewPlan) => void;
  effectivePlan: DemoPreviewPlan;
  effectivePlanAccess: (planAccess?: PlanAccess | null) => PlanAccess;
};

const defaultPlanAccess = (planAccess?: PlanAccess | null): PlanAccess => ({
  canCreateMembers: Boolean(planAccess?.canCreateMembers ?? true),
  canViewAdvancedAnalytics: Boolean(planAccess?.canViewAdvancedAnalytics ?? true),
  canReceiveMonthlyReports: Boolean(planAccess?.canReceiveMonthlyReports ?? true),
  canExportReports: Boolean(planAccess?.canExportReports ?? true),
  canUsePremiumFeatures: Boolean(planAccess?.canUsePremiumFeatures ?? true),
});

const fallbackContextValue: DemoPlanPreviewContextValue = {
  isDemoWorkspace: false,
  previewPlan: "BILLMYGYM",
  setPreviewPlan: () => {},
  effectivePlan: "BILLMYGYM",
  effectivePlanAccess: defaultPlanAccess,
};

const DemoPlanPreviewContext = createContext<DemoPlanPreviewContextValue | undefined>(undefined);

export function DemoPlanPreviewProvider({
  isDemoWorkspace,
  children,
}: {
  isDemoWorkspace: boolean;
  children: ReactNode;
}) {
  const value = useMemo<DemoPlanPreviewContextValue>(() => {
    return {
      isDemoWorkspace,
      previewPlan: "BILLMYGYM",
      setPreviewPlan: () => {},
      effectivePlan: "BILLMYGYM",
      effectivePlanAccess: defaultPlanAccess,
    };
  }, [isDemoWorkspace]);

  return <DemoPlanPreviewContext.Provider value={value}>{children}</DemoPlanPreviewContext.Provider>;
}

export function useDemoPlanPreview() {
  const context = useContext(DemoPlanPreviewContext);
  return context ?? fallbackContextValue;
}
