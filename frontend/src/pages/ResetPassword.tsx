import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, KeyRound, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";

const resetPasswordSchema = z.object({
  email: z.string().email("Enter a valid email address"),
  otp: z.string().length(6, "OTP must be 6 digits"),
  newPassword: z.string().min(8, "Password must be at least 8 characters"),
  confirmPassword: z.string().min(8, "Confirm password is required"),
}).refine((values) => values.newPassword === values.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;

export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const presetEmail = searchParams.get("email") ?? "";
  const [isOtpVerified, setIsOtpVerified] = useState(false);

  const defaultValues = useMemo<ResetPasswordValues>(() => ({
    email: presetEmail,
    otp: "",
    newPassword: "",
    confirmPassword: "",
  }), [presetEmail]);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues,
  });

  const email = watch("email");
  const otp = watch("otp");

  useEffect(() => {
    setIsOtpVerified(false);
  }, [email, otp]);

  const verifyOtpMutation = useMutation({
    mutationFn: async (values: Pick<ResetPasswordValues, "email" | "otp">) => {
      await api.post("/auth/verify-otp", values);
    },
    onSuccess: () => {
      setIsOtpVerified(true);
      clearErrors("otp");
      toast.success("Code verified. You can set a new password now.");
    },
    onError: (error: any) => {
      setIsOtpVerified(false);
      setError("otp", { message: error?.response?.data?.message || "Invalid or expired OTP" });
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: async (values: ResetPasswordValues) => {
      await api.post("/auth/change-password", {
        email: values.email,
        otp: values.otp,
        newPassword: values.newPassword,
      });
    },
    onSuccess: () => {
      toast.success("Password changed successfully. Please sign in with your new password.");
      navigate("/login");
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || "Unable to change password right now.";
      toast.error(message);
    },
  });

  const verifyOtp = async () => {
    setIsOtpVerified(false);
    await verifyOtpMutation.mutateAsync({ email, otp });
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link to="/forgot-password" className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900">
          <ArrowLeft className="h-4 w-4" />
          Back to forgot password
        </Link>
        <div className="mt-6 rounded-3xl bg-white px-8 py-10 shadow-xl shadow-slate-200/70 ring-1 ring-slate-200">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-900 text-white">
            <KeyRound className="h-7 w-7" />
          </div>
          <h1 className="mt-6 text-center text-3xl font-extrabold tracking-tight text-slate-900">
            Set a new password
          </h1>
          <p className="mt-3 text-center text-sm leading-6 text-slate-600">
            Enter the reset code from your email, verify it, and then choose a strong new password.
          </p>

          <form className="mt-8 space-y-5" onSubmit={handleSubmit((values) => resetPasswordMutation.mutate(values))}>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                Account email
              </label>
              <div className="mt-1">
                <Input id="email" type="email" autoComplete="email" {...register("email")} className="h-11" />
              </div>
              {errors.email && <p className="mt-2 text-sm font-medium text-red-500">{errors.email.message}</p>}
            </div>

            <div>
              <label htmlFor="otp" className="block text-sm font-medium text-slate-700">
                6-digit reset code
              </label>
              <div className="mt-1 flex gap-3">
                <Input id="otp" inputMode="numeric" maxLength={6} {...register("otp")} className="h-11" />
                <Button
                  type="button"
                  variant="outline"
                  onClick={verifyOtp}
                  disabled={verifyOtpMutation.isPending}
                  className="shrink-0"
                >
                  {verifyOtpMutation.isPending ? "Verifying..." : "Verify"}
                </Button>
              </div>
              {errors.otp && <p className="mt-2 text-sm font-medium text-red-500">{errors.otp.message}</p>}
            </div>

            {isOtpVerified && (
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-900 flex gap-3">
                <ShieldCheck className="h-5 w-5 shrink-0 mt-0.5" />
                <p>Your code is verified. Finish by setting your new password below.</p>
              </div>
            )}

            <div>
              <label htmlFor="newPassword" className="block text-sm font-medium text-slate-700">
                New password
              </label>
              <div className="mt-1">
                <Input id="newPassword" type="password" autoComplete="new-password" {...register("newPassword")} className="h-11" />
              </div>
              {errors.newPassword && <p className="mt-2 text-sm font-medium text-red-500">{errors.newPassword.message}</p>}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-slate-700">
                Confirm new password
              </label>
              <div className="mt-1">
                <Input id="confirmPassword" type="password" autoComplete="new-password" {...register("confirmPassword")} className="h-11" />
              </div>
              {errors.confirmPassword && <p className="mt-2 text-sm font-medium text-red-500">{errors.confirmPassword.message}</p>}
            </div>

            <Button
              type="submit"
              disabled={!isOtpVerified || resetPasswordMutation.isPending}
              className="w-full h-11 bg-slate-900 hover:bg-slate-800 text-white"
            >
              {resetPasswordMutation.isPending ? "Updating password..." : "Update password"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
