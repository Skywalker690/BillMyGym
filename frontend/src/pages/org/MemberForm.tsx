import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, CreditCard, Info, Sparkles, UserCog, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import api from "@/lib/api";

type MemberFormMode = "create" | "edit" | "renew";

const memberSchema = z.object({
  fullName: z.string().min(1, "Full name is required"),
  phone: z.string().min(1, "Phone is required"),
  status: z.enum(["ACTIVE", "DEACTIVATED"]),
  deactivationReason: z.string().optional(),
  durationMonths: z.coerce.number().min(1).optional(),
  planAmount: z.coerce.number().min(0).optional(),
  startDate: z.string().optional(),
  initialPaymentAmount: z.coerce.number().min(0).optional(),
});

type MemberFormValues = z.infer<typeof memberSchema>;
type MemberFormInput = z.input<typeof memberSchema>;

const scaleCurrency = (value: number) => Number(value.toFixed(2));
const resolveMembershipTotal = (monthlyFee: number, durationMonths: number) => scaleCurrency(monthlyFee * durationMonths);
const resolveCollectedAmount = (mode: MemberFormMode, membershipTotal: number, admissionFee: number, existingMember: boolean) =>
  mode === "create" && !existingMember ? scaleCurrency(membershipTotal + admissionFee) : scaleCurrency(membershipTotal);

export default function MemberForm({ mode = "create" }: { mode?: MemberFormMode }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const memberId = id && /^\d+$/.test(id) ? Number(id) : null;
  const isCreate = mode === "create";
  const isEdit = mode === "edit";
  const isRenew = mode === "renew";
  const canLoadMember = Boolean((isEdit || isRenew) && memberId);
  const [existingMember, setExistingMember] = useState(true);

  const { data: member, isLoading: memberLoading } = useQuery({
    queryKey: ["member", memberId],
    queryFn: async () => {
      const res = await api.get(`/members/${memberId}`);
      return res.data;
    },
    enabled: canLoadMember,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const { data: memberships } = useQuery({
    queryKey: ["memberMemberships", memberId],
    queryFn: async () => {
      const res = await api.get(`/members/${memberId}/memberships`);
      return res.data.memberships;
    },
    enabled: canLoadMember,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const { data: organization } = useQuery({
    queryKey: ["memberFormOrganization", user?.organizationId],
    queryFn: async () => {
      const res = await api.get(`/organizations/${user?.organizationId}`);
      return res.data;
    },
    enabled: Boolean(user?.organizationId),
    staleTime: 60_000,
  });

  const { register, watch, setValue, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<MemberFormInput, unknown, MemberFormValues>({
    resolver: zodResolver(memberSchema),
    defaultValues: {
      fullName: "",
      phone: "",
      status: "ACTIVE",
      deactivationReason: "",
      durationMonths: 1,
      planAmount: 0,
      startDate: new Date().toISOString().slice(0, 10),
      initialPaymentAmount: 0,
    },
  });
  const selectedStatus = watch("status");
  const watchedDurationMonths = Number(watch("durationMonths") ?? 1);
  const watchedPlanAmount = Number(watch("planAmount") ?? 0);
  const watchedInitialPaymentAmount = Number(watch("initialPaymentAmount") ?? 0);
  const organizationMonthlyFee = Number(organization?.monthlyMembershipFee ?? 0);
  const organizationAdmissionFee = Number(organization?.admissionFee ?? 0);
  const normalizedDurationMonths = Number.isFinite(watchedDurationMonths) && watchedDurationMonths > 0
    ? watchedDurationMonths
    : 1;
  const calculatedPlanAmount = resolveMembershipTotal(organizationMonthlyFee, normalizedDurationMonths);
  const calculatedCollectedAmount = resolveCollectedAmount(mode, calculatedPlanAmount, organizationAdmissionFee, existingMember);
  const durationMonthsField = register("durationMonths", { valueAsNumber: true });
  const planAmountField = register("planAmount", { valueAsNumber: true });
  const paymentCollectedField = register("initialPaymentAmount", { valueAsNumber: true });

  useEffect(() => {
    if (member && isEdit) {
      reset({
        fullName: member.fullName ?? "",
        phone: member.phone ?? "",
        status: member.status ?? "ACTIVE",
        deactivationReason: member.deactivationReason ?? "",
        durationMonths: 1,
        planAmount: 0,
        startDate: new Date().toISOString().slice(0, 10),
        initialPaymentAmount: 0,
      });
    }
    if (member && isRenew) {
      let nextStartDate = new Date().toISOString().slice(0, 10);
      if (memberships && memberships.length > 0) {
        const baseDate = new Date(`${memberships[0].endDate}T00:00:00`);
        baseDate.setDate(baseDate.getDate() + 1);
        nextStartDate = baseDate.toISOString().slice(0, 10);
      }
      
      reset({
        fullName: member.fullName ?? "",
        phone: member.phone ?? "",
        status: member.status ?? "ACTIVE",
        deactivationReason: member.deactivationReason ?? "",
        durationMonths: 1,
        planAmount: 0,
        startDate: nextStartDate,
        initialPaymentAmount: 0,
      });
    }
  }, [member, isEdit, isRenew, reset, memberships]);

  useEffect(() => {
    if (!organization || isEdit) {
      return;
    }
    setValue("planAmount", calculatedPlanAmount, { shouldDirty: false });
    setValue("initialPaymentAmount", calculatedCollectedAmount, { shouldDirty: false });
  }, [
    calculatedCollectedAmount,
    calculatedPlanAmount,
    existingMember,
    isEdit,
    mode,
    organization,
    setValue,
  ]);

  const mutation = useMutation({
    mutationFn: async (data: MemberFormValues) => {
      const memberPayload = {
        fullName: data.fullName,
        phone: data.phone,
        status: isEdit ? data.status : "ACTIVE",
        deactivationReason: isEdit && data.status === "DEACTIVATED" ? data.deactivationReason : null,
      };
      const savedMember = isCreate
        ? (await api.post("/members", memberPayload)).data
        : isEdit
          ? (await api.patch(`/members/${memberId}`, memberPayload)).data
          : member;

      if ((isCreate || isRenew) && savedMember && data.durationMonths && data.planAmount !== undefined && data.planAmount > 0) {
        const membershipResponse = await api.post(`/members/${savedMember.id}/memberships`, {
          durationMonths: data.durationMonths,
          planAmount: data.planAmount,
          startDate: data.startDate || undefined,
        }, {
          headers: {
            "X-Idempotency-Key": crypto.randomUUID(),
          },
        });
        const membership = membershipResponse.data;
        if (data.initialPaymentAmount && data.initialPaymentAmount > 0) {
          await api.post("/payments", {
            membershipId: membership.id,
            amount: data.initialPaymentAmount,
            paymentDate: new Date().toISOString().slice(0, 10),
            paymentType: isRenew ? "RENEWAL" : "NEW_MEMBERSHIP",
            paymentStatus: "PAID",
          }, {
            headers: {
              "X-Idempotency-Key": crypto.randomUUID(),
            },
          });
        }
      }

      return savedMember;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      toast.success(
        isCreate
          ? "Member created successfully"
          : isEdit
            ? "Member updated successfully"
            : "Membership renewed successfully"
      );
      navigate("/app/members");
    },
    onError: () => toast.error(
      isCreate
        ? "Failed to create member"
        : isEdit
          ? "Failed to update member"
          : "Failed to renew membership"
    ),
  });

  if ((isEdit || isRenew) && !memberId) {
    return <div className="p-8">Invalid member link.</div>;
  }

  if ((isEdit || isRenew) && memberLoading) return <div className="p-8">Loading member...</div>;

  const currentMembership = memberships?.[0];

  return (
    <div className="mx-auto max-w-6xl space-y-8 p-8 pb-16">
      <section className="billmygym-shell relative overflow-hidden rounded-[2rem] p-8">
        <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate("/app/members")}
              className="mt-1 rounded-full border border-white/70 bg-white/70 shadow-sm hover:bg-white"
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <div className="flex h-16 w-16 items-center justify-center rounded-[1.7rem] bg-white/75 text-emerald-700 shadow-[0_18px_35px_-24px_rgba(21,128,61,0.55)]">
              {isEdit ? <UserCog className="h-8 w-8" /> : <UserPlus className="h-8 w-8" />}
            </div>
            <div>
              <div className="text-[10px] font-black uppercase tracking-[0.28em] text-emerald-700">
                {isCreate ? "New Member Onboarding" : isEdit ? "Member Update" : "Membership Renewal"}
              </div>
              <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">
                {isCreate ? "Create Member" : isEdit ? "Edit Member" : "Renew Membership"}
              </h1>
            </div>
          </div>

          {(isCreate || isRenew) && (
            <div className={`grid gap-3 sm:grid-cols-2 ${isCreate && existingMember ? "xl:grid-cols-3" : "xl:grid-cols-4"}`}>
              <HeroChip label="Monthly Fee" value={`INR ${organizationMonthlyFee.toLocaleString("en-IN")}`} />
              {(!isCreate || !existingMember) && (
                <HeroChip
                  label="Admission Fee"
                  value={`INR ${organizationAdmissionFee.toLocaleString("en-IN")}`}
                />
              )}
              <HeroChip label="Total Amount" value={`INR ${watchedPlanAmount.toLocaleString("en-IN")}`} />
              <HeroChip label="Payment Collected" value={`INR ${watchedInitialPaymentAmount.toLocaleString("en-IN")}`} />
            </div>
          )}
        </div>
      </section>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
        <div className={`grid gap-6 ${isEdit ? "" : "xl:grid-cols-[1.08fr_0.92fr]"}`}>
          <div className="space-y-6">
            <div className="overflow-hidden rounded-[1.9rem] border border-white/85 bg-white/90 shadow-[0_20px_50px_-30px_rgba(15,23,42,0.22)]">
              <div className="border-b border-black/5 bg-white/20 p-6">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-sm">
                    {isEdit ? <UserCog className="h-5 w-5" /> : <UserPlus className="h-5 w-5" />}
                    </div>
                    <div className="min-w-0">
                      <h2 className="text-xl font-black tracking-tight text-slate-950">Member Identity</h2>
                    </div>
                  </div>
                  {(isEdit || isRenew) && currentMembership && (
                    <div className="rounded-2xl border border-emerald-100 bg-emerald-50/80 px-4 py-3 text-sm font-semibold text-emerald-900 sm:max-w-[360px]">
                      Current membership: {currentMembership.durationMonths} month plan | {currentMembership.status} | ends {new Date(currentMembership.endDate).toLocaleDateString()}
                    </div>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
                <Field label="Full Name" error={errors.fullName?.message}>
                  <Input {...register("fullName")} disabled={isRenew} className="h-12 rounded-2xl border-black/5 bg-white/70 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500" />
                </Field>
                <Field label="Phone" error={errors.phone?.message}>
                  <Input {...register("phone")} disabled={isRenew} className="h-12 rounded-2xl border-black/5 bg-white/70 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500" />
                </Field>
                {(isEdit || isRenew) && (
                  <Field label="Status">
                    <select {...register("status")} disabled={isRenew} className="h-12 w-full rounded-2xl border border-black/5 bg-white/70 px-4 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500">
                      <option value="ACTIVE">Active</option>
                      <option value="DEACTIVATED">Deactivated</option>
                    </select>
                  </Field>
                )}
                {selectedStatus === "DEACTIVATED" && isEdit && (
                  <div className="md:col-span-2">
                    <Field label="Deactivation Reason">
                      <Input {...register("deactivationReason")} className="h-12 rounded-2xl border-black/5 bg-white/70" />
                    </Field>
                  </div>
                )}
              </div>
            </div>
          </div>

          {(isCreate || isRenew) && (
          <div className="space-y-6">
            <div className="overflow-hidden rounded-[1.9rem] border border-white/85 bg-white/90 shadow-[0_20px_50px_-30px_rgba(15,23,42,0.22)]">
              <div className="relative overflow-hidden border-b border-black/5 bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.18),_transparent_42%),linear-gradient(135deg,rgba(255,255,255,0.82),rgba(236,253,245,0.62))] p-6">
                <div className="relative z-10 flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="flex items-center gap-3">
                      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-emerald-500/12 text-emerald-700 shadow-sm">
                        <CreditCard className="h-5 w-5" />
                      </div>
                      <div>
                        <h2 className="text-xl font-black tracking-tight text-slate-950">
                          {isCreate ? "Initial Membership" : "Membership Renewal"}
                        </h2>
                      </div>
                    </div>

                    {isCreate && (
                      <div className="mt-5 rounded-[1.5rem] border border-emerald-200 bg-white/80 p-4 shadow-sm">
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                            <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Admission Fee Control</div>
                            <div className="mt-1 text-lg font-black text-slate-950">Existing gym member</div>
                          </div>
                          <button
                            type="button"
                            onClick={() => setExistingMember((current) => !current)}
                            className={`inline-flex min-w-[220px] items-center justify-between rounded-full border px-4 py-3 text-sm font-black shadow-sm transition-colors ${
                              existingMember
                                ? "border-emerald-200 bg-emerald-100 text-emerald-800"
                                : "border-amber-200 bg-amber-50 text-amber-800"
                            }`}
                          >
                            <span>{existingMember ? "Existing Member" : "Brand New Member"}</span>
                            <span
                              className={`flex h-8 w-14 items-center rounded-full p-1 transition-colors ${
                                existingMember ? "bg-emerald-200/90 justify-end" : "bg-amber-200/90 justify-start"
                              }`}
                            >
                              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white text-slate-900 shadow-sm">
                                {existingMember ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : null}
                              </span>
                            </span>
                          </button>
                        </div>
                      </div>
                    )}

                    <div className="mt-5 flex flex-wrap items-center gap-3">
                      <div className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] text-emerald-700">
                        <Sparkles className="h-3.5 w-3.5" />
                        Monthly fee INR {organizationMonthlyFee.toLocaleString("en-IN")}
                      </div>
                      {(!isCreate || !existingMember) && (
                        <div className="inline-flex items-center gap-2 rounded-full border border-white/80 bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] text-amber-700">
                          <Sparkles className="h-3.5 w-3.5" />
                          Admission fee INR {organizationAdmissionFee.toLocaleString("en-IN")}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="grid min-w-[280px] gap-3 sm:grid-cols-2 lg:w-[360px] lg:grid-cols-1">
                    <PricingChip label="Membership Total" value={`INR ${watchedPlanAmount.toLocaleString("en-IN")}`} tone="slate" />
                    <PricingChip
                      label="Payment Collected"
                      value={`INR ${watchedInitialPaymentAmount.toLocaleString("en-IN")}`}
                      tone="emerald"
                      tooltip={isCreate ? (existingMember ? "Admission fee for existing gym member" : "Membership total plus one-time admission fee") : "Renewal collects only the membership total"}
                    />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
                <Field label="Duration Months" hint="Changing this updates the membership total and collected amount automatically.">
                  <Input
                    type="number"
                    min="1"
                    {...durationMonthsField}
                    className="h-12 rounded-2xl border-black/5 bg-white/70 text-center text-lg font-black tracking-tight"
                  />
                </Field>
                <Field
                  label="Membership Total"
                >
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    {...planAmountField}
                    readOnly
                    className="h-12 rounded-2xl border-black/5 bg-slate-100 text-slate-700"
                  />
                </Field>
                <Field label="Start Date">
                  <Input type="date" {...register("startDate")} className="h-12 rounded-2xl border-black/5 bg-white/70" />
                </Field>
                <Field
                  label="Payment Collected"
                >
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    {...paymentCollectedField}
                    readOnly
                    className="h-12 rounded-2xl border-black/5 bg-slate-100 text-slate-700"
                  />
                </Field>
              </div>
            </div>
          </div>
          )}
        </div>

        <div className="sticky bottom-4 z-20 flex flex-col gap-4 rounded-[1.8rem] border border-white/85 bg-white/92 p-5 shadow-[0_24px_60px_-30px_rgba(15,23,42,0.28)] sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Ready To Save</div>
            <p className="mt-1 text-sm font-medium text-slate-600">
              {isCreate
                ? "This will create the member record and, if pricing is filled, the initial membership and payment entry too."
                : isEdit
                  ? "This will update only the member profile details."
                  : "This will create the renewal membership and its payment entry without changing the member profile."}
            </p>
          </div>
          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={() => navigate(-1)} className="rounded-full border-white/80 bg-white/70 px-6">Cancel</Button>
            <Button type="submit" disabled={isSubmitting || mutation.isPending} className="rounded-full bg-emerald-600 px-8 font-bold text-white hover:bg-emerald-700">
            {isSubmitting || mutation.isPending ? "Saving..." : isCreate ? "Create Member" : isEdit ? "Save Member" : "Renew Membership"}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}

function Field({ label, error, hint, children }: { label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-3">
        <Label className="font-semibold text-slate-700 dark:text-slate-300">{label}</Label>
        {hint && <span className="text-[11px] font-semibold text-slate-400">{hint}</span>}
      </div>
      {children}
      {error && <p className="text-red-500 text-sm font-medium">{error}</p>}
    </div>
  );
}

function PricingChip({
  label,
  value,
  tone,
  tooltip,
}: {
  label: string;
  value: string;
  tone: "slate" | "emerald" | "amber";
  tooltip?: string;
}) {
  const toneClasses = {
    slate: "border-slate-200 bg-white/80 text-slate-900",
    emerald: "border-emerald-200 bg-emerald-50/80 text-emerald-900",
    amber: "border-amber-200 bg-amber-50/80 text-amber-900",
  }[tone];

  return (
    <div className={`rounded-2xl border px-4 py-3 shadow-sm ${toneClasses}`}>
      <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] opacity-70">
        <span>{label}</span>
        {tooltip && (
          <div className="group relative">
            <span className="flex h-4 w-4 items-center justify-center rounded-full border border-current/20 bg-white/70 text-current/80 transition-colors duration-200 hover:bg-white">
              <Info className="h-2.5 w-2.5" />
            </span>
            <div className="pointer-events-none absolute left-1/2 top-full z-20 mt-2 w-max -translate-x-1/2 rounded-full bg-slate-950 px-3 py-1.5 text-[10px] font-semibold normal-case tracking-normal text-white opacity-0 shadow-lg transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100">
              {tooltip}
            </div>
          </div>
        )}
      </div>
      <div className="mt-1 text-lg font-black">{value}</div>
    </div>
  );
}

function HeroChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/80 bg-white/75 px-4 py-3 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">{label}</div>
      <div className="mt-1 text-lg font-black text-slate-950">{value}</div>
    </div>
  );
}
