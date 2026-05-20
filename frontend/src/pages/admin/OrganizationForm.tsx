import { useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  Check,
  Crown,
  MapPin,
  ShieldCheck,
  Sparkles,
  User2,
  Zap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";
import { foundingPartnerDescription } from "@/lib/subscriptionLabels";

const orgSchema = z.object({
  name: z.string().min(1, "Organization name is required"),
  district: z.string().min(1, "District is required"),
  organizationType: z.enum(["REGULAR", "PILOT"]),
  subscriptionPlan: z.enum(["STARTER", "GROWTH", "PRO"]),
  adminName: z.string().min(1, "Admin name is required"),
  adminEmail: z.string().email("Invalid email address"),
  adminPhone: z.string().min(1, "Admin phone is required"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  logoUrl: z.string().url("Must be a valid URL").optional().or(z.literal("")),
  mapLocationLink: z.string().url("Must be a valid URL").optional().or(z.literal("")),
  address: z.string().optional(),
  description: z.string().optional(),
});

type OrgFormValues = z.infer<typeof orgSchema>;

const stepMeta = [
  { id: 1, label: "Workspace Setup", icon: Building2 },
  { id: 2, label: "Admin Access", icon: User2 },
  { id: 3, label: "Review & Launch", icon: Sparkles },
] as const;

const planCards = [
  {
    key: "STARTER",
    label: "Startup",
    price: "Rs. 299/mo",
    note: "100 members",
    icon: Zap,
    points: ["Best for lean operations", "Core member workflows", "Simple billing footprint"],
  },
  {
    key: "GROWTH",
    label: "Growth",
    price: "Rs. 699/mo",
    note: "500 members",
    icon: ShieldCheck,
    points: ["Better reporting depth", "Stronger expansion fit", "Balanced operational scale"],
  },
  {
    key: "PRO",
    label: "Pro",
    price: "Rs. 1499/mo",
    note: "Unlimited members",
    icon: Crown,
    points: ["Unlimited scale", "Premium operating headroom", "Best for larger gym groups"],
  },
] as const;

export default function OrganizationForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(1);

  const { register, handleSubmit, trigger, watch, setValue, formState: { errors, isSubmitting } } = useForm<OrgFormValues>({
    resolver: zodResolver(orgSchema),
    mode: "onTouched",
    defaultValues: {
      organizationType: "REGULAR",
      subscriptionPlan: "STARTER",
    },
  });

  const values = watch();
  const isPilot = values.organizationType === "PILOT";

  const mutation = useMutation({
    mutationFn: (data: OrgFormValues) => api.post("/auth/register", {
      organizationName: data.name,
      district: data.district,
      logoUrl: data.logoUrl || null,
      mapLocationLink: data.mapLocationLink || null,
      address: data.address || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      organizationDescription: data.description || null,
      organizationType: data.organizationType,
      subscriptionPlan: data.organizationType === "PILOT" ? "PILOT" : data.subscriptionPlan,
      password: data.password,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizationsDirectory"] });
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization provisioned successfully");
      navigate("/app/organizations");
    },
    onError: () => toast.error("Failed to provision organization"),
  });

  const selectedPlan = useMemo(
    () => planCards.find((card) => card.key === values.subscriptionPlan) ?? planCards[0],
    [values.subscriptionPlan]
  );

  const nextStep = async () => {
    let isValid = false;
    if (step === 1) {
      isValid = await trigger(["name", "district", "organizationType", "subscriptionPlan", "address", "logoUrl", "mapLocationLink", "description"]);
    } else if (step === 2) {
      isValid = await trigger(["adminName", "adminEmail", "adminPhone", "password"]);
    }
    if (isValid) setStep((current) => Math.min(current + 1, 3));
  };

  const onSubmit = (data: OrgFormValues) => mutation.mutate(data);

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-4 sm:p-6 lg:p-8">
      <section className="relative overflow-hidden rounded-[2.3rem] border border-white/80 bg-slate-950 p-6 text-white shadow-[0_30px_90px_rgba(15,23,42,0.26)] sm:p-8">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-emerald-400/20 blur-3xl" />
        <div className="absolute bottom-0 left-12 h-56 w-56 rounded-full bg-sky-400/12 blur-3xl" />
        <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <Button variant="ghost" onClick={() => navigate("/app/organizations")} className="mb-5 rounded-full px-0 text-white hover:bg-transparent hover:text-emerald-200">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Organizations
            </Button>
            <div className="inline-flex items-center rounded-full border border-emerald-300/20 bg-emerald-300/10 px-4 py-2 text-[11px] font-black uppercase tracking-[0.28em] text-emerald-200">
              Organization Provisioning
            </div>
            <h1 className="mt-5 max-w-4xl text-4xl font-black tracking-tight sm:text-5xl">
              Create a gym workspace with a cleaner launch flow and stronger setup summary.
            </h1>
            <p className="mt-4 max-w-3xl text-sm font-semibold leading-6 text-slate-300">
              Choose the billing model, assign the first admin, and review the workspace before it goes live. Pilot gyms keep their premium founding partner access without the standard billing path.
            </p>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <aside className="space-y-6">
          <section className="glass-panel rounded-[2rem] p-5 sm:p-6">
            <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Progress</div>
            <div className="mt-5 space-y-4">
              {stepMeta.map((item) => {
                const Icon = item.icon;
                const active = step === item.id;
                const complete = step > item.id;
                return (
                  <div key={item.id} className={`flex items-center gap-4 rounded-[1.4rem] border p-4 ${active ? "border-emerald-200 bg-emerald-50/80" : "border-white/80 bg-white/75"}`}>
                    <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${complete ? "bg-slate-950 text-white" : active ? "bg-emerald-600 text-white" : "bg-slate-100 text-slate-500"}`}>
                      {complete ? <Check className="h-5 w-5" /> : <Icon className="h-5 w-5" />}
                    </div>
                    <div>
                      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Step {item.id}</div>
                      <div className="mt-1 text-sm font-black text-slate-950">{item.label}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="glass-panel rounded-[2rem] p-5 sm:p-6">
            <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Workspace Preview</div>
            <div className="mt-5 rounded-[1.7rem] border border-white/80 bg-white/80 p-5 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-lg font-black text-slate-950">{values.name || "Untitled organization"}</div>
                  <div className="mt-1 text-sm font-semibold text-slate-500">{values.district || "District pending"}</div>
                </div>
                <div className={`rounded-full px-3 py-1 text-xs font-black ${isPilot ? "bg-sky-100 text-sky-800" : "bg-emerald-100 text-emerald-800"}`}>
                  {isPilot ? "Pilot Gym" : "Standard Billing"}
                </div>
              </div>
              <div className="mt-5 space-y-3 text-sm font-semibold text-slate-600">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-slate-400" />
                  {values.address || "Address not added yet"}
                </div>
                <div className="flex items-center gap-2">
                  <User2 className="h-4 w-4 text-slate-400" />
                  {values.adminName || "Primary admin pending"}
                </div>
              </div>
            </div>

            <div className="mt-5 rounded-[1.7rem] border border-emerald-100 bg-emerald-50/80 p-5">
              <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">
                {isPilot ? "Founding Partner Access" : "Selected Plan"}
              </div>
              <div className="mt-2 text-2xl font-black text-slate-950">{isPilot ? "Premium early partner access" : selectedPlan.label}</div>
              <div className="mt-2 text-sm font-semibold text-slate-600">
                {isPilot ? foundingPartnerDescription() : `${selectedPlan.price} · ${selectedPlan.note}`}
              </div>
            </div>
          </section>
        </aside>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {step === 1 && (
            <section className="glass-panel overflow-hidden rounded-[2rem]">
              <div className="border-b border-black/5 p-6">
                <h2 className="text-2xl font-black tracking-tight text-slate-950">Workspace Setup</h2>
                <p className="mt-1 text-sm font-semibold text-slate-500">Start with billing model, plan fit, and the profile details that matter most.</p>
              </div>
              <div className="space-y-6 p-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  <AccessCard
                    title="Standard Billing"
                    subtitle="Trial-led onboarding with a subscription plan selected up front."
                    selected={!isPilot}
                    onClick={() => setValue("organizationType", "REGULAR", { shouldValidate: true })}
                  />
                  <AccessCard
                    title="Pilot Gym"
                    subtitle="Founding partner access with premium access and no standard billing path."
                    selected={isPilot}
                    onClick={() => setValue("organizationType", "PILOT", { shouldValidate: true })}
                  />
                </div>

                {!isPilot && (
                  <div>
                    <Label className="font-semibold text-slate-700">Subscription Plan</Label>
                    <div className="mt-3 grid gap-4 md:grid-cols-3">
                      {planCards.map((plan) => {
                        const Icon = plan.icon;
                        const selected = values.subscriptionPlan === plan.key;
                        return (
                          <button
                            key={plan.key}
                            type="button"
                            onClick={() => setValue("subscriptionPlan", plan.key, { shouldValidate: true })}
                            className={`rounded-[1.7rem] border p-5 text-left transition ${
                              selected ? "border-emerald-300 bg-emerald-50/80 shadow-sm" : "border-white/80 bg-white/75 hover:-translate-y-0.5 hover:shadow-sm"
                            }`}
                          >
                            <div className="flex items-center justify-between gap-3">
                              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-white">
                                <Icon className="h-5 w-5" />
                              </div>
                              {selected && <BadgePill label="Selected" />}
                            </div>
                            <div className="mt-5 text-xl font-black text-slate-950">{plan.label}</div>
                            <div className="mt-1 text-sm font-bold text-emerald-700">{plan.price}</div>
                            <div className="mt-1 text-sm font-semibold text-slate-500">{plan.note}</div>
                            <div className="mt-4 space-y-2 text-sm font-semibold text-slate-600">
                              {plan.points.map((point) => (
                                <div key={point} className="flex items-start gap-2">
                                  <Check className="mt-0.5 h-4 w-4 text-emerald-600" />
                                  <span>{point}</span>
                                </div>
                              ))}
                            </div>
                          </button>
                        );
                      })}
                    </div>
                    {errors.subscriptionPlan && <p className="mt-2 text-sm font-medium text-red-500">{errors.subscriptionPlan.message}</p>}
                  </div>
                )}

                {isPilot && (
                  <div className="rounded-[1.7rem] border border-sky-100 bg-sky-50/80 p-5">
                    <div className="text-[10px] font-black uppercase tracking-[0.22em] text-sky-700">Pilot Gym Access</div>
                    <p className="mt-2 text-sm font-semibold leading-6 text-slate-700">{foundingPartnerDescription()}</p>
                  </div>
                )}

                <div className="grid gap-5 md:grid-cols-2">
                  <TextField label="Business Name" error={errors.name?.message}>
                    <Input {...register("name")} placeholder="e.g. Iron Athletics" className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <TextField label="District" error={errors.district?.message}>
                    <Input {...register("district")} placeholder="e.g. Downtown" className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <TextField label="Full Address">
                    <Input {...register("address")} placeholder="123 Fitness Way, City" className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <TextField label="Logo URL" error={errors.logoUrl?.message}>
                    <Input {...register("logoUrl")} placeholder="https://..." className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <div className="md:col-span-2">
                    <TextField label="Google Maps Link" error={errors.mapLocationLink?.message}>
                      <Input {...register("mapLocationLink")} placeholder="https://maps.google.com/..." className="h-11 rounded-xl bg-white/70" />
                    </TextField>
                  </div>
                  <div className="md:col-span-2">
                    <TextField label="Internal Notes">
                      <textarea {...register("description")} className="min-h-[110px] w-full rounded-2xl border border-black/5 bg-white/70 p-4 text-sm text-slate-900 shadow-sm outline-none focus:ring-2 focus:ring-emerald-500/40" />
                    </TextField>
                  </div>
                </div>

                <div className="flex justify-end border-t border-black/5 pt-6">
                  <Button type="button" onClick={nextStep} className="h-11 rounded-full bg-emerald-600 px-7 font-black text-white hover:bg-emerald-700">
                    Continue to Admin Setup
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            </section>
          )}

          {step === 2 && (
            <section className="glass-panel overflow-hidden rounded-[2rem]">
              <div className="border-b border-black/5 p-6">
                <h2 className="text-2xl font-black tracking-tight text-slate-950">Admin Access</h2>
                <p className="mt-1 text-sm font-semibold text-slate-500">Set up the first operator account that will run this workspace.</p>
              </div>
              <div className="space-y-6 p-6">
                <div className="grid gap-5 md:grid-cols-2">
                  <TextField label="Full Name" error={errors.adminName?.message}>
                    <Input {...register("adminName")} placeholder="Manager Name" className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <TextField label="Phone Number" error={errors.adminPhone?.message}>
                    <Input {...register("adminPhone")} placeholder="+91 98765 43210" className="h-11 rounded-xl bg-white/70" />
                  </TextField>
                  <div className="md:col-span-2">
                    <TextField label="Email Address" error={errors.adminEmail?.message}>
                      <Input {...register("adminEmail")} type="email" placeholder="manager@gym.com" className="h-11 rounded-xl bg-white/70" />
                    </TextField>
                  </div>
                  <div className="md:col-span-2">
                    <TextField label="Temporary Password" error={errors.password?.message}>
                      <Input {...register("password")} type="password" placeholder="Temporary password" className="h-11 rounded-xl bg-white/70" />
                    </TextField>
                    <p className="mt-2 text-xs font-semibold text-slate-500">The admin can change this after first login.</p>
                  </div>
                </div>

                <div className="flex justify-between border-t border-black/5 pt-6">
                  <Button type="button" variant="outline" onClick={() => setStep(1)} className="h-11 rounded-full border-white/80 bg-white px-6 font-black">
                    Back
                  </Button>
                  <Button type="button" onClick={nextStep} className="h-11 rounded-full bg-emerald-600 px-7 font-black text-white hover:bg-emerald-700">
                    Review Launch Summary
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </div>
              </div>
            </section>
          )}

          {step === 3 && (
            <section className="glass-panel overflow-hidden rounded-[2rem]">
              <div className="border-b border-black/5 p-6">
                <h2 className="text-2xl font-black tracking-tight text-slate-950">Review & Launch</h2>
                <p className="mt-1 text-sm font-semibold text-slate-500">One final pass before the workspace and admin account are provisioned.</p>
              </div>
              <div className="space-y-6 p-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <ReviewTile label="Organization" value={values.name || "Untitled organization"} />
                  <ReviewTile label="District" value={values.district || "-"} />
                  <ReviewTile label="Billing Model" value={isPilot ? "Pilot Gym" : "Standard Billing"} />
                  <ReviewTile label="Plan" value={isPilot ? "Founding Partner Access" : selectedPlan.label} />
                  <ReviewTile label="Primary Admin" value={values.adminName || "-"} />
                  <ReviewTile label="Admin Email" value={values.adminEmail || "-"} />
                </div>

                {isPilot && (
                  <div className="rounded-[1.7rem] border border-sky-100 bg-sky-50/80 p-5">
                    <div className="text-[10px] font-black uppercase tracking-[0.22em] text-sky-700">Pilot Gym Confirmation</div>
                    <p className="mt-2 text-sm font-semibold leading-6 text-slate-700">{foundingPartnerDescription()}</p>
                  </div>
                )}

                <div className="flex justify-between border-t border-black/5 pt-6">
                  <Button type="button" variant="outline" onClick={() => setStep(2)} className="h-11 rounded-full border-white/80 bg-white px-6 font-black">
                    Back
                  </Button>
                  <Button type="submit" disabled={isSubmitting} className="h-11 rounded-full bg-slate-950 px-8 font-black text-white hover:bg-slate-800">
                    {isSubmitting ? "Provisioning..." : "Create Organization"}
                  </Button>
                </div>
              </div>
            </section>
          )}
        </form>
      </div>
    </div>
  );
}

function AccessCard({
  title,
  subtitle,
  selected,
  onClick,
}: {
  title: string;
  subtitle: string;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-[1.7rem] border p-5 text-left transition ${
        selected ? "border-emerald-300 bg-emerald-50/80 shadow-sm" : "border-white/80 bg-white/75 hover:-translate-y-0.5 hover:shadow-sm"
      }`}
    >
      <div className="flex items-center justify-between gap-3">
        <div className="text-lg font-black text-slate-950">{title}</div>
        {selected && <BadgePill label="Selected" />}
      </div>
      <p className="mt-3 text-sm font-semibold leading-6 text-slate-600">{subtitle}</p>
    </button>
  );
}

function TextField({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700">{label}</Label>
      {children}
      {error && <p className="text-sm font-medium text-red-500">{error}</p>}
    </div>
  );
}

function ReviewTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.4rem] border border-white/80 bg-white/80 p-4 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
      <div className="mt-2 text-sm font-black text-slate-950">{value}</div>
    </div>
  );
}

function BadgePill({ label }: { label: string }) {
  return <span className="rounded-full bg-emerald-600 px-3 py-1 text-[11px] font-black text-white">{label}</span>;
}
