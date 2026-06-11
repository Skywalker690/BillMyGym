/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";
import AppLayout from "./components/layout/AppLayout";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "sonner";

import Login from "./pages/Login";
import Signup from "./pages/Signup";
import ForgotPassword from "./pages/ForgotPassword";
import Unauthorized from "./pages/Unauthorized";
import NotFound from "./pages/NotFound";
import ResetPassword from "./pages/ResetPassword";
import Consent from "./pages/Consent";
import TermsAndConditions from "./pages/TermsAndConditions";
import PrivacyPolicy from "./pages/PrivacyPolicy";

import Dashboard from "./pages/org/Dashboard";
import MembersList from "./pages/org/MembersList";
import MemberForm from "./pages/org/MemberForm";
import Reminders from "./pages/org/Reminders";

import Settings from "./pages/org/Settings";
import OrganizationProfile from "./pages/org/OrganizationProfile";
import Subscription from "./pages/org/Subscription";
import Reports from "./pages/org/Reports";

// SUPER_ADMIN pages
import PlatformOverview from "./pages/admin/PlatformOverview";
import PlatformReports from "./pages/admin/PlatformReports";
import OrganizationsList from "./pages/admin/OrganizationsList";
import OrganizationForm from "./pages/admin/OrganizationForm";
import OrganizationDetail from "./pages/admin/OrganizationDetail";
import GlobalAuditLogs from "./pages/admin/GlobalAuditLogs";
import OrganizationAuditLogs from "./pages/admin/OrganizationAuditLogs";
import AdminReferralAnalytics from "./pages/admin/AdminReferralAnalytics";
import DemoManagement from "./pages/admin/DemoManagement";

import ReferralDashboard from "./pages/org/ReferralDashboard";

const queryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/unauthorized" element={<Unauthorized />} />
            <Route path="/terms" element={<TermsAndConditions />} />
            <Route path="/privacy" element={<PrivacyPolicy />} />

            <Route element={<ProtectedRoute allowedRoles={["ORG_ADMIN", "SUPER_ADMIN"]} allowPendingConsent />}>
              <Route path="/consent" element={<Consent />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={["ORG_ADMIN"]} />}>
              <Route path="/app/subscription/recovery" element={<Subscription immersive />} />
              <Route path="/app/subscription/recovery/plans" element={<Subscription immersive showPlans />} />
            </Route>

            <Route element={<AppLayout />}>
              {/* SUPER_ADMIN Routes */}
              <Route element={<ProtectedRoute allowedRoles={["SUPER_ADMIN"]} />}>
                <Route path="/app/platform-overview" element={<PlatformOverview />} />
                <Route path="/app/platform-reports" element={<PlatformReports />} />
                <Route path="/app/organizations" element={<OrganizationsList />} />
                <Route path="/app/organizations/new" element={<OrganizationForm />} />
                <Route path="/app/organizations/:id" element={<OrganizationDetail />} />
                <Route path="/app/organizations/:id/audit-logs" element={<OrganizationAuditLogs />} />
                <Route path="/app/audit-logs" element={<GlobalAuditLogs />} />
                <Route path="/app/referrals/analytics" element={<AdminReferralAnalytics />} />
                <Route path="/app/demo-management" element={<DemoManagement />} />
              </Route>

              {/* ORG_ADMIN Routes */}
              <Route element={<ProtectedRoute allowedRoles={["ORG_ADMIN"]} />}>
                <Route path="/app/dashboard" element={<Dashboard />} />
                <Route path="/app/members" element={<MembersList />} />
                <Route path="/app/members/new" element={<MemberForm mode="create" />} />
                <Route path="/app/members/:id/edit" element={<MemberForm mode="edit" />} />
                <Route path="/app/members/:id/renew" element={<MemberForm mode="renew" />} />
                <Route path="/app/reminders" element={<Reminders />} />

                <Route path="/app/reports" element={<Reports />} />
                <Route path="/app/referrals" element={<ReferralDashboard />} />
                <Route path="/app/subscription" element={<Subscription />} />
                <Route path="/app/subscription/plans" element={<Subscription showPlans />} />
                <Route path="/app/settings" element={<Settings />} />
                <Route path="/app/organization" element={<OrganizationProfile />} />
              </Route>
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
        <Toaster position="top-right" richColors />
      </AuthProvider>
    </QueryClientProvider>
  );
}
