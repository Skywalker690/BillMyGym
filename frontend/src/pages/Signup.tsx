import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dumbbell, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

const schema = z.object({
  name: z.string().min(1, "Organization name is required"),
  place: z.string().min(1, "Place is required"),
  district: z.string().min(1, "District is required"),
  adminName: z.string().min(1, "Admin name is required"),
  adminEmail: z.string().email("Enter a valid admin email"),
  adminPhone: z.string().min(1, "Admin phone is required"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  referralCodeUsed: z.string().optional(),
});

type FormInput = z.input<typeof schema>;
type Values = z.output<typeof schema>;

export default function Signup() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const referralCode = searchParams.get("ref") || "";

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, any, Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: "",
      place: "",
      district: "",
      adminName: "",
      adminEmail: "",
      adminPhone: "",
      password: "",
      referralCodeUsed: referralCode,
    },
  });

  useEffect(() => {
    if (referralCode) {
      setValue("referralCodeUsed", referralCode);
    }
  }, [referralCode, setValue]);

  const mutation = useMutation({
    mutationFn: (data: Values) =>
      api.post("/auth/signup", {
        organizationName: data.name,
        place: data.place,
        district: data.district,
        adminFullName: data.adminName,
        adminEmail: data.adminEmail,
        adminPhoneNumber: data.adminPhone,
        password: data.password,
        referralCodeUsed: data.referralCodeUsed || null,
      }),
    onSuccess: () => {
      toast.success("Organization created successfully! Check your email.");
      navigate("/login");
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Failed to create organization";
      toast.error(msg);
    },
  });

  const onSubmit = (values: Values) => mutation.mutate(values);

  return (
    <div className="flex min-h-screen bg-slate-50">
      <div className="flex flex-1 flex-col justify-center px-4 py-12 sm:px-6 lg:flex-none lg:px-20 xl:px-24">
        <div className="mx-auto w-full max-w-sm lg:w-[24rem]">
          <div className="flex flex-col items-center justify-center text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-[1.2rem] bg-emerald-600 text-white shadow-lg">
              <Dumbbell className="h-6 w-6" />
            </div>
            <h2 className="mt-6 text-3xl font-black tracking-tight text-slate-900">
              Join GymFlow
            </h2>
            <p className="mt-2 text-sm text-slate-600">
              Already have an account?{" "}
              <button
                onClick={() => navigate("/login")}
                className="font-bold text-emerald-600 hover:text-emerald-500"
              >
                Log in here
              </button>
            </p>
          </div>

          <div className="mt-10">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div className="space-y-1">
                <Label>Organization Name</Label>
                <Input {...register("name")} className="h-12 rounded-2xl" placeholder="GymMax" />
                {errors.name && <p className="text-xs font-semibold text-rose-600">{errors.name.message}</p>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label>Place</Label>
                  <Input {...register("place")} className="h-12 rounded-2xl" placeholder="Kakkanad" />
                  {errors.place && <p className="text-xs font-semibold text-rose-600">{errors.place.message}</p>}
                </div>
                <div className="space-y-1">
                  <Label>District</Label>
                  <Input {...register("district")} className="h-12 rounded-2xl" placeholder="Ernakulam" />
                  {errors.district && <p className="text-xs font-semibold text-rose-600">{errors.district.message}</p>}
                </div>
              </div>

              <div className="space-y-1">
                <Label>Admin Name</Label>
                <Input {...register("adminName")} className="h-12 rounded-2xl" placeholder="John Doe" />
                {errors.adminName && <p className="text-xs font-semibold text-rose-600">{errors.adminName.message}</p>}
              </div>

              <div className="space-y-1">
                <Label>Admin Email</Label>
                <Input type="email" {...register("adminEmail")} className="h-12 rounded-2xl" placeholder="john@gymmax.com" />
                {errors.adminEmail && <p className="text-xs font-semibold text-rose-600">{errors.adminEmail.message}</p>}
              </div>

              <div className="space-y-1">
                <Label>Phone Number</Label>
                <Input {...register("adminPhone")} className="h-12 rounded-2xl" placeholder="+91 98XXXXXXX" />
                {errors.adminPhone && <p className="text-xs font-semibold text-rose-600">{errors.adminPhone.message}</p>}
              </div>

              <div className="space-y-1">
                <Label>Password</Label>
                <Input type="password" {...register("password")} className="h-12 rounded-2xl" />
                {errors.password && <p className="text-xs font-semibold text-rose-600">{errors.password.message}</p>}
              </div>

              <div className="space-y-1">
                <Label>Referral Code (Optional)</Label>
                <Input {...register("referralCodeUsed")} className="h-12 rounded-2xl" placeholder="REF-CODE" readOnly={!!referralCode} />
              </div>

              <Button
                type="submit"
                className="w-full h-12 rounded-full bg-slate-950 font-black text-white hover:bg-slate-800"
                disabled={isSubmitting || mutation.isPending}
              >
                {isSubmitting || mutation.isPending ? "Creating account..." : "Sign up"}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </form>
          </div>
        </div>
      </div>
      <div className="relative hidden w-0 flex-1 lg:block">
        <div className="absolute inset-0 bg-slate-950">
          <div className="flex h-full items-center justify-center p-20">
            <div className="max-w-xl text-center">
              <h2 className="text-4xl font-black text-white sm:text-5xl">
                Scale your gym with confidence.
              </h2>
              <p className="mt-4 text-lg text-slate-400 font-medium">
                GymFlow gives you all the tools you need to manage your members, subscriptions, and staff efficiently.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
