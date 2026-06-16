import { useEffect, useState, type ReactNode } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Building, KeyRound, Mail, Phone, ShieldCheck, User, LayoutList, Plus, Edit2, CheckCircle2, Wallet, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
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
    <div className="mx-auto max-w-6xl space-y-6 sm:space-y-8 p-3 sm:p-5 lg:p-8">
      <section className="billmygym-shell rounded-xl sm:rounded-2xl lg:rounded-[2rem] p-3 sm:p-5 lg:p-8">
        <div className="flex flex-col gap-4 sm:gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex h-12 sm:h-14 lg:h-16 w-12 sm:w-14 lg:w-16 items-center justify-center rounded-[1.2rem] sm:rounded-[1.4rem] lg:rounded-[1.6rem] bg-[linear-gradient(135deg,#e7f6ee_0%,#fff7ed_100%)] text-emerald-700 shadow-sm">
            <Building className="h-6 sm:h-7 lg:h-8 w-6 sm:w-7 lg:w-8" />
          </div>
          <div className="flex-1">
            <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Workspace Identity</div>
            <h1 className="mt-1 sm:mt-2 text-xl sm:text-2xl lg:text-4xl font-black tracking-tight text-slate-950">{organization.name}</h1>
          </div>
          <div className="grid grid-cols-3 gap-1.5 sm:gap-3 lg:min-w-[420px]">
            <IdentityCard label="Organization ID" value={`ORG-${organization.id}`} tone="slate" />
            <IdentityCard label="Workspace Type" value={organization.organizationType ?? "-"} tone="emerald" />
            <IdentityCard label="Workspace Status" value={organization.status ?? "-"} tone="amber" />
          </div>
        </div>
      </section>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
        <div className="glass-panel overflow-hidden rounded-xl sm:rounded-2xl lg:rounded-[1.9rem]">
          <div className="border-b border-white/70 p-3 sm:p-5 lg:p-6">
            <h2 className="flex items-center gap-2 sm:gap-3 text-lg sm:text-xl lg:text-2xl font-black tracking-tight text-slate-950">
              <User className="h-4 sm:h-5 w-4 sm:w-5 text-emerald-700" />
              Primary Contact
            </h2>
            {isDemoWorkspace && (
              <p className="mt-1.5 sm:mt-2 text-xs sm:text-sm font-semibold text-sky-700">
                Demo gyms can update the primary contact used for manual reminder showcases.
              </p>
            )}
          </div>
          <div className="grid gap-4 sm:gap-5 lg:gap-6 p-3 sm:p-5 lg:p-6 grid-cols-1 md:grid-cols-2">
            <Field label="Admin Full Name" icon={<User className="h-4 w-4" />}>
              <Input
                readOnly={!isDemoWorkspace}
                {...register("adminName")}
                className={`h-10 sm:h-11 rounded-lg sm:rounded-xl pl-9 sm:pl-10 text-sm ${isDemoWorkspace ? "bg-white/80 text-slate-900" : "bg-slate-50/80 text-slate-600"}`}
              />
            </Field>
            <Field label="Admin Email" icon={<Mail className="h-4 w-4" />}>
              <Input readOnly type="email" {...register("adminEmail")} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-slate-50/80 pl-9 sm:pl-10 text-sm text-slate-600" />
            </Field>
            <Field label="Admin Phone Number" icon={<Phone className="h-4 w-4" />}>
              <Input
                readOnly={!isDemoWorkspace}
                {...register("adminPhone")}
                className={`h-10 sm:h-11 rounded-lg sm:rounded-xl pl-9 sm:pl-10 text-sm ${isDemoWorkspace ? "bg-white/80 text-slate-900" : "bg-slate-50/80 text-slate-600"}`}
              />
            </Field>
            <Field label="Admission Fee (INR)">
              <Input readOnly type="number" {...register("admissionFee", { valueAsNumber: true })} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-slate-50/80 text-sm text-slate-600" />
            </Field>
          </div>
        </div>

        <div className="flex justify-end pt-1 sm:pt-2">
          {isDemoWorkspace ? (
            <Button type="submit" disabled={isSubmitting} className="h-10 sm:h-12 rounded-full bg-slate-950 px-6 sm:px-8 text-sm sm:text-base font-bold text-white hover:bg-slate-800">
              {isSubmitting ? "Saving..." : "Save Demo Contact"}
            </Button>
          ) : (
            <div className="rounded-full border border-slate-200 bg-slate-50 px-4 sm:px-5 py-2 sm:py-3 text-xs sm:text-sm font-semibold text-slate-500">
              Contact details are managed by platform admin for regular workspaces.
            </div>
          )}
        </div>
      </form>

      <MembershipPlansSection organizationId={organization.id} isDemoWorkspace={isDemoWorkspace} />

        <div className="glass-panel overflow-hidden rounded-xl sm:rounded-2xl lg:rounded-[1.9rem]">
          <div className="border-b border-white/70 p-3 sm:p-5 lg:p-6">
            <h2 className="flex items-center gap-2 sm:gap-3 text-lg sm:text-xl lg:text-2xl font-black tracking-tight text-slate-950">
              <KeyRound className="h-4 sm:h-5 w-4 sm:w-5 text-emerald-700" />
              Password & Security
            </h2>
          </div>
          <div className="grid gap-6 p-3 sm:p-5 lg:p-6 grid-cols-1 xl:grid-cols-[1.35fr_0.9fr]">
            <div className="space-y-4 sm:space-y-5">
              <div className="grid gap-4 sm:gap-5 grid-cols-1 md:grid-cols-2">
                <SecureField label="Current Password" error={passwordErrors.currentPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("currentPassword", { required: "Current password is required" })} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-white/60 text-sm" />
                </SecureField>
                <div />
                <SecureField label="New Password" error={passwordErrors.newPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("newPassword", { required: "New password is required", minLength: { value: 8, message: "Password must be at least 8 characters" } })} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-white/60 text-sm" />
                </SecureField>
                <SecureField label="Confirm New Password" error={passwordErrors.confirmPassword?.message as string | undefined}>
                  <Input type="password" {...registerPassword("confirmPassword", { required: "Confirm password is required" })} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-white/60 text-sm" />
                </SecureField>
              </div>

              <div className="flex flex-wrap gap-2 sm:gap-3">
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
                  className="h-10 sm:h-11 rounded-full bg-emerald-600 px-4 sm:px-6 text-sm sm:text-base text-white hover:bg-emerald-700"
                >
                  {requestPasswordOtpMutation.isPending ? "Sending code..." : "Send Verification Code"}
                </Button>
                {passwordOtpSent && (
                  <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 sm:px-4 py-1.5 sm:py-2 text-xs sm:text-sm font-semibold text-emerald-700">
                    Code sent to {user?.email}
                  </span>
                )}
              </div>

              {passwordOtpSent && (
                <div className="rounded-[1.2rem] sm:rounded-[1.5rem] border border-amber-200 bg-amber-50/70 p-3 sm:p-4 lg:p-5">
                  <div className="grid gap-2 sm:gap-3 grid-cols-1 md:grid-cols-[1fr_auto_auto] md:items-end">
                    <SecureField label="Verification Code" error={passwordErrors.otp?.message as string | undefined}>
                      <Input id="otp" {...registerPassword("otp", { required: "OTP is required", minLength: { value: 6, message: "OTP must be 6 digits" }, maxLength: { value: 6, message: "OTP must be 6 digits" } })} className="h-10 sm:h-11 rounded-lg sm:rounded-xl bg-white/80 text-sm" />
                    </SecureField>
                    <Button type="button" variant="outline" onClick={handleVerifyPasswordOtp} disabled={verifyPasswordOtpMutation.isPending} className="h-10 sm:h-11 rounded-full text-sm">
                      {verifyPasswordOtpMutation.isPending ? "Verifying..." : "Verify OTP"}
                    </Button>
                    <Button type="button" disabled={!passwordOtpVerified || confirmPasswordMutation.isPending} onClick={handlePasswordSubmit((data) => confirmPasswordMutation.mutate(data.otp))} className="h-10 sm:h-11 rounded-full bg-slate-950 px-4 sm:px-6 text-sm text-white hover:bg-slate-800">
                      {confirmPasswordMutation.isPending ? "Updating..." : "Update Password"}
                    </Button>
                  </div>
                </div>
              )}
            </div>

            <div className="rounded-[1.3rem] sm:rounded-[1.6rem] lg:rounded-[1.75rem] border border-emerald-100 bg-[linear-gradient(180deg,#f8fcfa_0%,#eef4f0_100%)] p-3 sm:p-5 lg:p-6 shadow-sm">
              <div className="inline-flex items-center gap-1.5 sm:gap-2 rounded-full bg-emerald-100 px-2.5 sm:px-3 py-1 text-[9px] sm:text-[11px] font-black uppercase tracking-[0.2em] text-emerald-700">
                <ShieldCheck className="h-3.5 sm:h-4 w-3.5 sm:w-4" />
                Security Flow
              </div>
              <div className="mt-3 sm:mt-4 lg:mt-5 space-y-2 sm:space-y-3 text-xs sm:text-sm text-slate-600">
                <p className="font-semibold text-slate-900">Password changes are protected by one-time email verification.</p>
                <p>1. Enter your current password and the replacement password.</p>
                <p>2. We send a one-time code to the admin inbox.</p>
                <p>3. Verify the code and complete the update.</p>
              </div>
            </div>
          </div>
        </div>    </div>
  );
}

function Field({ label, icon, children }: { label: string; icon?: ReactNode; children: ReactNode }) {
  return (
    <div className="space-y-1 sm:space-y-2">
      <Label className="text-xs sm:text-sm font-semibold text-slate-700">{label}</Label>
      <div className="relative">
        {icon && <div className="absolute left-2.5 sm:left-3 top-1/2 -translate-y-1/2 text-slate-400 h-4 w-4">{icon}</div>}
        {children}
      </div>
    </div>
  );
}

function SecureField({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-1 sm:space-y-2">
      <Label className="text-xs sm:text-sm font-semibold text-slate-700">{label}</Label>
      {children}
      {error && <p className="text-xs sm:text-sm font-medium text-red-500">{error}</p>}
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
    <div className={cn("rounded-lg sm:rounded-[1.2rem] lg:rounded-[1.35rem] border p-2.5 sm:p-3 lg:p-4 shadow-sm", toneClass)}>
      <div className="text-[8px] sm:text-[9px] lg:text-[10px] font-black uppercase tracking-[0.22em] opacity-70">{label}</div>
      <div className="mt-1 sm:mt-1.5 lg:mt-2 text-sm sm:text-base lg:text-lg font-black">{value}</div>
    </div>
  );
}

function MembershipPlansSection({ organizationId, isDemoWorkspace }: { organizationId: number; isDemoWorkspace: boolean }) {
  const queryClient = useQueryClient();
  const [editingPlan, setEditingPlan] = useState<any>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [deletingPlanId, setDeletingPlanId] = useState<number | null>(null);
  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm();

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ["orgMembershipPlans", organizationId],
    queryFn: async () => {
      const res = await api.get(`/organizations/${organizationId}/membership-plans`);
      return res.data?.plans || [];
    },
    enabled: !!organizationId,
  });

  const createMutation = useMutation({
    mutationFn: (data: any) => api.post(`/organizations/${organizationId}/membership-plans`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgMembershipPlans", organizationId] });
      toast.success("Membership plan created.");
      setIsCreating(false);
      reset();
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to create plan");
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: any) => api.patch(`/organizations/${organizationId}/membership-plans/${editingPlan.id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgMembershipPlans", organizationId] });
      toast.success("Membership plan updated.");
      setEditingPlan(null);
      reset();
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to update plan");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (planId: number) => api.delete(`/organizations/${organizationId}/membership-plans/${planId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgMembershipPlans", organizationId] });
      toast.success("Membership plan deleted.");
      setEditingPlan(null);
      reset();
      setDeletingPlanId(null);
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to delete plan");
    }
  });

  const confirmDelete = () => {
    if (deletingPlanId) {
      deleteMutation.mutate(deletingPlanId);
    }
  };

  const onSubmit = (data: any) => {
    if (editingPlan) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data);
    }
  };

  const handleEdit = (plan: any) => {
    setIsCreating(false);
    setEditingPlan(plan);
    reset({ name: plan.name, price: plan.price });
  };

  const cancelEdit = () => {
    setIsCreating(false);
    setEditingPlan(null);
    reset();
  };

  return (
    <div className="glass-panel overflow-hidden rounded-xl sm:rounded-2xl lg:rounded-[1.9rem]">
      <div className="border-b border-white/70 p-3 sm:p-5 lg:p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3 sm:gap-0">
        <h2 className="flex items-center gap-2 sm:gap-3 text-lg sm:text-xl lg:text-2xl font-black tracking-tight text-slate-950">
          <LayoutList className="h-4 sm:h-5 w-4 sm:w-5 text-emerald-700" />
          Membership Plans
        </h2>
        {!isDemoWorkspace && (
          <Button 
            type="button" 
            onClick={() => { setIsCreating(true); setEditingPlan(null); reset({ name: "", price: "" }); }}
            disabled={isCreating || !!editingPlan}
            className="h-8 rounded-full bg-emerald-600 px-4 text-xs font-bold text-white hover:bg-emerald-700"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" /> New Plan
          </Button>
        )}
      </div>
      <div className="p-3 sm:p-5 lg:p-6 space-y-4">
        {(isCreating || editingPlan) && (
          <form onSubmit={handleSubmit(onSubmit)} className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-4 sm:p-5 shadow-inner">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-black uppercase tracking-[0.1em] text-emerald-800">
                {isCreating ? "Create New Plan" : "Edit Plan"}
              </h3>
              <button type="button" onClick={cancelEdit} className="text-slate-400 hover:text-slate-600">
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="grid gap-4 sm:gap-5 grid-cols-1 md:grid-cols-2">
              <Field label="Plan Name" icon={<LayoutList className="h-4 w-4" />}>
                <Input 
                  {...register("name", { required: true })} 
                  placeholder="e.g. Premium Plan" 
                  className="h-10 sm:h-11 rounded-lg bg-white pl-9 sm:pl-10 text-sm" 
                />
              </Field>
              <Field label="Plan Price (INR)" icon={<Wallet className="h-4 w-4" />}>
                <Input 
                  type="number" 
                  {...register("price", { required: true, valueAsNumber: true })} 
                  placeholder="e.g. 1500" 
                  className="h-10 sm:h-11 rounded-lg bg-white pl-9 sm:pl-10 text-sm" 
                />
              </Field>
            </div>
            <div className="mt-5 flex items-center justify-end gap-3">
              {!isCreating && editingPlan && !editingPlan.defaultPlan && (
                <Button 
                  type="button" 
                  onClick={() => setDeletingPlanId(editingPlan.id)} 
                  disabled={deleteMutation.isPending || isSubmitting}
                  className="h-10 rounded-full bg-red-50 px-6 text-sm font-bold text-red-600 hover:bg-red-100 mr-auto"
                >
                  Delete Plan
                </Button>
              )}
              <Button type="submit" disabled={isSubmitting} className="h-10 rounded-full bg-slate-950 px-6 text-sm font-bold text-white hover:bg-slate-800">
                {isSubmitting ? "Saving..." : "Save Plan"}
              </Button>
            </div>
          </form>
        )}

        {isLoading ? (
          <div className="text-sm text-slate-500 py-4">Loading plans...</div>
        ) : plans.length === 0 ? (
          <div className="text-sm text-slate-500 py-4">No membership plans found.</div>
        ) : (
          <div className="grid gap-3 sm:gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
            {plans.map((plan: any) => (
              <div key={plan.id} className="relative flex flex-col rounded-[1.2rem] border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-2">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-black tracking-tight text-slate-900">{plan.name}</span>
                      {plan.defaultPlan && (
                        <span className="inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-[9px] font-black uppercase tracking-[0.1em] text-emerald-800">
                          Default
                        </span>
                      )}
                    </div>
                    <div className="text-xs font-semibold text-slate-500">
                      Status: <span className={plan.active ? "text-emerald-600" : "text-amber-600"}>{plan.active ? "Active" : "Inactive"}</span>
                    </div>
                  </div>
                  {!isDemoWorkspace && (
                    <button type="button" onClick={() => handleEdit(plan)} className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-50 text-slate-500 hover:bg-slate-100 hover:text-slate-900">
                      <Edit2 className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
                <div className="mt-4 mt-auto flex items-end justify-between">
                  <div>
                    <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Monthly Fee</div>
                    <div className="text-xl font-black tracking-tight text-slate-950">₹{plan.price}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {deletingPlanId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm transition-all duration-200">
          <div className="w-full max-w-sm scale-100 rounded-2xl bg-white p-6 shadow-xl ring-1 ring-slate-900/5 transition-all">
            <h3 className="text-lg font-black tracking-tight text-slate-900">Delete Plan?</h3>
            <p className="mt-2 text-sm text-slate-600">Are you sure you want to delete this membership plan? This action cannot be undone.</p>
            <div className="mt-6 flex justify-end gap-3">
              <Button type="button" onClick={() => setDeletingPlanId(null)} disabled={deleteMutation.isPending} className="bg-slate-100 text-slate-700 hover:bg-slate-200 h-9 rounded-full px-4 text-xs font-bold">
                Cancel
              </Button>
              <Button type="button" onClick={confirmDelete} disabled={deleteMutation.isPending} className="bg-red-600 text-white hover:bg-red-700 h-9 rounded-full px-4 text-xs font-bold">
                {deleteMutation.isPending ? "Deleting..." : "Yes, Delete"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
