import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronDown,
  CreditCard,
  Download,
  FileSpreadsheet,
  Info,
  IndianRupee,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Sparkles,
  Trash2,
  Upload,
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
  adminFullName?: string | null;
  adminPhoneNumber?: string | null;
  adminEmail?: string | null;
  monthlyMembershipFee?: number | null;
  admissionFee?: number | null;
  organizationType?: "REGULAR" | "DEMO";
  subscriptionPlan?: "BILLMYGYM";
  status: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  trialStartDate?: string | null;
  trialEndDate?: string | null;
  subscriptionDueDate?: string | null;
  activeMembers?: number | null;
  includedActiveMembers?: number | null;
  overageMembers?: number | null;
  overageRate?: number | null;
  overageAmount?: number | null;
  estimatedMonthlyBill?: number | null;
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
  activeMembers?: number;
  pricing?: {
    baseAmount?: number;
    totalAmount?: number;
    activeMembers?: number;
    includedMembers?: number | null;
    overageMembers?: number;
    overageRate?: number;
    overageAmount?: number;
  };
  paymentHistory?: SubscriptionPayment[];
  paymentHistoryPage?: PaymentHistoryPage;
};

type BulkImportIssue = {
  field: string;
  message: string;
};

type BulkImportResponse = {
  totalRows: number;
  importedCount: number;
  limit: number;
  errors: Array<{
    rowNumber?: number | null;
    field: string;
    message: string;
  }>;
};

type FormValues = {
  name: string;
  place: string;
  district: string;
  mapLocationLink: string;
  adminName: string;
  adminPhone: string;
  adminEmail: string;
  monthlyMembershipFee: number;
  admissionFee: number;
};

const KERALA_DISTRICTS = [
  "Thiruvananthapuram",
  "Kollam",
  "Pathanamthitta",
  "Alappuzha",
  "Kottayam",
  "Idukki",
  "Ernakulam",
  "Thrissur",
  "Palakkad",
  "Malappuram",
  "Kozhikode",
  "Wayanad",
  "Kannur",
  "Kasaragod",
] as const;

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
  return plan === "BILLMYGYM" ? "BillMyGym" : "BillMyGym";
}

export default function OrganizationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [paymentHistory, setPaymentHistory] = useState<SubscriptionPayment[]>([]);
  const [paymentHistoryPage, setPaymentHistoryPage] = useState<PaymentHistoryPage | null>(null);
  const [selectedImportFile, setSelectedImportFile] = useState<File | null>(null);
  const [bulkImportSummary, setBulkImportSummary] = useState<BulkImportResponse | null>(null);
  const [bulkImportIssues, setBulkImportIssues] = useState<BulkImportIssue[]>([]);
  const [bulkImportMessage, setBulkImportMessage] = useState<string | null>(null);
  const bulkImportInputRef = useRef<HTMLInputElement | null>(null);

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

  const { register, handleSubmit, reset, watch, setValue, formState: { isSubmitting } } = useForm<FormValues>();
  const watchedDistrict = watch("district");

  useEffect(() => {
    if (!organization) return;
    reset({
      name: organization.name,
      place: organization.place ?? "",
      district: organization.district ?? "",
      mapLocationLink: organization.mapLocationLink ?? "",
      adminName: organization.adminFullName ?? "",
      adminPhone: organization.adminPhoneNumber ?? "",
      adminEmail: organization.adminEmail ?? "",
      monthlyMembershipFee: Number(organization.monthlyMembershipFee ?? 0),
      admissionFee: Number(organization.admissionFee ?? 0),
    });
  }, [organization, reset]);

  const updateMutation = useMutation({
    mutationFn: (data: FormValues) => api.patch(`/organizations/${id}`, {
      name: data.name,
      place: data.place || null,
      district: data.district,
      logoUrl: organization?.logoUrl,
      mapLocationLink: data.mapLocationLink || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      monthlyMembershipFee: data.monthlyMembershipFee,
      admissionFee: data.admissionFee,
      organizationType: organization?.organizationType,
      subscriptionPlan: organization?.subscriptionPlan,
      status: organization?.status,
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
      queryClient.invalidateQueries({ queryKey: ["platformYearlyReport"] });
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

  const bulkImportMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append("file", file);
      const res = await api.post(`/organizations/${id}/members/bulk-upload`, formData);
      return res.data as BulkImportResponse;
    },
    onSuccess: (data) => {
      setBulkImportSummary(data);
      setBulkImportIssues([]);
      setBulkImportMessage(`${data.importedCount} members were imported into this gym.`);
      setSelectedImportFile(null);
      if (bulkImportInputRef.current) {
        bulkImportInputRef.current.value = "";
      }
      toast.success(`${data.importedCount} members imported successfully`);
    },
    onError: (error: any) => {
      const responseData = error?.response?.data;
      const issues = Array.isArray(responseData?.errors) ? responseData.errors : [];
      setBulkImportSummary(null);
      setBulkImportIssues(issues);
      setBulkImportMessage(responseData?.message ?? "Bulk member upload failed.");
      toast.error(responseData?.message ?? "Bulk member upload failed.");
    },
  });

  const onSubmit = (data: FormValues) => updateMutation.mutate(data);

  const needsRenewalAttention = useMemo(() => {
    if (!organization) return false;
    if (organization.status === "PAUSED" || organization.status === "DEACTIVATED") return true;
    const targetDate =
      organization.status === "TRIAL"
          ? organization.trialEndDate ?? organization.subscriptionDueDate
          : organization.subscriptionDueDate;
    if (!targetDate) return false;
    const limit = new Date();
    limit.setDate(limit.getDate() + 7);
    return new Date(targetDate) <= limit;
  }, [organization]);

  const visibleBulkImportIssues = useMemo(() => bulkImportIssues.slice(0, 8), [bulkImportIssues]);

  if (isLoading) return <div className="p-8 text-sm font-semibold text-slate-500">Loading organization...</div>;
  if (!organization) return <div className="p-8 text-sm font-semibold text-slate-500">Organization not found.</div>;

  const money = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  });
  const pricing = subscriptionStatus?.pricing;
  const activeMembersUsed = Number(pricing?.activeMembers ?? organization.activeMembers ?? 0);
  const includedActiveMembers = pricing?.includedMembers ?? organization.includedActiveMembers ?? null;
  const overageMembers = Number(pricing?.overageMembers ?? organization.overageMembers ?? 0);
  const overageRate = Number(pricing?.overageRate ?? organization.overageRate ?? 0);
  const overageAmount = Number(pricing?.overageAmount ?? organization.overageAmount ?? 0);
  const estimatedMonthlyBill = Number(subscriptionStatus?.monthlyAmount ?? organization.estimatedMonthlyBill ?? 0);
  const basePlanAmount = Number(pricing?.baseAmount ?? 299);

  function downloadSampleCsv() {
    const csv = "fullName,phone,lastSubscriptionDate\nAarav Nair,9876543210,2026-05-20\nDiya Joseph,9123456789,20 May 2026\n";
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "billmygym-members-sample.csv";
    anchor.click();
    URL.revokeObjectURL(url);
  }

  function issueLabel(field: string) {
    const match = /^rows\[(\d+)\]\.(.+)$/.exec(field);
    if (!match) {
      return field === "file" ? "CSV file" : field;
    }
    const [, rowNumber, rowField] = match;
    const fieldLabel =
      rowField === "fullName"
        ? "Full name"
        : rowField === "phone"
          ? "Phone number"
          : rowField === "lastSubscriptionDate"
            ? "Last subscription date"
            : rowField;
    return `Row ${rowNumber} - ${fieldLabel}`;
  }

  return (
    <div className="mx-auto max-w-7xl space-y-4 p-3 sm:space-y-6 sm:p-6 lg:p-8">
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
              {organization.organizationType === "DEMO" && (
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

      <section className="grid grid-cols-2 gap-2 sm:gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          icon={CalendarDays}
          label={organization.organizationType === "DEMO" ? "Workspace Type" : "Renewal Date"}
          value={formatDate(
            organization.organizationType === "DEMO"
              ? null
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
          <section className="glass-panel overflow-visible rounded-[2rem]">
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
                <DistrictCombobox
                  value={watchedDistrict ?? ""}
                  onChange={(value) => {
                    setValue("district", value, { shouldValidate: true, shouldDirty: true });
                  }}
                />
              </Field>
              <Field label="Map Location">
                <Input
                  {...register("mapLocationLink")}
                  className="h-11 rounded-xl bg-white/70"
                  placeholder="https://maps.google.com/..."
                />
              </Field>
            </div>
          </section>

          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <ShieldCheck className="h-6 w-6 text-emerald-700" />
                Access & Billing Control
              </h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">Manage subscription state, workspace type, and visible renewal signals.</p>
            </div>
            <div className="grid gap-5 p-6 md:grid-cols-2">
              <Field label="Monthly Membership Fee (INR)">
                <Input type="number" min="0" step="0.01" {...register("monthlyMembershipFee", { valueAsNumber: true })} className="h-11 rounded-xl bg-white/70" />
              </Field>
              <Field label="Admission Fee (INR)">
                <Input type="number" min="0" step="0.01" {...register("admissionFee", { valueAsNumber: true })} className="h-11 rounded-xl bg-white/70" />
              </Field>
            </div>
            <div className="grid gap-4 border-t border-black/5 p-6 md:grid-cols-3">
              <DateTile label="Trial Window" value={`${formatDate(organization.trialStartDate)} - ${formatDate(organization.trialEndDate)}`} attention={organization.status === "TRIAL" && needsRenewalAttention} />
              <DateTile label="Workspace Type" value={organization.organizationType === "DEMO" ? "Demo Workspace" : "Regular Workspace"} attention={organization.organizationType === "DEMO"} />
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
          <section className="glass-panel overflow-hidden rounded-[2rem]">
            <div className="border-b border-black/5 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <FileSpreadsheet className="h-6 w-6 text-emerald-700" />
                Bulk Member Import
              </h2>
              <p className="mt-1 text-sm font-semibold text-slate-500">
                Super admins can import up to 200 members into this gym at a time with full name, phone number, and the member's last subscription date.
              </p>
            </div>
            <div className="space-y-5 p-6">
              <div className="rounded-[1.6rem] border border-emerald-200 bg-gradient-to-br from-emerald-50 via-white to-sky-50 p-5 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">CSV Rules</div>
                    <p className="mt-2 max-w-xl text-sm font-semibold leading-6 text-slate-600">
                      Use `fullName`, `phone`, and `lastSubscriptionDate`. The next membership expiry is calculated as the same date in the next month.
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={downloadSampleCsv}
                    className="rounded-full border-emerald-200 bg-white px-5 font-black text-emerald-700 hover:bg-emerald-50"
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Download Sample
                  </Button>
                </div>
                <div className="mt-4 rounded-[1.3rem] border border-white/80 bg-slate-950 p-4 text-xs font-semibold text-emerald-100 shadow-inner">
                  <div>fullName,phone,lastSubscriptionDate</div>
                  <div>Aarav Nair,9876543210,2026-05-20</div>
                  <div>Diya Joseph,9123456789,20 May 2026</div>
                </div>
              </div>

              <div className="rounded-[1.7rem] border border-dashed border-slate-300 bg-slate-50/80 p-5">
                <input
                  ref={bulkImportInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  className="hidden"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    setSelectedImportFile(file);
                    setBulkImportSummary(null);
                    setBulkImportIssues([]);
                    setBulkImportMessage(null);
                  }}
                />
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <div className="text-sm font-black text-slate-950">Upload file for {organization.name}</div>
                    <p className="mt-1 text-sm font-semibold text-slate-500">
                      Maximum 200 member rows per upload. Blank phone numbers and blank subscription dates are not allowed in this importer.
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <Badge className="border-slate-200 bg-white text-slate-700">3 columns only</Badge>
                      <Badge className="border-slate-200 bg-white text-slate-700">200 row limit</Badge>
                      <Badge className="border-slate-200 bg-white text-slate-700">1-month expiry calculation</Badge>
                      <Badge className="border-slate-200 bg-white text-slate-700">Org-specific phone uniqueness</Badge>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => bulkImportInputRef.current?.click()}
                      className="rounded-full border-white/80 bg-white px-5 font-black"
                    >
                      <FileSpreadsheet className="mr-2 h-4 w-4" />
                      Choose CSV
                    </Button>
                    <Button
                      type="button"
                      onClick={() => {
                        if (!selectedImportFile) {
                          toast.error("Choose a CSV file first.");
                          return;
                        }
                        bulkImportMutation.mutate(selectedImportFile);
                      }}
                      disabled={!selectedImportFile || bulkImportMutation.isPending}
                      className="rounded-full bg-emerald-600 px-5 font-black text-white hover:bg-emerald-700"
                    >
                      <Upload className="mr-2 h-4 w-4" />
                      {bulkImportMutation.isPending ? "Importing..." : "Import Members"}
                    </Button>
                  </div>
                </div>
                <div className="mt-4 rounded-[1.2rem] border border-white/80 bg-white/90 px-4 py-3 text-sm font-semibold text-slate-700">
                  {selectedImportFile ? `Selected file: ${selectedImportFile.name}` : "No CSV selected yet."}
                </div>
              </div>

              {bulkImportSummary && (
                <div className="rounded-[1.6rem] border border-emerald-200 bg-emerald-50/90 p-5 shadow-sm">
                  <div className="flex items-start gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-600 text-white">
                      <CheckCircle2 className="h-5 w-5" />
                    </div>
                    <div>
                      <div className="text-sm font-black text-emerald-900">Import completed successfully</div>
                      <p className="mt-1 text-sm font-semibold text-emerald-800">
                        {bulkImportSummary.importedCount} members were added from {bulkImportSummary.totalRows} CSV rows.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {bulkImportMessage && bulkImportIssues.length > 0 && (
                <div className="rounded-[1.6rem] border border-rose-200 bg-rose-50/90 p-5 shadow-sm">
                  <div className="flex items-start gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-rose-600 text-white">
                      <AlertCircle className="h-5 w-5" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-black text-rose-900">{bulkImportMessage}</div>
                      <div className="mt-3 space-y-2">
                        {visibleBulkImportIssues.map((issue, index) => (
                          <div key={`${issue.field}-${index}`} className="rounded-[1.1rem] border border-rose-200 bg-white/90 px-4 py-3">
                            <div className="text-xs font-black uppercase tracking-[0.22em] text-rose-700">{issueLabel(issue.field)}</div>
                            <div className="mt-1 text-sm font-semibold text-slate-700">{issue.message}</div>
                          </div>
                        ))}
                        {bulkImportIssues.length > visibleBulkImportIssues.length && (
                          <div className="text-xs font-bold text-rose-700">
                            Showing {visibleBulkImportIssues.length} of {bulkImportIssues.length} issues. Fix the file and upload again.
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section className="glass-panel rounded-[2rem] p-6">
            <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Quick View</div>
            <div className="mt-5 space-y-4">
              <InfoRow icon={Building2} label="Organization ID" value={String(organization.id)} />
              <InfoRow icon={CalendarDays} label="Joined GymMax" value={formatDate(subscriptionStatus?.joinedAt)} />
              <InfoRow icon={MapPin} label="Place" value={organization.place || "Not set"} />
              <InfoRow icon={MapPin} label="District" value={organization.district || "Not set"} />
              <InfoRow icon={IndianRupee} label="Monthly Fee" value={money.format(organization.monthlyMembershipFee ?? 0)} />
              <InfoRow icon={IndianRupee} label="Admission Fee" value={money.format(organization.admissionFee ?? 0)} />
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
              <MiniMetric
                label="Active Members"
                value={includedActiveMembers == null ? `${activeMembersUsed}` : `${activeMembersUsed}/${includedActiveMembers}`}
              />
              <MiniMetric
                label="Extra Members"
                value={overageMembers > 0 ? `${overageMembers}` : "0"}
              />
              <MiniMetric
                label="Extra Member Cost"
                value={money.format(overageAmount)}
              />
              <MiniMetric
                label="Base Plan Amount"
                value={money.format(basePlanAmount)}
              />
              <MiniMetric
                label="Estimated Monthly Bill"
                value={money.format(estimatedMonthlyBill)}
              />
            </div>
            <div className="border-t border-black/5 px-6 py-5">
              <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50/80 p-4">
                <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Member Billing Snapshot</div>
                <div className="mt-2 text-sm font-black text-slate-950">
                  {includedActiveMembers == null ? `${activeMembersUsed} active members in this workspace.` : `${activeMembersUsed}/${includedActiveMembers} active members are currently used.`}
                </div>
                <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">
                  {overageMembers > 0
                    ? `${overageMembers} extra active members are billed at ${money.format(overageRate)} each, adding ${money.format(overageAmount)} on top of the ${money.format(basePlanAmount)} base plan.`
                    : `This gym is still within its included active-member limit, so there is no extra member charge beyond the ${money.format(basePlanAmount)} base plan.`}
                </p>
              </div>
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

function DistrictCombobox({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!wrapperRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filteredDistricts = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return KERALA_DISTRICTS;
    return KERALA_DISTRICTS.filter((district) => district.toLowerCase().includes(normalized));
  }, [query]);

  return (
    <div ref={wrapperRef} className="relative z-[80]">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        className="flex h-11 w-full items-center justify-between rounded-xl border border-black/5 bg-white/70 px-4 text-left text-sm font-semibold text-slate-900 shadow-sm outline-none transition hover:border-emerald-200 focus:ring-2 focus:ring-emerald-500/40"
      >
        <span className={value ? "text-slate-900" : "text-slate-400"}>{value || "Choose a Kerala district"}</span>
        <ChevronDown className={`h-4 w-4 text-slate-400 transition ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div className="absolute left-0 top-full z-[90] mt-2 w-full overflow-hidden rounded-[1.4rem] border border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.14)]">
          <div className="border-b border-slate-100 p-3">
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search district..."
              className="h-11 rounded-xl bg-slate-50"
              autoFocus
            />
          </div>
          <div className="max-h-72 overflow-y-auto p-2">
            {filteredDistricts.length === 0 ? (
              <div className="rounded-xl px-3 py-6 text-center text-sm font-semibold text-slate-500">
                No district matches your search.
              </div>
            ) : (
              filteredDistricts.map((district) => (
                <button
                  key={district}
                  type="button"
                  onClick={() => {
                    onChange(district);
                    setQuery("");
                    setOpen(false);
                  }}
                  className="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-800"
                >
                  <span>{district}</span>
                  {value === district && <Check className="h-4 w-4 text-emerald-600" />}
                </button>
              ))
            )}
          </div>
        </div>
      )}
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
    <div className={`group relative rounded-[1.15rem] border p-3 shadow-sm sm:rounded-[1.75rem] sm:p-5 ${styles}`}>
      <Icon className="h-5 w-5 sm:h-6 sm:w-6" />
      <div className="mt-3 flex items-center gap-2 text-[9px] font-black uppercase tracking-[0.1em] opacity-70 sm:mt-4 sm:text-[10px] sm:tracking-[0.22em]">
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
      <div className="mt-1 text-lg font-black tracking-tight sm:mt-2 sm:text-2xl">{value}</div>
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
