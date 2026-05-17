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

// Lazy load or import pages
import Login from "./pages/Login";
import ForgotPassword from "./pages/ForgotPassword";
import Unauthorized from "./pages/Unauthorized";
import NotFound from "./pages/NotFound";
import ResetPassword from "./pages/ResetPassword";

// ORG_ADMIN pages
import Dashboard from "./pages/org/Dashboard";
import MembersList from "./pages/org/MembersList";
import MemberForm from "./pages/org/MemberForm";
import Reminders from "./pages/org/Reminders";
import Notifications from "./pages/org/Notifications";
import Settings from "./pages/org/Settings";
import OrganizationProfile from "./pages/org/OrganizationProfile";

// SUPER_ADMIN pages
import PlatformOverview from "./pages/admin/PlatformOverview";
import OrganizationsList from "./pages/admin/OrganizationsList";
import OrganizationForm from "./pages/admin/OrganizationForm";
import OrganizationDetail from "./pages/admin/OrganizationDetail";

const queryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/unauthorized" element={<Unauthorized />} />

            <Route element={<AppLayout />}>
              {/* SUPER_ADMIN Routes */}
              <Route element={<ProtectedRoute allowedRoles={["SUPER_ADMIN"]} />}>
                <Route path="/app/platform-overview" element={<PlatformOverview />} />
                <Route path="/app/organizations" element={<OrganizationsList />} />
                <Route path="/app/organizations/new" element={<OrganizationForm />} />
                <Route path="/app/organizations/:id" element={<OrganizationDetail />} />
              </Route>

              {/* ORG_ADMIN Routes */}
              <Route element={<ProtectedRoute allowedRoles={["ORG_ADMIN"]} />}>
                <Route path="/app/dashboard" element={<Dashboard />} />
                <Route path="/app/members" element={<MembersList />} />
                <Route path="/app/members/new" element={<MemberForm />} />
                <Route path="/app/members/:id/edit" element={<MemberForm isEdit />} />
                <Route path="/app/reminders" element={<Reminders />} />
                <Route path="/app/notifications" element={<Notifications />} />
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
