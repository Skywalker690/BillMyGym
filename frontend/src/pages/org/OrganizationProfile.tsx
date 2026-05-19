import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Building, MapPin, Phone, Mail, User, KeyRound, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

export default function OrganizationProfile() {
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
    }
  });

  useEffect(() => {
    if (organization) {
      reset({
        name: organization.name || "",
        district: organization.district || "",
        description: organization.description || "",
        adminName: organization.adminFullName || "",
        adminPhone: organization.adminPhoneNumber || "",
        adminEmail: organization.adminEmail || "",
        monthlyMembershipFee: organization.monthlyMembershipFee || 0,
      });
    }
  }, [organization, reset]);

  const mutation = useMutation({
    mutationFn: (data: any) => api.patch(`/organizations/${organization.id}`, {
      name: data.name,
      district: data.district,
      logoUrl: organization.logoUrl,
      mapLocationLink: organization.mapLocationLink,
      address: organization.address,
      description: data.description || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      monthlyMembershipFee: data.monthlyMembershipFee,
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
      toast.success("A verification code has been sent to your email.");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to send password verification code.");
    }
  });

  const verifyPasswordOtpMutation = useMutation({
    mutationFn: (otp: string) => api.post("/auth/verify-otp", {
      email: user?.email,
      otp,
    }),
    onSuccess: () => {
      setPasswordOtpVerified(true);
      clearPasswordErrors("otp");
      toast.success("Code verified. You can now update your password.");
    },
    onError: (error: any) => {
      setPasswordOtpVerified(false);
      setPasswordError("otp", { message: error?.response?.data?.message || "Invalid or expired OTP" });
    }
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
    }
  });

  const onSubmit = (data: any) => {
    mutation.mutate(data);
  };

  const onRequestPasswordOtp = (data: any) => {
    if (data.newPassword !== data.confirmPassword) {
      setPasswordError("confirmPassword", { message: "Passwords do not match" });
      return;
    }
    clearPasswordErrors("confirmPassword");
    requestPasswordOtpMutation.mutate({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
      confirmPassword: data.confirmPassword,
    });
  };

  const onConfirmPassword = (data: any) => {
    if (!passwordOtpVerified) {
      setPasswordError("otp", { message: "Verify the OTP before updating the password" });
      return;
    }
    confirmPasswordMutation.mutate(data.otp);
  };

  if (isLoading) {
    return <div className="p-8">Loading profile...</div>;
  }

  if (!organization) {
    return <div className="p-8">No organization found.</div>;
  }

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8">
      <div className="billmygym-shell flex items-center space-x-4 mb-4 p-6 rounded-[2rem] backdrop-blur-xl">
        <div className="h-16 w-16 rounded-2xl flex items-center justify-center text-emerald-700 ring-4 ring-white/60 shadow-md bg-[linear-gradient(135deg,#e7f6ee_0%,#fff7ed_100%)]">
          <Building className="h-8 w-8" />
        </div>
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white tracking-tight">{organization.name}</h1>
          <p className="text-slate-500 dark:text-slate-400 font-medium">Organization Profile</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building className="w-5 h-5 mr-3 text-indigo-500" /> General Information</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Public details about the gym.</p>
          </div>
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name" className="font-semibold text-slate-700 dark:text-slate-300">Organization Name</Label>
                <div className="relative">
                  <Building className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                  <Input id="name" {...register("name")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="district" className="font-semibold text-slate-700 dark:text-slate-300">District / Region</Label>
                <div className="relative">
                  <MapPin className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                  <Input id="district" {...register("district")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description" className="font-semibold text-slate-700 dark:text-slate-300">Description</Label>
              <textarea
                id="description"
                {...register("description")}
                className="w-full p-3 pl-4 rounded-xl shadow-sm border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 backdrop-blur-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 min-h-[100px] text-slate-900 dark:text-slate-100"
                placeholder="A short description of your facility..."
              />
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><User className="w-5 h-5 mr-3 text-emerald-500" /> Primary Contact</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Contact info for the main administrator.</p>
          </div>
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="adminName" className="font-semibold text-slate-700 dark:text-slate-300">Admin Full Name</Label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminName" {...register("adminName")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
            </div>
            <div className="space-y-2 md:col-start-1">
              <Label htmlFor="adminPhone" className="font-semibold text-slate-700 dark:text-slate-300">Admin Phone Number</Label>
              <div className="relative">
                <Phone className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminPhone" {...register("adminPhone")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="adminEmail" className="font-semibold text-slate-700 dark:text-slate-300">Admin Email</Label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminEmail" {...register("adminEmail")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" type="email" />
              </div>
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building className="w-5 h-5 mr-3 text-amber-500" /> Billing Configuration</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Financial settings for this organization.</p>
          </div>
          <div className="p-6">
            <div className="space-y-2 max-w-sm">
              <Label htmlFor="monthlyMembershipFee" className="font-semibold text-slate-700 dark:text-slate-300">Monthly Membership Fee (INR)</Label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-500 font-bold">Rs</span>
                <Input
                  id="monthlyMembershipFee"
                  type="number"
                  {...register("monthlyMembershipFee", { valueAsNumber: true })}
                  className="pl-9 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><KeyRound className="w-5 h-5 mr-3 text-emerald-600" /> Password & Security</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Change your organization admin password with email verification.</p>
          </div>
          <div className="p-6 grid grid-cols-1 xl:grid-cols-[1.4fr_0.9fr] gap-6">
            <form onSubmit={handlePasswordSubmit(onRequestPasswordOtp)} className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="space-y-2 md:col-span-2">
                  <Label htmlFor="currentPassword" className="font-semibold text-slate-700 dark:text-slate-300">Current Password</Label>
                  <Input id="currentPassword" type="password" {...registerPassword("currentPassword", { required: "Current password is required" })} className="bg-white/70 border-white/80 rounded-xl h-11" />
                  {passwordErrors.currentPassword && <p className="text-sm font-medium text-red-500">{String(passwordErrors.currentPassword.message)}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="newPassword" className="font-semibold text-slate-700 dark:text-slate-300">New Password</Label>
                  <Input id="newPassword" type="password" {...registerPassword("newPassword", { required: "New password is required", minLength: { value: 8, message: "Password must be at least 8 characters" } })} className="bg-white/70 border-white/80 rounded-xl h-11" />
                  {passwordErrors.newPassword && <p className="text-sm font-medium text-red-500">{String(passwordErrors.newPassword.message)}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword" className="font-semibold text-slate-700 dark:text-slate-300">Confirm New Password</Label>
                  <Input id="confirmPassword" type="password" {...registerPassword("confirmPassword", { required: "Confirm password is required" })} className="bg-white/70 border-white/80 rounded-xl h-11" />
                  {passwordErrors.confirmPassword && <p className="text-sm font-medium text-red-500">{String(passwordErrors.confirmPassword.message)}</p>}
                </div>
              </div>

              <div className="flex flex-wrap gap-3">
                <Button type="submit" disabled={requestPasswordOtpMutation.isPending || isPasswordSubmitting} className="rounded-full bg-emerald-600 hover:bg-emerald-700 text-white px-6">
                  {requestPasswordOtpMutation.isPending ? "Sending code..." : "Send Verification Code"}
                </Button>
                {passwordOtpSent && (
                  <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700">
                    Code sent to {user?.email}
                  </span>
                )}
              </div>

              {passwordOtpSent && (
                <div className="rounded-[1.5rem] border border-amber-200 bg-[var(--billmygym-amber-bg)] p-5">
                  <div className="grid grid-cols-1 md:grid-cols-[1fr_auto_auto] gap-3 items-end">
                    <div className="space-y-2">
                      <Label htmlFor="otp" className="font-semibold text-slate-700">Verification Code</Label>
                      <Input id="otp" {...registerPassword("otp", { required: "OTP is required", minLength: { value: 6, message: "OTP must be 6 digits" }, maxLength: { value: 6, message: "OTP must be 6 digits" } })} className="bg-white/80 border-white/80 rounded-xl h-11" />
                      {passwordErrors.otp && <p className="text-sm font-medium text-red-500">{String(passwordErrors.otp.message)}</p>}
                    </div>
                    <Button type="button" variant="outline" onClick={() => verifyPasswordOtpMutation.mutate(watchPassword("otp"))} disabled={verifyPasswordOtpMutation.isPending} className="rounded-full h-11">
                      {verifyPasswordOtpMutation.isPending ? "Verifying..." : "Verify OTP"}
                    </Button>
                    <Button type="button" onClick={handlePasswordSubmit(onConfirmPassword)} disabled={!passwordOtpVerified || confirmPasswordMutation.isPending} className="rounded-full h-11 bg-slate-900 hover:bg-slate-800 text-white">
                      {confirmPasswordMutation.isPending ? "Updating..." : "Update Password"}
                    </Button>
                  </div>
                </div>
              )}
            </form>

            <div className="rounded-[1.75rem] border border-emerald-100 bg-[linear-gradient(180deg,#f8fcfa_0%,#eef4f0_100%)] p-6 shadow-sm">
              <div className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-3 py-1 text-[11px] font-black uppercase tracking-[0.2em] text-emerald-700">
                <ShieldCheck className="h-4 w-4" />
                Security Flow
              </div>
              <div className="mt-5 space-y-4 text-sm text-slate-600">
                <p className="font-semibold text-slate-900">Your password update follows the same protected flow as BillMyGym email verification.</p>
                <p>1. Enter your current password and new password.</p>
                <p>2. We send a one-time code to your admin email.</p>
                <p>3. Verify the code and confirm the update.</p>
                <p className="rounded-2xl border border-amber-200 bg-white/75 px-4 py-3 text-amber-800">Keep this email inbox secure. The OTP expires automatically and can only be used once.</p>
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4 mt-8">
          <Button type="submit" disabled={isSubmitting} className="px-10 rounded-full shadow-md shadow-emerald-500/20 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold h-12">
            {isSubmitting ? "Saving..." : "Save Profile"}
          </Button>
        </div>
      </form>
    </div>
  );
}

