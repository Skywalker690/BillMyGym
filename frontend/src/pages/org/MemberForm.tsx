import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, CreditCard, Sparkles, UserCog, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import api from "@/lib/api";
import { persistMemberPricing, readCachedMemberPricing, toMemberPricing, type MemberPricing } from "@/lib/memberPricingCache";
import { fetchAndCacheSubscriptionStatus, readCachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";

type MemberFormMode = "create" | "edit" | "renew";

const memberSchema = z.object({
  fullName: z.string().min(1, "Full name is required"),
  phone: z.string().optional().default(""),
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
  const [existingMember, setExistingMember] = useState(false);

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

  const { data: organizationPricing, isLoading: pricingLoading } = useQuery<MemberPricing>({
    queryKey: ["memberFormOrganizationPricing", "v2", user?.organizationId],
    queryFn: async () => {
      const organizationId = user?.organizationId ?? null;

      const cachedPricing = readCachedMemberPricing(organizationId);
      if (cachedPricing) {
        return cachedPricing;
      }

      const statusPricing = toMemberPricing(readCachedSubscriptionStatus(), organizationId);
      if (statusPricing) {
        persistMemberPricing(organizationId, statusPricing);
        return statusPricing;
      }

      try {
        const subscriptionStatus = await fetchAndCacheSubscriptionStatus();
        const liveStatusPricing = toMemberPricing(subscriptionStatus, organizationId);
        if (liveStatusPricing) {
          persistMemberPricing(organizationId, liveStatusPricing);
          return liveStatusPricing;
        }
      } catch {
        // Fall through to narrower pricing sources and the last-known-good cache.
      }

      try {
        const settingsResponse = await api.get("/settings");
        const settingsPricing = toMemberPricing(settingsResponse.data, organizationId);
        if (settingsPricing) {
          persistMemberPricing(organizationId, settingsPricing);
          return settingsPricing;
        }
      } catch {
        // Settings can be blocked in some subscription states on older backend builds.
      }

      if (organizationId) {
        try {
          const organizationResponse = await api.get(`/organizations/${organizationId}`);
          const organizationPricing = toMemberPricing(organizationResponse.data, organizationId);
          if (organizationPricing) {
            persistMemberPricing(organizationId, organizationPricing);
            return organizationPricing;
          }
        } catch {
          // Organization profile data is a final live fallback.
        }
      }

      throw new Error("Organization pricing is unavailable");
    },
    enabled: Boolean(user?.organizationId),
    retry: 1,
    staleTime: 5 * 60_000,
    refetchOnWindowFocus: false,
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
  const watchedInitialPaymentAmount = Number(watch("initialPaymentAmount") ?? 0);
  const organizationMonthlyFee = Number(organizationPricing?.monthlyMembershipFee ?? 0);
  const organizationAdmissionFee = Number(organizationPricing?.admissionFee ?? 0);
  const normalizedDurationMonths = Number.isFinite(watchedDurationMonths) && watchedDurationMonths > 0
    ? watchedDurationMonths
    : 1;
  const isBrandNewMember = isCreate && !existingMember;
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
    if (!organizationPricing || isEdit) {
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
    organizationPricing,
    setValue,
  ]);

  const mutation = useMutation({
    mutationFn: async (data: MemberFormValues) => {
      const memberPayload = {
        fullName: data.fullName,
        phone: data.phone?.trim() || null,
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
          const isAdmissionApplicable = isCreate && !existingMember && organizationAdmissionFee > 0;
          
          if (isAdmissionApplicable) {
            // Record admission fee payment
            await api.post("/payments", {
              membershipId: membership.id,
              amount: organizationAdmissionFee,
              paymentDate: new Date().toISOString().slice(0, 10),
              paymentType: "ADMISSION",
              paymentStatus: "PAID",
            }, {
              headers: {
                "X-Idempotency-Key": crypto.randomUUID(),
              },
            });
          }
          
          const membershipAmount = data.planAmount && data.planAmount > 0 ? data.planAmount : 0;
            
          if (membershipAmount > 0) {
            // Record membership fee payment
            await api.post("/payments", {
              membershipId: membership.id,
              amount: membershipAmount,
              paymentDate: new Date().toISOString().slice(0, 10),
              paymentType: "MEMBERSHIP",
              paymentStatus: "PAID",
            }, {
              headers: {
                "X-Idempotency-Key": crypto.randomUUID(),
              },
            });
          }
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
  if ((isCreate || isRenew) && pricingLoading) return <div className="p-8">Loading organization pricing...</div>;

  const currentMembership = memberships?.[0];

  return (
    <div className="mx-auto max-w-6xl space-y-5 px-3 pb-16 pt-3 sm:space-y-6 sm:p-6 lg:space-y-8 lg:p-8">
      <section className="billmygym-shell relative overflow-hidden rounded-[1.8rem] p-4 sm:rounded-[2rem] sm:p-6 lg:p-8">
        {isBrandNewMember && (
          <div className="absolute inset-0 lg:hidden bg-[radial-gradient(circle_at_top_left,_rgba(245,158,11,0.24),_transparent_42%),linear-gradient(135deg,rgba(255,251,235,0.96),rgba(254,240,138,0.5))]" />
        )}
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
            <div className={`hidden sm:flex h-16 w-16 items-center justify-center rounded-[1.7rem] bg-white/75 ${
              isBrandNewMember
                ? "text-amber-700 shadow-[0_18px_35px_-24px_rgba(217,119,6,0.5)] lg:text-emerald-700 lg:shadow-[0_18px_35px_-24px_rgba(21,128,61,0.55)]"
                : "text-emerald-700 shadow-[0_18px_35px_-24px_rgba(21,128,61,0.55)]"
            }`}>
              {isEdit ? <UserCog className="h-8 w-8" /> : <UserPlus className="h-8 w-8" />}
            </div>
            <div>
              <div className={`hidden sm:block text-[10px] font-black uppercase tracking-[0.22em] sm:tracking-[0.28em] ${
                isBrandNewMember ? "text-amber-700 lg:text-emerald-700" : "text-emerald-700"
              }`}>
                {isCreate ? "New Member Onboarding" : isEdit ? "Member Update" : "Membership Renewal"}
              </div>
              <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                {isCreate ? "Create Member" : isEdit ? "Edit Member" : "Renew Membership"}
              </h1>
            </div>
          </div>

          {(isCreate || isRenew) && (
            <div className={`hidden sm:grid w-full grid-cols-2 gap-2 sm:gap-3 lg:w-auto ${isCreate && existingMember ? "xl:grid-cols-2" : "xl:grid-cols-3"}`}>
              <HeroChip
                label="Monthly Fee"
                value={`INR ${organizationMonthlyFee.toLocaleString("en-IN")}`}
                tone={isBrandNewMember ? "amber" : "emerald"}
              />
              {(!isCreate || !existingMember) && (
                <HeroChip
                  label="Admission Fee"
                  value={`INR ${organizationAdmissionFee.toLocaleString("en-IN")}`}
                  tone={isBrandNewMember ? "amber" : "emerald"}
                />
              )}
              <HeroChip
                label="Payment Collected"
                value={`INR ${watchedInitialPaymentAmount.toLocaleString("en-IN")}`}
                className={!isCreate || !existingMember ? "col-span-2 xl:col-span-1" : ""}
                tone={isBrandNewMember ? "amber" : "emerald"}
              />
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

              <div className="grid grid-cols-2 gap-3 p-4 sm:gap-6 sm:p-6 md:grid-cols-2">
                <Field label="Full Name" error={errors.fullName?.message}>
                  <Input {...register("fullName")} disabled={isRenew} placeholder="Full Name" className="h-12 rounded-2xl border-black/5 bg-white/70 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500" />
                </Field>
                <Field label="Phone" error={errors.phone?.message}>
                  <Input {...register("phone")} disabled={isRenew} placeholder="Optional " className="h-12 rounded-2xl border-black/5 bg-white/70 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500" />
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
            <div className={`overflow-hidden rounded-[1.9rem] border shadow-[0_20px_50px_-30px_rgba(15,23,42,0.22)] ${
              isBrandNewMember
                ? "border-amber-200/70 bg-amber-50/45 lg:border-white/85 lg:bg-white/90"
                : "border-white/85 bg-white/90"
            }`}>
              <div className={`relative overflow-hidden border-b border-black/5 p-6 ${
                isBrandNewMember
                  ? "bg-[radial-gradient(circle_at_top_left,_rgba(245,158,11,0.24),_transparent_44%),linear-gradient(135deg,rgba(255,251,235,0.96),rgba(254,240,138,0.58))] lg:bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.18),_transparent_42%),linear-gradient(135deg,rgba(255,255,255,0.82),rgba(236,253,245,0.62))]"
                  : "bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.18),_transparent_42%),linear-gradient(135deg,rgba(255,255,255,0.82),rgba(236,253,245,0.62))]"
              }`}>
                <div className="relative z-10 flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <div className="flex items-center gap-3">
                      <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl shadow-sm ${
                        isBrandNewMember
                          ? "bg-amber-500/12 text-amber-700 lg:bg-emerald-500/12 lg:text-emerald-700"
                          : "bg-emerald-500/12 text-emerald-700"
                      }`}>
                        <CreditCard className="h-5 w-5" />
                      </div>
                      <div>
                        <h2 className="text-xl font-black tracking-tight text-slate-950">
                          {isCreate ? "Initial Membership" : "Membership Renewal"}
                        </h2>
                      </div>
                    </div>

                    {isCreate && (
                      <div className={`mt-5 rounded-[1.5rem] border bg-white/80 p-4 shadow-sm ${
                        isBrandNewMember
                          ? "border-amber-200 lg:border-emerald-200"
                          : "border-emerald-200"
                      }`}>
                        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                          <div>
                      
                            <div className="mt-1 text-lg font-black text-slate-950">Existing gym member</div>
                          </div>
                          <button
                            type="button"
                            onClick={() => setExistingMember((current) => !current)}
                            className={`inline-flex w-full items-center justify-between rounded-full border px-4 py-3 text-sm font-black shadow-sm transition-colors sm:min-w-[220px] sm:w-auto ${
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
                      <div className={`inline-flex items-center gap-2 rounded-full border bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] ${
                        isBrandNewMember
                          ? "border-amber-200/70 text-amber-700 lg:border-white/80 lg:text-emerald-700"
                          : "border-white/80 text-emerald-700"
                      }`}>
                        <Sparkles className="h-3.5 w-3.5" />
                        Monthly fee INR {organizationMonthlyFee.toLocaleString("en-IN")}
                      </div>
                      {(!isCreate || !existingMember) && (
                        <div className={`inline-flex items-center gap-2 rounded-full border bg-white/70 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] ${
                          isBrandNewMember
                            ? "border-amber-200/70 text-amber-700"
                            : "border-white/80 text-amber-700"
                        }`}>
                          <Sparkles className="h-3.5 w-3.5" />
                          Admission fee INR {organizationAdmissionFee.toLocaleString("en-IN")}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="hidden lg:block lg:w-[380px]">
                    <div className="rounded-[1.4rem] border border-emerald-200 bg-white/90 p-4 shadow-[0_18px_42px_-28px_rgba(5,150,105,0.65)] sm:p-5">
                      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Payment Collected</div>
                      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">
                        INR {watchedInitialPaymentAmount.toLocaleString("en-IN")}
                      </div>
                      <div className="mt-2 text-xs font-semibold leading-5 text-slate-500">
                        {isCreate ? (existingMember ? "Final membership payment" : "Final membership and admission payment") : "Final renewal payment"}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="p-6">
                <div className="grid grid-cols-[0.88fr_1.12fr] gap-3 lg:grid-cols-2 lg:gap-6">
                  <div className={`rounded-[1.45rem] border p-3 shadow-sm lg:rounded-none lg:border-0 lg:bg-transparent lg:p-0 lg:shadow-none ${
                    isBrandNewMember
                      ? "border-amber-200/80 bg-white/85 lg:border-0 lg:bg-transparent"
                      : "border-emerald-100 bg-white/85 lg:border-0 lg:bg-transparent"
                  }`}>
                    <Field label="Duration Months" >
                      <Input
                        type="number"
                        min="1"
                        {...durationMonthsField}
                        className="h-12 rounded-2xl border-black/5 bg-white/70 text-center text-lg font-black tracking-tight"
                      />
                    </Field>
                  </div>
                  <div className={`rounded-[1.45rem] border p-3 shadow-sm lg:rounded-none lg:border-0 lg:bg-transparent lg:p-0 lg:shadow-none ${
                    isBrandNewMember
                      ? "border-amber-200/80 bg-white/85 lg:border-0 lg:bg-transparent"
                      : "border-emerald-100 bg-white/85 lg:border-0 lg:bg-transparent"
                  }`}>
                    <Field label="Start Date">
                      <Input type="date" {...register("startDate")} className="h-12 rounded-2xl border-black/5 bg-white/70 min-w-0" />
                    </Field>
                  </div>
                </div>
                <input type="hidden" {...planAmountField} />
                <div className="mt-6">
                  <Field label="Payment Collected" hint="Final amount tracked ">
                    <div className={`rounded-[1.5rem] border-2 p-3 shadow-[0_18px_45px_-30px_rgba(5,150,105,0.65)] sm:p-4 ${
                      isBrandNewMember
                        ? "border-amber-300 bg-amber-50/85 shadow-[0_18px_45px_-30px_rgba(217,119,6,0.6)] lg:border-emerald-300 lg:bg-emerald-50/80 lg:shadow-[0_18px_45px_-30px_rgba(5,150,105,0.65)]"
                        : "border-emerald-300 bg-emerald-50/80"
                    }`}>
                      <Input
                        type="number"
                        min="0"
                        step="0.01"
                        {...paymentCollectedField}
                        readOnly
                        className={`h-14 rounded-2xl border-0 bg-white text-center text-2xl font-black tracking-tight shadow-sm sm:h-16 sm:text-3xl ${
                          isBrandNewMember ? "text-amber-900 lg:text-emerald-900" : "text-emerald-900"
                        }`}
                      />
                      <div className={`mt-3 text-center text-xs font-semibold leading-5 sm:text-sm ${
                        isBrandNewMember ? "text-amber-800 lg:text-emerald-800" : "text-emerald-800"
                      }`}>
                        {isCreate ? (
                          existingMember
                            ? `${normalizedDurationMonths} month${normalizedDurationMonths > 1 ? "s" : ""} membership`
                            : `${normalizedDurationMonths} month${normalizedDurationMonths > 1 ? "s" : ""} membership plus admission`
                        ) : (
                          `${normalizedDurationMonths} month${normalizedDurationMonths > 1 ? "s" : ""} renewal`
                        )}
                      </div>
                    </div>
                  </Field>
                </div>
              </div>
            </div>
          </div>
          )}
        </div>

        <div className="mt-8 flex justify-center pb-8">
          <Button 
            type="submit" 
            disabled={isSubmitting || mutation.isPending} 
            className={`w-full sm:w-auto h-14 px-12 rounded-full text-lg font-black shadow-xl transition-all ${
              isBrandNewMember 
                ? "bg-amber-500 text-amber-950 hover:bg-amber-400 border border-amber-300" 
                : "bg-emerald-600 text-white hover:bg-emerald-500 border border-emerald-500"
            }`}
          >
            {isSubmitting || mutation.isPending ? "Saving..." : isCreate ? "Create Member" : isEdit ? "Save Member" : "Renew Membership"}
          </Button>
        </div>
      </form>
    </div>
  );
}

function Field({ label, error, hint, children }: { label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
        <Label className="font-semibold text-slate-700 dark:text-slate-300">{label}</Label>
        {hint && <span className="text-[11px] font-semibold text-slate-400">{hint}</span>}
      </div>
      {children}
      {error && <p className="text-red-500 text-sm font-medium">{error}</p>}
    </div>
  );
}

function HeroChip({
  label,
  value,
  className = "",
  tone = "emerald",
}: {
  label: string;
  value: string;
  className?: string;
  tone?: "emerald" | "amber";
}) {
  return (
    <div className={`rounded-2xl border px-3 py-3 shadow-sm sm:px-4 ${
      tone === "amber"
        ? "border-amber-200/70 bg-amber-50/90 lg:border-white/80 lg:bg-white/75"
        : "border-white/80 bg-white/75"
    } ${className}`}>
      <div className={`text-[10px] font-black uppercase tracking-[0.22em] ${
        tone === "amber" ? "text-amber-700 lg:text-emerald-700" : "text-emerald-700"
      }`}>{label}</div>
      <div className="mt-1 text-base font-black text-slate-950 sm:text-lg">{value}</div>
    </div>
  );
}
