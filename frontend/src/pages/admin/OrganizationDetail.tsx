import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  ArrowLeft,
  Building2,
  CalendarDays,
  CreditCard,
  Info,
  IndianRupee,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Sparkles,
  Trash2,
  User2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";
import { organizationStatusLabel, organizationTypeLabel } from "@/lib/subscriptionLabels";

type Organization = {
  id: number;
  name: string;
  district?: string | null;
  place?: string | null;
  logoUrl?: string | null;
  mapLocationLink?: string | null;
  address?: string | null;
  adminFullName?: string | null;
  adminPhoneNumber?: string | null;
  adminEmail?: string | null;
  description?: string | null;
  monthlyMembershipFee?: number | null;
  organizationType?: "REGULAR" | "PILOT" | "DEMO";
  subscriptionPlan?: "STARTER" | "GROWTH" | "PRO" | "PILOT";
  status: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  trialStartDate?: string | null;
  trialEndDate?: string | null;
  pilotStartDate?: string | null;
  pilotEndDate?: string | null;
  subscriptionDueDate?: string | null;
};

type SubscriptionPayment = {
  id: number;
  amount: number;
  paymentDate?: string | null;
  subscriptionStart?: string | null;
  subscriptionEnd?: string | null;
  paymentStatus: "PAID" | "PENDING" | "FAILED" | "REFUNDED";
  razorpayOrderId?: string | null;
};

type PaymentHistoryPage = {
  items: SubscriptionPayment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

type SubscriptionStatus = {
  monthlyAmount?: number;
  lifetimeSubscriptionRevenue?: number;
  joinedAt?: string | null;
  currentPeriodStart?: string | null;
  currentPeriodEnd?: string | null;
  subscriptionDueDate?: string | null;
  whatsappUsage?: number;
  emailUsage?: number;
  paymentHistory?: SubscriptionPayment[];
  paymentHistoryPage?: PaymentHistoryPage;
};

type FormValues = {
  name: string;
  place: string;
  district: string;
  address: string;
  description: string;
  adminName: string;
  adminPhone: string;
  adminEmail: string;
  organizationType: "REGULAR" | "PILOT" | "DEMO";
  status: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
};

const paymentBadgeClasses: Record<SubscriptionPayment["paymentStatus"], string> = {
  PAID: "border-emerald-200 bg-emerald-100 text-emerald-800",
  PENDING: "border-amber-200 bg-amber-100 text-amber-800",
  FAILED: "border-rose-200 bg-rose-100 text-rose-800",
  REFUNDED: "border-slate-200 bg-slate-100 text-slate-700",
};

function formatDate(value?: string | null) {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
}

function planLabel(plan?: Organization["subscriptionPlan"]) {
  switch (plan) {
    case "STARTER":
      return "Starter";
    case "GROWTH":
      return "Growth";
    case "PRO":
      return "Pro";
    case "PILOT":
      return "Pilot";
    case "DEMO":
      return "Demo";
    default:
      return "Starter";
  }
}

export default function OrganizationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [paymentHistory, setPaymentHistory] = useState<SubscriptionPayment[]>([]);
  const [paymentHistoryPage, setPaymentHistoryPage] = useState<PaymentHistoryPage | null>(null);

  const { data: organization, isLoading } = useQuery<Organization>({
    queryKey: ["organization", id],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}`);
      return res.data;
    },
  });

  const { data: subscriptionStatus } = useQuery<SubscriptionStatus>({
    queryKey: ["organizationSubscriptionStatus", id],
    queryFn: async () => {
      const res = await api.get(`/subscription/organizations/${id}/status`);
      return res.data;
    },
    enabled: Boolean(id),
    staleTime: 60_000,
  });

  useEffect(() => {
    if (!subscriptionStatus) {
      setPaymentHistory([]);
      setPaymentHistoryPage(null);
      return;
    }
    const initialHistory = subscriptionStatus.paymentHistory ?? [];
    setPaymentHistory(initialHistory);
    setPaymentHistoryPage(
      subscriptionStatus.paymentHistoryPage ?? {
        items: initialHistory,
        page: 0,
        size: 5,
        totalElements: initialHistory.length,
        totalPages: initialHistory.length ? 1 : 0,
        hasNext: false,
      }
    );
  }, [subscriptionStatus]);

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm<FormValues>();

  useEffect(() => {
    if (!organization) return;
    reset({
      name: organization.name,
      place: organization.place ?? "",
      district: organization.district ?? "",
      address: organization.address ?? "",
      description: organization.description ?? "",
      adminName: organization.adminFullName ?? "",
      adminPhone: organization.adminPhoneNumber ?? "",
      adminEmail: organization.adminEmail ?? "",
      organizationType: organization.organizationType ?? "REGULAR",
      status: organization.status,
    });
  }, [organization, reset]);

  const updateMutation = useMutation({
    mutationFn: (data: FormValues) => api.patch(`/organizations/${id}`, {
      name: data.name,
      place: data.place || null,
      district: data.district,
      logoUrl: organization?.logoUrl,
      mapLocationLink: organization?.mapLocationLink,
      address: data.address || null,
      description: data.description || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      monthlyMembershipFee: organization?.monthlyMembershipFee,
      organizationType: data.organizationType,
      subscriptionPlan: organization?.subscriptionPlan,
      status: data.status,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organization", id] });
      queryClient.invalidateQueries({ queryKey: ["organizationsDirectory"] });
      toast.success("Organization details updated successfully");
    },
    onError: () => toast.error("Failed to update organization"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/organizations/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizationsDirectory"] });
      toast.success("Organization deleted successfully");
      navigate("/app/organizations");
    },
    onError: () => toast.error("Failed to delete organization"),
  });

  const loadMoreHistoryMutation = useMutation({
    mutationFn: async (page: number) => {
      const res = await api.get(`/subscription/organizations/${id}/payments`, {
        params: { page, size: paymentHistoryPage?.size ?? 5 },
      });
      return res.data as PaymentHistoryPage;
    },
    onSuccess: (page) => {
      setPaymentHistory((current) => {
        const existingIds = new Set(current.map((payment) => payment.id));
        return [...current, ...page.items.filter((payment) => !existingIds.has(payment.id))];
      });
      setPaymentHistoryPage(page);
    },
    onError: () => toast.error("Could not load older subscription payments"),
  });

  const onSubmit = (data: FormValues) => updateMutation.mutate(data);

  const needsRenewalAttention = useMemo(() => {
    if (!organization) return false;
    if (organization.status === "PAUSED" || organization.status === "DEACTIVATED") return true;
    const targetDate =
      organization.organizationType === "PILOT" || organization.organizationType === "DEMO"
        ? organization.pilotEndDate
        : organization.status === "TRIAL"
          ? organization.trialEndDate ?? organization.subscriptionDueDate
          : organization.subscriptionDueDate;
    if (!targetDate) return false;
    const limit = new Date();
    limit.setDate(limit.getDate() + 7);
    return new Date(targetDate) <= limit;
  }, [organization]);

  if (isLoading) return <div className="p-8 text-sm font-semibold text-slate-500">Loading organization...</div>;
  if (!organization) return <div className="p-8 text-sm font-semibold text-slate-500">Organization not found.</div>;

  const money = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  });

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-4 sm:p-6 lg:p-8">
      <section className="relative overflow-hidden rounded-[2.3rem] border border-white/80 bg-slate-950 p-6 text-white shadow-[0_28px_90px_rgba(15,23,42,0.24)] sm:p-8">
        <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-emerald-400/18 blur-3xl" />
        <div className="absolute bottom-0 left-16 h-52 w-52 rounded-full bg-rose-400/12 blur-3xl" />
        <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <Button variant="ghost" onClick={() => navigate("/app/organizations")} className="mb-5 rounded-full px-0 text-white hover:bg-transparent hover:text-emerald-200">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Organizations
            </Button>
            <div className="flex flex-wrap items-center gap-2">
              <Badge className="border-white/20 bg-white/10 text-white">{organizationStatusLabel(organization.status)}</Badge>
              <Badge className="border-emerald-300/20 bg-emerald-300/10 text-emerald-100">{planLabel(organization.subscriptionPlan)}</Badge>
              {(organization.organizationType === "PILOT" || organization.organizationType === "DEMO") && (
                <Badge className="border-sky-300/20 bg-sky-300/10 text-sky-100">{organizationTypeLabel(organization.organizationType)}</Badge>
              )}
            </div>
            <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">{organization.name}</h1>
            <p className="mt-3 max-w-3xl text-sm font-semibold leading-6 text-slate-300">
              Subscription health, organization details, and admin records are organized here in a cleaner operations layout so billing issues and access windows are easier to spot.
            </p>
          </div>
          <Button
            variant="destructive"
            onClick={() => {
              if (confirm("Soft delete this organization? History, members, and payments will be preserved.")) {
                deleteMutation.mutate();
              }
            }}
            disabled={deleteMutation.isPending}
            className="rounded-full px-6 font-black"
          >
            <Trash2 className="mr-2 h-4 w-4" />
            {deleteMutation.isPending ? "Deleting..." : "Delete Organization"}
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(`/app/organizations/${organization.id}/audit-logs`)}
            className="rounded-full border-white/40 bg-white/10 px-6 font-black text-white hover:bg-white/20 hover:text-white"
          >
            View Audit Logs
          </Button>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          icon={CalendarDays}
          label={organization.organizationType === "PILOT" || organization.organizationType === "DEMO" ? "Access Window" : "Renewal Date"}
          value={formatDate(
            organization.organizationType === "PILOT" || organization.organizationType === "DEMO"
              ? organization.pilotEndDate
              : organization.status === "TRIAL"
                ? organization.trialEndDate ?? organization.subscriptionDueDate
                : organization.subscriptionDueDate
          )}
          tone={needsRenewalAttention ? "rose" : "emerald"}
          infoContent={`Current Billing Period: ${formatDate(subscriptionStatus?.currentPeriodStart)} - ${formatDate(subscriptionStatus?.currentPeriodEnd)}`}
        />
        <SummaryCard
          icon={CreditCard}
          label="Subscription Plan"
          value={planLabel(organization.subscriptionPlan)}
          tone="slate"
        />
        <SummaryCard
          icon={IndianRupee}
          label="Monthly Billing"
          value={money.format(subscriptionStatus?.monthlyAmount ?? 0)}
          tone="slate"
        />
        <SummaryCard
          icon={CreditCard}
          label="Total Revenue"
          value={money.format(subscriptionStatus?.lifetimeSubscriptionRevenue ?? 0)}
          tone="emerald"
          infoContent={`Joined GymMax: ${formatDate(subscriptionStatus?.joinedAt)}`}
        />
      </section>

      <form onSubmit={handleSubmit(onSubmit)} className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <div className="space-y-6">
          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="text-2xl font-black tracking-tight text-slate-950">Workspace Profile</h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">Update the core organization record and keep the profile polished.</p>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <Field label="Business Name">
                <Input {...register("name")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="Place">
                <Input {...register("place")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="District">
                <Input {...register("district")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="Full Address">
                <Input {...register("address")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="Map Location">
                <Input value={organization.mapLocationLink ?? ""} readOnly className="h-11 rounded-xl bg-slate-50 text-slate-500" />
              </Field>
              <div className="md:col-span-2">
                <Field label="Internal Notes">
                  <textarea {...register("description")} className="min-h-[120px] w-full rounded-2xl border border-black/5 bg-white/70 p-4 text-sm text-slate-900 shadow-sm outline-none focus:ring-2 focus:ring-emerald-500/40" />
                </Field>
              </div>
            </div>
          </section>

          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <ShieldCheck className="h-6 w-6 text-emerald-700" />
                Access & Billing Control
              </h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">Manage subscription state, pilot access, and visible renewal signals.</p>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <Field label="Subscription Status">
                <select {...register("status")} className="h-11 w-full rounded-xl border border-black/5 bg-white/75 px-4 text-sm font-semibold text-slate-900 outline-none focus:ring-2 focus:ring-emerald-500/40">
                  <option value="TRIAL">Free Trial Access</option>
                  <option value="ACTIVE">Active Subscription</option>
                  <option value="PAUSED">Subscription Renewal Required</option>
                  <option value="DEACTIVATED">Account Inactive</option>
                </select>
              </Field>
              <Field label="Pilot Gym Access">
                <select {...register("organizationType")} className="h-11 w-full rounded-xl border border-black/5 bg-white/75 px-4 text-sm font-semibold text-slate-900 outline-none focus:ring-2 focus:ring-emerald-500/40">
                  <option value="REGULAR">Standard Billing</option>
                  <option value="PILOT">Pilot</option>
                  <option value="DEMO">Demo</option>
                </select>
              </Field>
            </div>
            <div className="grid gap-4 border-t border-black/5 p-6 md:grid-cols-3">
              <DateTile label="Trial Window" value={`${formatDate(organization.trialStartDate)} - ${formatDate(organization.trialEndDate)}`} attention={organization.status === "TRIAL" && needsRenewalAttention} />
              <DateTile label="Pilot Window" value={`${formatDate(organization.pilotStartDate)} - ${formatDate(organization.pilotEndDate)}`} attention={(organization.organizationType === "PILOT" || organization.organizationType === "DEMO") && needsRenewalAttention} />
              <DateTile label="Renewal Date" value={formatDate(organization.subscriptionDueDate)} attention={organization.organizationType === "REGULAR" && needsRenewalAttention} />
            </div>
          </section>

          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <User2 className="h-6 w-6 text-emerald-700" />
                Primary Admin
              </h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">Keep the main operator’s contact details current for billing and access communication.</p>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <Field label="Full Name">
                <Input {...register("adminName")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="Phone Number">
                <Input {...register("adminPhone")} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <div className="md:col-span-2">
                <Field label="Email Address">
                  <Input {...register("adminEmail")} type="email" className="h-11 rounded-xl bg-white/70" />
                </Field>
              </div>
            </div>
          </section>

          <div className="flex justify-end">
            <Button type="submit" disabled={isSubmitting} className="h-12 rounded-full bg-emerald-600 px-8 font-black text-white hover:bg-emerald-700">
              {isSubmitting ? "Saving..." : "Save Organization Changes"}
            </Button>
          </div>
        </div>

        <div className="space-y-6">
          <section className="glass-panel rounded-[2rem] p-6">
            <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Quick View</div>
            <div className="mt-5 space-y-4">
              <InfoRow icon={Building2} label="Organization ID" value={String(organization.id)} />
              <InfoRow icon={CalendarDays} label="Joined GymMax" value={formatDate(subscriptionStatus?.joinedAt)} />
              <InfoRow icon={MapPin} label="Place" value={organization.place || "Not set"} />
              <InfoRow icon={MapPin} label="District" value={organization.district || "Not set"} />
              <InfoRow icon={Mail} label="Admin Email" value={organization.adminEmail || "Not set"} />
              <InfoRow icon={Phone} label="Admin Phone" value={organization.adminPhoneNumber || "Not set"} />
            </div>
          </section>

          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <CreditCard className="h-6 w-6 text-emerald-700" />
                Subscription Ledger
              </h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">Billing history remains separated from member payments for cleaner audit trails.</p>
            </div>
            <div className="grid gap-4 p-6 md:grid-cols-2">
              <MiniMetric label="Plan" value={planLabel(organization.subscriptionPlan)} />
              <MiniMetric label="Lifetime Revenue" value={money.format(subscriptionStatus?.lifetimeSubscriptionRevenue ?? 0)} />
              <MiniMetric label="WhatsApp Usage" value={String(subscriptionStatus?.whatsappUsage ?? 0)} />
              <MiniMetric label="Email Usage" value={String(subscriptionStatus?.emailUsage ?? 0)} />
              <MiniMetric label="Renewal Date" value={formatDate(subscriptionStatus?.subscriptionDueDate ?? organization.subscriptionDueDate)} />
            </div>
            <div className="divide-y divide-slate-100 border-t border-black/5">
              {paymentHistory.length ? (
                paymentHistory.map((payment) => (
                  <div key={payment.id} className="grid gap-3 p-5 md:grid-cols-[1fr_auto] md:items-center">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-lg font-black text-slate-950">{money.format(payment.amount || 0)}</span>
                        <Badge className={paymentBadgeClasses[payment.paymentStatus]}>{payment.paymentStatus}</Badge>
                      </div>
                      <p className="mt-2 text-xs font-semibold text-slate-500">
                        {formatDate(payment.subscriptionStart)} - {formatDate(payment.subscriptionEnd)} · Order {payment.razorpayOrderId || "manual"}
                      </p>
                    </div>
                    <div className="text-sm font-bold text-slate-700">{formatDate(payment.paymentDate)}</div>
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-sm font-semibold text-slate-500">No subscription payment records yet.</div>
              )}
              {paymentHistoryPage && paymentHistoryPage.totalElements > paymentHistory.length && (
                <div className="flex items-center justify-between gap-3 bg-slate-50/70 p-4">
                  <div className="text-sm font-bold text-slate-500">
                    Showing {paymentHistory.length} of {paymentHistoryPage.totalElements} payments
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => loadMoreHistoryMutation.mutate((paymentHistoryPage.page ?? 0) + 1)}
                    disabled={!paymentHistoryPage.hasNext || loadMoreHistoryMutation.isPending}
                    className="rounded-full border-white/80 bg-white px-5 font-black"
                  >
                    {loadMoreHistoryMutation.isPending ? "Loading..." : "Load Older Payments"}
                  </Button>
                </div>
              )}
            </div>
          </section>
        </div>
      </form>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700">{label}</Label>
      {children}
    </div>
  );
}

function SummaryCard({
  icon: Icon,
  label,
  value,
  tone,
  infoContent,
}: {
  icon: typeof CalendarDays;
  label: string;
  value: string;
  tone: "slate" | "emerald" | "rose" | "sky";
  infoContent?: string;
}) {
  const styles = {
    slate: "border-slate-200 bg-white text-slate-900",
    emerald: "border-emerald-200 bg-emerald-50 text-emerald-950",
    rose: "border-rose-200 bg-rose-50 text-rose-900",
    sky: "border-sky-200 bg-sky-50 text-sky-900",
  }[tone];

  return (
    <div className={`group relative rounded-[1.75rem] border p-5 shadow-sm ${styles}`}>
      <Icon className="h-6 w-6" />
      <div className="mt-4 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] opacity-70">
        <span>{label}</span>
        {infoContent && (
          <div className="relative">
            <div className="flex h-5 w-5 items-center justify-center rounded-full border border-current/20 bg-white/60">
              <Info className="h-3.5 w-3.5" />
            </div>
            <div className="pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 w-64 -translate-x-1/2 rounded-2xl border border-slate-200 bg-slate-950 px-3 py-2 text-center text-[11px] font-semibold normal-case tracking-normal text-white opacity-0 shadow-xl transition duration-150 group-hover:opacity-100">
              {infoContent}
            </div>
          </div>
        )}
      </div>
      <div className="mt-2 text-2xl font-black tracking-tight">{value}</div>
    </div>
  );
}

function DateTile({ label, value, attention }: { label: string; value: string; attention?: boolean }) {
  return (
    <div className={`rounded-[1.4rem] border p-4 ${attention ? "border-rose-200 bg-rose-50/80" : "border-white/80 bg-white/70"}`}>
      <div className={`text-[10px] font-black uppercase tracking-[0.22em] ${attention ? "text-rose-700" : "text-slate-500"}`}>{label}</div>
      <div className={`mt-2 text-sm font-black ${attention ? "text-rose-700" : "text-slate-950"}`}>{value}</div>
    </div>
  );
}

function InfoRow({ icon: Icon, label, value }: { icon: typeof Building2; label: string; value: string }) {
  return (
    <div className="flex items-start gap-3 rounded-[1.3rem] border border-white/80 bg-white/75 p-4 shadow-sm">
      <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-950 text-white">
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0">
        <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
        <div className="mt-1 break-words text-sm font-black text-slate-950">{value}</div>
      </div>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.3rem] border border-white/80 bg-white/75 p-4 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
      <div className="mt-2 text-lg font-black text-slate-950">{value}</div>
    </div>
  );
}
