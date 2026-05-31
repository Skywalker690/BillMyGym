import { useMemo, useState } from "react";
import { ExternalLink, FileCheck2, ShieldCheck } from "lucide-react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { toast } from "sonner";

export default function Consent() {
  const navigate = useNavigate();
  const { isAuthenticated, user, legalPolicy, consentRequired, consentMessage, updateUser } = useAuth();
  const [termsChecked, setTermsChecked] = useState(false);
  const [privacyChecked, setPrivacyChecked] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const dashboardPath = useMemo(
    () => (user?.role === "SUPER_ADMIN" ? "/app/platform-overview" : "/app/dashboard"),
    [user?.role]
  );

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!consentRequired) {
    return <Navigate to={dashboardPath} replace />;
  }

  const handleAccept = async () => {
    if (!legalPolicy) {
      toast.error("Legal policy details are still loading. Please try again.");
      return;
    }
    setIsSubmitting(true);
    try {
      const response = await api.post("/auth/consent", {
        termsAccepted: true,
        privacyAccepted: true,
        version: legalPolicy.currentVersion,
      });
      updateUser(response.data);
      toast.success("Legal consent recorded successfully.");
      navigate(dashboardPath, { replace: true });
    } catch (error: any) {
      const message = error?.response?.data?.message || "Could not record your consent right now.";
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f7faf8_0%,#eef5f1_100%)] px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto flex min-h-[calc(100vh-3rem)] max-w-3xl items-center">
        <div className="w-full rounded-3xl border border-emerald-100 bg-white shadow-[0_24px_80px_rgba(15,23,42,0.08)]">
          <div className="border-b border-slate-200 px-6 py-6 sm:px-8">
            <div className="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
              <ShieldCheck className="h-4 w-4" />
              Mandatory legal consent
            </div>
            <h1 className="mt-4 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">Before Continuing</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">{consentMessage}</p>
          </div>

          <div className="px-6 py-6 sm:px-8">
            <div className="grid gap-4 sm:grid-cols-2">
              <Link
                to="/terms"
                target="_blank"
                rel="noreferrer"
                className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-emerald-300 hover:bg-emerald-50"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold text-slate-900">View Terms & Conditions</div>
                    <div className="mt-1 text-xs font-medium text-slate-500">Review the rules and responsibilities for using BillMyGym.</div>
                  </div>
                  <ExternalLink className="h-4 w-4 text-slate-400" />
                </div>
              </Link>

              <Link
                to="/privacy"
                target="_blank"
                rel="noreferrer"
                className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 transition hover:border-emerald-300 hover:bg-emerald-50"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-bold text-slate-900">View Privacy Policy</div>
                    <div className="mt-1 text-xs font-medium text-slate-500">Understand what data we collect, why we use it, and how it is protected.</div>
                  </div>
                  <ExternalLink className="h-4 w-4 text-slate-400" />
                </div>
              </Link>
            </div>

            <div className="mt-8 space-y-4 rounded-2xl border border-slate-200 bg-slate-50 px-5 py-5">
              <label className="flex items-start gap-3">
                <input
                  type="checkbox"
                  checked={termsChecked}
                  onChange={(event) => setTermsChecked(event.target.checked)}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                />
                <span className="text-sm font-medium text-slate-700">I have read and agree to the Terms & Conditions</span>
              </label>

              <label className="flex items-start gap-3">
                <input
                  type="checkbox"
                  checked={privacyChecked}
                  onChange={(event) => setPrivacyChecked(event.target.checked)}
                  className="mt-1 h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                />
                <span className="text-sm font-medium text-slate-700">I have read and agree to the Privacy Policy</span>
              </label>
            </div>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-2 text-xs font-medium text-slate-500">
                <FileCheck2 className="h-4 w-4 text-emerald-600" />
                {legalPolicy ? `Version ${legalPolicy.currentVersion} effective ${legalPolicy.effectiveDate}` : "Loading legal policy details"}
              </div>
              <Button
                type="button"
                disabled={!termsChecked || !privacyChecked || isSubmitting || !legalPolicy}
                onClick={handleAccept}
                className="h-11 rounded-full bg-emerald-600 px-6 font-bold text-white hover:bg-emerald-500"
              >
                {isSubmitting ? "Saving..." : "Accept & Continue"}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
