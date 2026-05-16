import { useEffect } from "react";
import type { ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CreditCard, UserCog, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

const memberSchema = z.object({
  fullName: z.string().min(1, "Full name is required"),
  phone: z.string().min(1, "Phone is required"),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
  status: z.enum(["ACTIVE", "PAUSED", "DEACTIVATED"]),
  deactivationReason: z.string().optional(),
  notes: z.string().optional(),
  planName: z.string().optional(),
  durationMonths: z.coerce.number().min(1).optional(),
  planAmount: z.coerce.number().min(0).optional(),
  startDate: z.string().optional(),
  initialPaymentAmount: z.coerce.number().min(0).optional(),
  gatewayTransactionId: z.string().optional(),
});

type MemberFormValues = z.infer<typeof memberSchema>;
type MemberFormInput = z.input<typeof memberSchema>;

export default function MemberForm({ isEdit }: { isEdit?: boolean }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: member, isLoading: memberLoading } = useQuery({
    queryKey: ["member", id],
    queryFn: async () => {
      const res = await api.get(`/members/${id}`);
      return res.data;
    },
    enabled: isEdit && !!id,
  });

  const { data: memberships } = useQuery({
    queryKey: ["memberMemberships", id],
    queryFn: async () => {
      const res = await api.get(`/members/${id}/memberships`);
      return res.data.memberships;
    },
    enabled: isEdit && !!id,
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<MemberFormInput, unknown, MemberFormValues>({
    resolver: zodResolver(memberSchema),
    defaultValues: {
      fullName: "",
      phone: "",
      email: "",
      status: "ACTIVE",
      deactivationReason: "",
      notes: "",
      planName: "",
      durationMonths: 1,
      planAmount: 0,
      startDate: new Date().toISOString().slice(0, 10),
      initialPaymentAmount: 0,
      gatewayTransactionId: "",
    },
  });

  useEffect(() => {
    if (member && isEdit) {
      reset({
        fullName: member.fullName ?? "",
        phone: member.phone ?? "",
        email: member.email ?? "",
        status: member.status ?? "ACTIVE",
        deactivationReason: member.deactivationReason ?? "",
        notes: member.notes ?? "",
        planName: "",
        durationMonths: 1,
        planAmount: 0,
        startDate: new Date().toISOString().slice(0, 10),
        initialPaymentAmount: 0,
        gatewayTransactionId: "",
      });
    }
  }, [member, isEdit, reset]);

  const mutation = useMutation({
    mutationFn: async (data: MemberFormValues) => {
      const memberPayload = {
        fullName: data.fullName,
        phone: data.phone,
        email: data.email || null,
        status: data.status,
        deactivationReason: data.status === "DEACTIVATED" ? data.deactivationReason : null,
        notes: data.notes,
      };
      const memberResponse = isEdit
        ? await api.patch(`/members/${id}`, memberPayload)
        : await api.post("/members", memberPayload);
      const savedMember = memberResponse.data;

      if (data.planName && data.durationMonths && data.planAmount !== undefined && data.planAmount > 0) {
        const membershipResponse = await api.post(`/members/${savedMember.id}/memberships`, {
          planName: data.planName,
          durationMonths: data.durationMonths,
          planAmount: data.planAmount,
          startDate: data.startDate || undefined,
        });
        const membership = membershipResponse.data;
        if (data.initialPaymentAmount && data.initialPaymentAmount > 0) {
          await api.post("/payments", {
            membershipId: membership.id,
            amount: data.initialPaymentAmount,
            paymentDate: new Date().toISOString().slice(0, 10),
            paymentType: isEdit ? "RENEWAL" : "NEW_MEMBERSHIP",
            paymentStatus: data.initialPaymentAmount >= data.planAmount ? "PAID" : "PARTIAL",
            gatewayTransactionId: data.gatewayTransactionId || null,
            notes: isEdit ? "Membership payment" : "Initial membership payment",
          });
        }
      }

      return savedMember;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      toast.success(`Member ${isEdit ? "updated" : "created"} successfully`);
      navigate("/app/members");
    },
    onError: () => toast.error(`Failed to ${isEdit ? "update" : "create"} member`),
  });

  if (isEdit && memberLoading) return <div className="p-8">Loading member...</div>;

  const currentMembership = memberships?.[0];

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-6">
      <div className="flex items-center space-x-4 mb-8 bg-white/40 dark:bg-slate-900/40 p-6 rounded-2xl backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <Button variant="ghost" size="icon" onClick={() => navigate("/app/members")} className="rounded-full mr-2">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="bg-indigo-100/50 dark:bg-indigo-900/40 p-4 rounded-2xl text-indigo-600 dark:text-indigo-400 shadow-sm">
          {isEdit ? <UserCog className="w-8 h-8" /> : <UserPlus className="w-8 h-8" />}
        </div>
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">{isEdit ? "Edit Member" : "Create Member"}</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Identity is saved separately from membership history and payments.</p>
        </div>
      </div>

      <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Member Identity</h2>
            {isEdit && currentMembership && (
              <p className="text-sm text-slate-500 mt-2">
                Current membership: {currentMembership.planName} | {currentMembership.status} | ends {new Date(currentMembership.endDate).toLocaleDateString()}
              </p>
            )}
          </div>
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <Field label="Full Name" error={errors.fullName?.message}>
              <Input {...register("fullName")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Phone" error={errors.phone?.message}>
              <Input {...register("phone")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Email" error={errors.email?.message}>
              <Input type="email" {...register("email")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Status">
              <select {...register("status")} className="w-full h-11 rounded-xl border border-black/10 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 px-4 text-sm">
                <option value="ACTIVE">Active</option>
                <option value="PAUSED">Paused</option>
                <option value="DEACTIVATED">Deactivated</option>
              </select>
            </Field>
            <div className="md:col-span-2">
              <Field label="Deactivation Reason">
                <Input {...register("deactivationReason")} className="h-11 rounded-xl" />
              </Field>
            </div>
            <div className="md:col-span-2 space-y-2">
              <Label className="font-semibold text-slate-700 dark:text-slate-300">Notes</Label>
              <textarea {...register("notes")} className="w-full min-h-24 rounded-xl border border-black/10 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 p-4 text-sm" />
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5 flex items-center gap-3">
            <CreditCard className="w-5 h-5 text-emerald-600" />
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">{isEdit ? "New Membership / Renewal" : "Initial Membership"}</h2>
          </div>
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <Field label="Plan Name">
              <Input {...register("planName")} placeholder="Monthly Gym Access" className="h-11 rounded-xl" />
            </Field>
            <Field label="Duration Months">
              <Input type="number" min="1" {...register("durationMonths")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Plan Amount">
              <Input type="number" min="0" step="0.01" {...register("planAmount")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Start Date">
              <Input type="date" {...register("startDate")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Payment Collected">
              <Input type="number" min="0" step="0.01" {...register("initialPaymentAmount")} className="h-11 rounded-xl" />
            </Field>
            <Field label="Gateway Transaction ID">
              <Input {...register("gatewayTransactionId")} className="h-11 rounded-xl" />
            </Field>
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={() => navigate(-1)} className="rounded-full px-6">Cancel</Button>
          <Button type="submit" disabled={isSubmitting || mutation.isPending} className="rounded-full px-8 bg-indigo-600 hover:bg-indigo-700 text-white">
            {isSubmitting || mutation.isPending ? "Saving..." : "Save"}
          </Button>
        </div>
      </form>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700 dark:text-slate-300">{label}</Label>
      {children}
      {error && <p className="text-red-500 text-sm font-medium">{error}</p>}
    </div>
  );
}
