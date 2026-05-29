import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

export type DemoPreviewPlan = "STARTER" | "GROWTH" | "PRO";

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

const STORAGE_KEY = "billmygym_demo_preview_plan";

const DemoPlanPreviewContext = createContext<DemoPlanPreviewContextValue | undefined>(undefined);

export function DemoPlanPreviewProvider({
  isDemoWorkspace,
  children,
}: {
  isDemoWorkspace: boolean;
  children: ReactNode;
}) {
  const [previewPlan, setPreviewPlanState] = useState<DemoPreviewPlan>("PRO");

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "STARTER" || stored === "GROWTH" || stored === "PRO") {
      setPreviewPlanState(stored);
    }
  }, []);

  const setPreviewPlan = (plan: DemoPreviewPlan) => {
    setPreviewPlanState(plan);
    window.localStorage.setItem(STORAGE_KEY, plan);
  };

  const value = useMemo<DemoPlanPreviewContextValue>(() => {
    const effectivePlan = isDemoWorkspace ? previewPlan : "PRO";
    return {
      isDemoWorkspace,
      previewPlan,
      setPreviewPlan,
      effectivePlan,
      effectivePlanAccess: (planAccess) => {
        if (!isDemoWorkspace) {
          return {
            canCreateMembers: Boolean(planAccess?.canCreateMembers ?? true),
            canViewAdvancedAnalytics: Boolean(planAccess?.canViewAdvancedAnalytics ?? true),
            canReceiveMonthlyReports: Boolean(planAccess?.canReceiveMonthlyReports ?? true),
            canExportReports: Boolean(planAccess?.canExportReports ?? true),
            canUsePremiumFeatures: Boolean(planAccess?.canUsePremiumFeatures ?? true),
          };
        }
        if (previewPlan === "STARTER") {
          return {
            canCreateMembers: true,
            canViewAdvancedAnalytics: false,
            canReceiveMonthlyReports: false,
            canExportReports: false,
            canUsePremiumFeatures: false,
          };
        }
        if (previewPlan === "GROWTH") {
          return {
            canCreateMembers: true,
            canViewAdvancedAnalytics: true,
            canReceiveMonthlyReports: true,
            canExportReports: false,
            canUsePremiumFeatures: false,
          };
        }
        return {
          canCreateMembers: true,
          canViewAdvancedAnalytics: true,
          canReceiveMonthlyReports: true,
          canExportReports: true,
          canUsePremiumFeatures: true,
        };
      },
    };
  }, [isDemoWorkspace, previewPlan]);

  return <DemoPlanPreviewContext.Provider value={value}>{children}</DemoPlanPreviewContext.Provider>;
}

export function useDemoPlanPreview() {
  const context = useContext(DemoPlanPreviewContext);
  if (!context) {
    throw new Error("useDemoPlanPreview must be used within a DemoPlanPreviewProvider");
  }
  return context;
}
