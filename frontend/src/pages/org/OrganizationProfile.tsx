import { useEffect, useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Building, KeyRound, Mail, Phone, ShieldCheck, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";

type PasswordFieldName = "currentPassword" | "newPassword" | "confirmPassword" | "otp";

export default function OrganizationProfile() {
  const { isDemoWorkspace } = useDemoPlanPreview();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [passwordOtpSent, setPasswordOtpSent] = useState(false);
  const [passwordOtpVerified, setPasswordOtpVerified] = useState(false);

  const { data: organization, isLoading } = useQuery({
    queryKey: ["orgProfile", user?.organizationId],
    queryFn: async () => {
      if (user?.organizationId) {
        const res = await api.get(`/organizations/${user.organizationId}`);
        return res.data;
      }
      const res = await api.get("/organizations");
      return res.data[0] ?? null;
    },
  });

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm();
  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    watch: watchPassword,
    reset: resetPasswordForm,
    setError: setPasswordError,
    clearErrors: clearPasswordErrors,
    formState: { errors: passwordErrors, isSubmitting: isPasswordSubmitting },
  } = useForm({
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
      otp: "",
    },
  });

  const applyPasswordRequestErrors = (error: any) => {
    const message = error?.response?.data?.message || "Failed to send password verification code.";
    const fieldErrors = Array.isArray(error?.response?.data?.errors) ? error.response.data.errors : [];

    for (const fieldError of fieldErrors) {
      const fieldName = fieldError?.field as PasswordFieldName | undefined;
      const fieldMessage = fieldError?.message as string | undefined;
      if (fieldName && fieldMessage) {
        setPasswordError(fieldName, { message: fieldMessage });
      }
    }

    if (!fieldErrors.length) {
      const normalizedMessage = String(message).toLowerCase();
      if (normalizedMessage.includes("current password")) {
        setPasswordError("currentPassword", { message });
      } else if (normalizedMessage.includes("confirm password")) {
        setPasswordError("confirmPassword", { message });
      } else if (normalizedMessage.includes("new password")) {
        setPasswordError("newPassword", { message });
      }
    }

    toast.error(message);
  };

  useEffect(() => {
    if (organization) {
      reset({
        adminName: organization.adminFullName || "",
        adminPhone: formatIndianPhone(organization.adminPhoneNumber || ""),
        adminEmail: organization.adminEmail || "",
        monthlyMembershipFee: organization.monthlyMembershipFee || 0,
        admissionFee: organization.admissionFee || 0,
      });
    }
  }, [organization, reset]);

  const mutation = useMutation({
    mutationFn: (data: any) => api.patch(`/organizations/${organization.id}`, {
      name: organization.name,
      district: organization.district,
      logoUrl: organization.logoUrl,
      mapLocationLink: organization.mapLocationLink,
      address: organization.address,
      adminFullName: isDemoWorkspace ? data.adminName : organization.adminFullName,
      adminPhoneNumber: isDemoWorkspace ? normalizeIndianPhone(data.adminPhone) : organization.adminPhoneNumber,
      adminEmail: organization.adminEmail,
      monthlyMembershipFee: organization.monthlyMembershipFee,
      admissionFee: organization.admissionFee,
      status: organization.status,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgProfile"] });
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization profile updated.");
    },
    onError: () => toast.error("Failed to update profile"),
  });

  const requestPasswordOtpMutation = useMutation({
    mutationFn: (data: any) => api.post("/auth/password/request-otp", data),
    onSuccess: () => {
      setPasswordOtpSent(true);
      setPasswordOtpVerified(false);
      clearPasswordErrors(["currentPassword", "newPassword", "confirmPassword", "otp"]);
      toast.success("A verification code has been sent to your email.");
    },
    onError: (error: any) => {
      setPasswordOtpSent(false);
      setPasswordOtpVerified(false);
      applyPasswordRequestErrors(error);
    },
  });

  const verifyPasswordOtpMutation = useMutation({
    mutationFn: async (otp: string) => {
      const response = await api.post("/auth/verify-otp", {
        email: user?.email,
        otp,
      });
      const payload = response.data;
      if (payload && typeof payload === "object" && "success" in payload && payload.success === false) {
        throw new Error((payload as { message?: string }).message || "Invalid or expired OTP");
      }
      return true;
    },
    onSuccess: () => {
      setPasswordOtpVerified(true);
      clearPasswordErrors("otp");
      toast.success("Code verified. You can now update your password.");
    },
    onError: (error: any) => {
      setPasswordOtpVerified(false);
      setPasswordError("otp", { message: error?.response?.data?.message || "Invalid or expired OTP" });
    },
  });

  const confirmPasswordMutation = useMutation({
    mutationFn: (otp: string) => api.post("/auth/password/confirm", {
      otp,
      newPassword: watchPassword("newPassword"),
    }),
    onSuccess: () => {
      toast.success("Password updated successfully.");
      setPasswordOtpSent(false);
      setPasswordOtpVerified(false);
      resetPasswordForm();
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to update password.");
    },
  });

  const handleVerifyPasswordOtp = handlePasswordSubmit(async (data) => {
    try {
      setPasswordOtpVerified(false);
      await verifyPasswordOtpMutation.mutateAsync(data.otp);
    } catch {
      // Error presentation is handled by the mutation onError callback.
    }
  });

  if (isLoading) {
    return <div className="p-8">Loading profile...</div>;
  }

  if (!organization) {
    return <div className="p-8">No organization found.</div>;
  }

  return (
    <div className="mx-auto max-w-6xl space-y-8 p-8">
      <section className="billmygym-shell rounded-[2rem] p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex h-16 w-16 items-center justify-center rounded-[1.6rem] bg-[linear-gradient(135deg,#e7f6ee_0%,#fff7ed_100%)] text-emerald-700 shadow-sm">
            <Building className="h-8 w-8" />
          </div>
          <div className="flex-1">
            <div className="text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Workspace Identity</div>
            <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">{organization.name}</h1>
            <p className="mt-2 text-sm font-medium text-slate-600">
              {isDemoWorkspace
                ? "Update the demo workspace contact number used during live walkthroughs and manual reminder showcases."
                : "Maintain your public gym identity, primary admin contact, and billing-facing organization details."}
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3 lg:min-w-[420px]">
            <IdentityCard label="Organization ID" value={`ORG-${organization.id}`} tone="slate" />
            <IdentityCard label="Workspace Type" value={organization.organizationType ?? "-"} tone="emerald" />
            <IdentityCard label="Workspace Status" value={organization.status ?? "-"} tone="amber" />
          </div>
        </div>
      </section>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-6">
            <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
              <User className="h-5 w-5 text-emerald-700" />
              Primary Contact
            </h2>
            {isDemoWorkspace && (
              <p className="mt-2 text-sm font-semibold text-sky-700">
                Demo gyms can update the primary contact used for manual reminder showcases.
              </p>
            )}
          </div>
          <div className="grid gap-6 p-6 md:grid-cols-2">
            <Field label="Admin Full Name" icon={<User className="h-4 w-4" />}>
              <Input
                readOnly={!isDemoWorkspace}
                {...register("adminName")}
                className={`h-11 rounded-xl pl-10 ${isDemoWorkspace ? "bg-white/80 text-slate-900" : "bg-slate-50/80 text-slate-600"}`}
              />
            </Field>
            <Field label="Admin Email" icon={<Mail className="h-4 w-4" />}>
              <Input readOnly type="email" {...register("adminEmail")} className="h-11 rounded-xl bg-slate-50/80 pl-10 text-slate-600" />
            </Field>
            <Field label="Admin Phone Number" icon={<Phone className="h-4 w-4" />}>
              <Input
                readOnly={!isDemoWorkspace}
                {...register("adminPhone")}
                className={`h-11 rounded-xl pl-10 ${isDemoWorkspace ? "bg-white/80 text-slate-900" : "bg-slate-50/80 text-slate-600"}`}
              />
            </Field>
            <Field label="Monthly Membership Fee (INR)">
              <Input readOnly type="number" {...register("monthlyMembershipFee", { valueAsNumber: true })} className="h-11 rounded-xl bg-slate-50/80 text-slate-600" />
            </Field>
            <Field label="Admission Fee (INR)">
              <Input readOnly type="number" {...register("admissionFee", { valueAsNumber: true })} className="h-11 rounded-xl bg-slate-50/80 text-slate-600" />
            </Field>
          </div>
        </div>

        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-6">
            <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
              <KeyRound className="h-5 w-5 text-emerald-700" />
              Password & Security
            </h2>
          </div>
          <div className="grid gap-6 p-6 xl:grid-cols-[1.35fr_0.9fr]">
            <div className="space-y-5">
              <div className="grid gap-5 md:grid-cols-2">
                <SecureField label="Current Password" error={passwordErrors.currentPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("currentPassword", { required: "Current password is required" })} className="h-11 rounded-xl bg-white/60" />
                </SecureField>
                <div />
                <SecureField label="New Password" error={passwordErrors.newPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("newPassword", { required: "New password is required", minLength: { value: 8, message: "Password must be at least 8 characters" } })} className="h-11 rounded-xl bg-white/60" />
                </SecureField>
                <SecureField label="Confirm New Password" error={passwordErrors.confirmPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("confirmPassword", { required: "Confirm password is required" })} className="h-11 rounded-xl bg-white/60" />
                </SecureField>
              </div>

              <div className="flex flex-wrap gap-3">
                <Button
                  type="button"
                  disabled={requestPasswordOtpMutation.isPending || isPasswordSubmitting}
                  onClick={handlePasswordSubmit((data) => {
                    if (data.newPassword !== data.confirmPassword) {
                      setPasswordError("confirmPassword", { message: "Passwords do not match" });
                      return;
                    }
                    clearPasswordErrors(["currentPassword", "newPassword", "confirmPassword", "otp"]);
                    requestPasswordOtpMutation.mutate({
                      currentPassword: data.currentPassword,
                      newPassword: data.newPassword,
                      confirmPassword: data.confirmPassword,
                    });
                  })}
                  className="rounded-full bg-emerald-600 px-6 text-white hover:bg-emerald-700"
                >
                  {requestPasswordOtpMutation.isPending ? "Sending code..." : "Send Verification Code"}
                </Button>
                {passwordOtpSent && (
                  <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700">
                    Code sent to {user?.email}
                  </span>
                )}
              </div>

              {passwordOtpSent && (
                <div className="rounded-[1.5rem] border border-amber-200 bg-amber-50/70 p-5">
                  <div className="grid gap-3 md:grid-cols-[1fr_auto_auto] md:items-end">
                    <SecureField label="Verification Code" error={passwordErrors.otp?.message as string | undefined}>
                      <Input id="otp" {...registerPassword("otp", { required: "OTP is required", minLength: { value: 6, message: "OTP must be 6 digits" }, maxLength: { value: 6, message: "OTP must be 6 digits" } })} className="h-11 rounded-xl bg-white/80" />
                    </SecureField>
                    <Button type="button" variant="outline" onClick={handleVerifyPasswordOtp} disabled={verifyPasswordOtpMutation.isPending} className="h-11 rounded-full">
                      {verifyPasswordOtpMutation.isPending ? "Verifying..." : "Verify OTP"}
                    </Button>
                    <Button type="button" disabled={!passwordOtpVerified || confirmPasswordMutation.isPending} onClick={handlePasswordSubmit((data) => confirmPasswordMutation.mutate(data.otp))} className="h-11 rounded-full bg-slate-950 text-white hover:bg-slate-800">
                      {confirmPasswordMutation.isPending ? "Updating..." : "Update Password"}
                    </Button>
                  </div>
                </div>
              )}
            </div>

            <div className="rounded-[1.75rem] border border-emerald-100 bg-[linear-gradient(180deg,#f8fcfa_0%,#eef4f0_100%)] p-6 shadow-sm">
              <div className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-3 py-1 text-[11px] font-black uppercase tracking-[0.2em] text-emerald-700">
                <ShieldCheck className="h-4 w-4" />
                Security Flow
              </div>
              <div className="mt-5 space-y-4 text-sm text-slate-600">
                <p className="font-semibold text-slate-900">Password changes are protected by one-time email verification.</p>
                <p>1. Enter your current password and the replacement password.</p>
                <p>2. We send a one-time code to the admin inbox.</p>
                <p>3. Verify the code and complete the update.</p>
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-2">
          {isDemoWorkspace ? (
            <Button type="submit" disabled={isSubmitting} className="h-12 rounded-full bg-slate-950 px-8 font-bold text-white hover:bg-slate-800">
              {isSubmitting ? "Saving..." : "Save Demo Contact"}
            </Button>
          ) : (
            <div className="rounded-full border border-slate-200 bg-slate-50 px-5 py-3 text-sm font-semibold text-slate-500">
              Contact details are managed by platform admin for regular workspaces.
            </div>
          )}
        </div>
      </form>
    </div>
  );
}

function Field({ label, icon, children }: { label: string; icon?: ReactNode; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700">{label}</Label>
      <div className="relative">
        {icon && <div className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">{icon}</div>}
        {children}
      </div>
    </div>
  );
}

function SecureField({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700">{label}</Label>
      {children}
      {error && <p className="text-sm font-medium text-red-500">{error}</p>}
    </div>
  );
}

function formatIndianPhone(phone: string) {
  return phone.replace(/^\+?91\s?/, "").trim();
}

function normalizeIndianPhone(phone: string) {
  const trimmed = phone.trim();
  if (!trimmed) return trimmed;
  const digits = trimmed.replace(/[^\d]/g, "");
  if (digits.length === 10) {
    return `+91${digits}`;
  }
  if (digits.length === 12 && digits.startsWith("91")) {
    return `+${digits}`;
  }
  return trimmed.startsWith("+") ? trimmed : `+${trimmed}`;
}

function IdentityCard({ label, value, tone }: { label: string; value: string; tone: "slate" | "emerald" | "amber" }) {
  const toneClass = {
    slate: "border-slate-200 bg-white/80 text-slate-900",
    emerald: "border-emerald-100 bg-emerald-50/80 text-emerald-900",
    amber: "border-amber-100 bg-amber-50/80 text-amber-900",
  }[tone];

  return (
    <div className={`rounded-[1.35rem] border p-4 shadow-sm ${toneClass}`}>
      <div className="text-[10px] font-black uppercase tracking-[0.22em] opacity-70">{label}</div>
      <div className="mt-2 text-lg font-black">{value}</div>
    </div>
  );
}
