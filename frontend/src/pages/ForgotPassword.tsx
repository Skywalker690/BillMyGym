import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, Mail } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";

const forgotPasswordSchema = z.object({
  email: z.string().email("Enter a valid email address"),
});

type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const presetEmail = searchParams.get("email") ?? "";
  const [submittedEmail, setSubmittedEmail] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: presetEmail },
  });

  const mutation = useMutation({
    mutationFn: async (values: ForgotPasswordValues) => api.post("/auth/forgot-password", values),
    onSuccess: (_, values) => {
      setSubmittedEmail(values.email);
      toast.success("If your account exists, we have sent a reset code to your email.");
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || "Unable to send reset code right now.";
      toast.error(message);
    },
  });

  const goToReset = () => {
    navigate(`/reset-password?email=${encodeURIComponent(submittedEmail)}`);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <Link to="/login" className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900">
          <ArrowLeft className="h-4 w-4" />
          Back to sign in
        </Link>
        <div className="mt-6 rounded-3xl bg-white px-8 py-10 shadow-xl shadow-slate-200/70 ring-1 ring-slate-200">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
            <Mail className="h-7 w-7" />
          </div>
          <h1 className="mt-6 text-center text-3xl font-extrabold tracking-tight text-slate-900">
            Reset your password
          </h1>
          <p className="mt-3 text-center text-sm leading-6 text-slate-600">
            Enter the email address for your BillMyGym account and we will send you a one-time reset code.
          </p>

          <form className="mt-8 space-y-6" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                Account email
              </label>
              <div className="mt-1">
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  {...register("email")}
                  className="h-11"
                />
              </div>
              {errors.email && <p className="mt-2 text-sm font-medium text-red-500">{errors.email.message}</p>}
            </div>

            <Button type="submit" disabled={mutation.isPending} className="w-full h-11 bg-emerald-600 hover:bg-emerald-700 text-white">
              {mutation.isPending ? "Sending code..." : "Send reset code"}
            </Button>
          </form>

          {submittedEmail && (
            <div className="mt-6 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-900">
              <p className="font-semibold">Check your inbox</p>
              <p className="mt-1 leading-6">
                If <strong>{submittedEmail}</strong> belongs to an active account, a 6-digit code is on its way.
              </p>
              <Button type="button" variant="outline" onClick={goToReset} className="mt-4 w-full">
                Continue to reset password
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

