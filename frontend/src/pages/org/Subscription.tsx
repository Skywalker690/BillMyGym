import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CreditCard, IndianRupee, ShieldCheck, Sparkles, Users, Mail, MessageCircle, CalendarDays } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

type SubscriptionPayment = {
  id: number;
  amount: number;
  paymentDate?: string | null;
  subscriptionStart?: string | null;
  subscriptionEnd?: string | null;
  paymentStatus: "PAID" | "PENDING" | "FAILED" | "REFUNDED";
  razorpayOrderId?: string | null;
  razorpayPaymentId?: string | null;
  createdAt?: string | null;
};

type SubscriptionStatus = {
  organizationId: number;
  organizationName: string;
  organizationType: "REGULAR" | "PILOT";
  organizationStatus: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  subscriptionPlan: "STARTER" | "GROWTH" | "PRO" | "PILOT";
  monthlyAmount: number;
  trialEndsAt?: string | null;
  pilotEndsAt?: string | null;
  subscriptionDueDate?: string | null;
  currentPeriodStart?: string | null;
  currentPeriodEnd?: string | null;
  whatsappUsage: number;
  emailUsage: number;
  activeMembers: number;
  memberLimit?: {
    usedMembers: number;
    memberLimit?: number | null;
    unlimited: boolean;
    remainingSlots: number;
    usagePercent: number;
  };
  paymentHistory: SubscriptionPayment[];
};

type CreateOrderResponse = {
  razorpayOrderId: string;
  keyId?: string | null;
  paymentsEnabled: boolean;
  amount: number;
  amountInPaise: number;
  currency: string;
  subscriptionStart: string;
  subscriptionEnd: string;
  paymentStatus: string;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const dateText = (value?: string | null) => {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
};

const statusStyles: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 border-emerald-200",
  TRIAL: "bg-amber-100 text-amber-800 border-amber-200",
  PAUSED: "bg-orange-100 text-orange-800 border-orange-200",
  DEACTIVATED: "bg-rose-100 text-rose-800 border-rose-200",
  PILOT: "bg-sky-100 text-sky-800 border-sky-200",
  REGULAR: "bg-slate-100 text-slate-800 border-slate-200",
  STARTER: "bg-slate-100 text-slate-800 border-slate-200",
  GROWTH: "bg-emerald-100 text-emerald-800 border-emerald-200",
  PRO: "bg-indigo-100 text-indigo-800 border-indigo-200",
};

function loadRazorpayScript() {
  return new Promise<boolean>((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }

    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function Subscription() {
  const queryClient = useQueryClient();
  const [manualOrder, setManualOrder] = useState<CreateOrderResponse | null>(null);
  const [manualPaymentId, setManualPaymentId] = useState("");
  const [manualSignature, setManualSignature] = useState("");

  const { data: subscription, isLoading } = useQuery<SubscriptionStatus>({
    queryKey: ["subscriptionStatus"],
    queryFn: async () => {
      const res = await api.get("/subscription/status");
      return res.data;
    },
    staleTime: 60_000,
  });

  const verifyMutation = useMutation({
    mutationFn: (payload: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
      api.post("/subscription/verify", payload, {
        headers: {
          "X-Idempotency-Key": crypto.randomUUID(),
        },
      }),
    onSuccess: () => {
      toast.success("Subscription payment verified.");
      setManualOrder(null);
      setManualPaymentId("");
      setManualSignature("");
      queryClient.invalidateQueries({ queryKey: ["subscriptionStatus"] });
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || "Payment verification failed."),
  });

  const createOrderMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post("/subscription/create", undefined, {
        headers: {
          "X-Idempotency-Key": crypto.randomUUID(),
        },
      });
      return res.data as CreateOrderResponse;
    },
    onSuccess: async (order) => {
      setManualOrder(order);

      if (!order.paymentsEnabled) {
        toast.success("Local payment mode is enabled, so the subscription was activated without Razorpay.");
        setManualOrder(null);
        queryClient.invalidateQueries({ queryKey: ["subscriptionStatus"] });
        return;
      }

      if (!order.keyId || order.razorpayOrderId.startsWith("order_local_")) {
        toast.info("Subscription order created. Razorpay keys are not configured, so checkout is in local verification mode.");
        return;
      }

      const loaded = await loadRazorpayScript();
      if (!loaded || !window.Razorpay) {
        toast.error("Unable to load Razorpay Checkout. You can verify the payment manually after collecting payment details.");
        return;
      }

      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: "BillMyGym",
        description: "Monthly SaaS subscription",
        order_id: order.razorpayOrderId,
        handler: (response: any) => {
          verifyMutation.mutate({
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
        },
        theme: {
          color: "#16a34a",
        },
      });
      checkout.open();
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || "Failed to create subscription order."),
  });

  if (isLoading) {
    return <div className="p-8">Loading subscription...</div>;
  }

  if (!subscription) {
    return <div className="p-8">Subscription status is unavailable.</div>;
  }

  const isPilot = subscription.organizationType === "PILOT";
  const isActive = subscription.organizationStatus === "ACTIVE" && !isPilot;
  const isTrial = subscription.organizationStatus === "TRIAL";
  const isPaused = subscription.organizationStatus === "PAUSED";
  const isDeactivated = subscription.organizationStatus === "DEACTIVATED";
  const lifecycleCards = isPilot
    ? [{ label: "Pilot Access Ends", value: dateText(subscription.pilotEndsAt), emphasis: "text-sky-700" }]
    : isActive
      ? [
          {
            label: "Current Billing Period",
            value: `${dateText(subscription.currentPeriodStart)} - ${dateText(subscription.currentPeriodEnd)}`,
            emphasis: "text-emerald-700",
          },
          { label: "Next Due Date", value: dateText(subscription.subscriptionDueDate), emphasis: "text-emerald-700" },
        ]
      : isPaused
        ? [
            { label: "Subscription Expired", value: dateText(subscription.subscriptionDueDate), emphasis: "text-orange-700" },
            { label: "Renew Required", value: "Payment needed to restore access", emphasis: "text-orange-700" },
          ]
        : isDeactivated
          ? [{ label: "Organization Deactivated", value: "Contact support to restore this workspace", emphasis: "text-rose-700" }]
          : [{ label: "Trial Ends", value: dateText(subscription.trialEndsAt), emphasis: "text-amber-700" }];
  const ctaLabel = createOrderMutation.isPending
    ? "Creating order..."
    : isPilot
      ? "Pilot billing bypassed"
      : isActive
        ? "Subscription Active"
        : isTrial
          ? "Subscribe Now"
          : isPaused
            ? "Renew Subscription"
            : isDeactivated
              ? "Organization Deactivated"
              : "Subscribe Now";
  const ctaDisabled = createOrderMutation.isPending || verifyMutation.isPending || isPilot || isActive || isDeactivated;

  return (
    <div className="mx-auto max-w-6xl space-y-8 p-8">
      <div className="billmygym-shell relative overflow-hidden rounded-[2rem] p-8">
        <div className="relative z-10 grid gap-8 lg:grid-cols-[1.35fr_0.65fr] lg:items-center">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <Badge className={statusStyles[subscription.organizationStatus]}>{subscription.organizationStatus}</Badge>
              <Badge className={statusStyles[subscription.organizationType]}>{subscription.organizationType}</Badge>
              <Badge className={statusStyles[subscription.subscriptionPlan]}>{subscription.subscriptionPlan}</Badge>
            </div>
            <h1 className="text-4xl font-black tracking-tight text-slate-950">{subscription.organizationName} Subscription</h1>
            <p className="mt-3 max-w-2xl text-base font-medium text-slate-600">
              BillMyGym SaaS access is billed monthly at {money.format(subscription.monthlyAmount || 0)} on the {subscription.subscriptionPlan} plan.
            </p>
            <div className={`mt-6 grid gap-3 ${lifecycleCards.length === 1 ? "sm:grid-cols-1" : "sm:grid-cols-2"}`}>
              {lifecycleCards.map((card) => (
                <div key={card.label} className="rounded-2xl border border-white/70 bg-white/70 p-4 shadow-sm">
                  <div className={`text-[10px] font-black uppercase tracking-[0.24em] ${card.emphasis}`}>{card.label}</div>
                  <div className="mt-2 text-lg font-black text-slate-950">{card.value}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-[1.8rem] border border-emerald-100 bg-white/75 p-6 shadow-sm">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <CreditCard className="h-7 w-7" />
            </div>
            <div className="mt-5 text-sm font-black uppercase tracking-[0.24em] text-slate-500">{subscription.subscriptionPlan} Plan</div>
            <div className="mt-2 flex items-end gap-1 text-slate-950">
              <span className="text-4xl font-black">{money.format(subscription.monthlyAmount || 299)}</span>
              <span className="pb-1 text-sm font-bold text-slate-500">/ month</span>
            </div>
            <Button
              onClick={() => createOrderMutation.mutate()}
              disabled={ctaDisabled}
              className={`mt-6 h-12 w-full rounded-full font-bold text-white ${
                isActive
                  ? "bg-slate-900 hover:bg-slate-900"
                  : isDeactivated
                    ? "bg-rose-500 hover:bg-rose-500"
                    : "bg-emerald-600 hover:bg-emerald-700"
              }`}
            >
              {ctaLabel}
            </Button>
          </div>
        </div>
        <Sparkles className="absolute -right-10 -top-10 h-48 w-48 text-amber-200/40" />
      </div>

      <div className="grid gap-5 md:grid-cols-3">
        <div className="glass-panel rounded-[1.75rem] p-6">
          <Users className="h-6 w-6 text-emerald-700" />
          <div className="mt-4 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Active Members</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{subscription.activeMembers}</div>
        </div>
        <div className="glass-panel rounded-[1.75rem] p-6">
          <MessageCircle className="h-6 w-6 text-emerald-700" />
          <div className="mt-4 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">WhatsApp This Month</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{subscription.whatsappUsage}</div>
        </div>
        <div className="glass-panel rounded-[1.75rem] p-6">
          <Mail className="h-6 w-6 text-emerald-700" />
          <div className="mt-4 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Email This Month</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{subscription.emailUsage}</div>
        </div>
      </div>

      {subscription.memberLimit && (
        <div className="glass-panel rounded-[1.75rem] p-6">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Member Limit</div>
              <div className="mt-2 text-3xl font-black text-slate-950">
                {subscription.memberLimit.unlimited
                  ? `${subscription.memberLimit.usedMembers} / Unlimited`
                  : `${subscription.memberLimit.usedMembers} / ${subscription.memberLimit.memberLimit ?? 0}`}
              </div>
              <p className="mt-1 text-sm font-semibold text-slate-500">
                {subscription.memberLimit.unlimited
                  ? "Unlimited member capacity"
                  : `${subscription.memberLimit.remainingSlots} slots remaining`}
              </p>
            </div>
            {!subscription.memberLimit.unlimited && (
              <div className="w-full max-w-md">
                <div className="h-3 overflow-hidden rounded-full bg-slate-200">
                  <div
                    className="h-full rounded-full bg-emerald-600"
                    style={{ width: `${Math.min(Math.max(subscription.memberLimit.usagePercent, 0), 100)}%` }}
                  />
                </div>
                <div className="mt-2 text-right text-sm font-black text-slate-600">{subscription.memberLimit.usagePercent}%</div>
              </div>
            )}
          </div>
        </div>
      )}

      {manualOrder && (
        <div className="glass-panel rounded-[1.8rem] p-6">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-xl font-black text-slate-950">Pending Checkout Verification</h2>
              <p className="mt-1 text-sm font-medium text-slate-500">
                Order {manualOrder.razorpayOrderId} covers {dateText(manualOrder.subscriptionStart)} to {dateText(manualOrder.subscriptionEnd)}.
              </p>
            </div>
            <Badge className="bg-amber-100 text-amber-800 border-amber-200">{manualOrder.paymentStatus}</Badge>
          </div>

          <div className="mt-5 grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
            <div className="space-y-2">
              <Label htmlFor="razorpayPaymentId">Razorpay Payment ID</Label>
              <Input id="razorpayPaymentId" value={manualPaymentId} onChange={(event) => setManualPaymentId(event.target.value)} placeholder="pay_..." className="rounded-xl bg-white/75" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="razorpaySignature">Razorpay Signature</Label>
              <Input id="razorpaySignature" value={manualSignature} onChange={(event) => setManualSignature(event.target.value)} placeholder="signature" className="rounded-xl bg-white/75" />
            </div>
            <Button
              onClick={() =>
                verifyMutation.mutate({
                  razorpayOrderId: manualOrder.razorpayOrderId,
                  razorpayPaymentId: manualPaymentId,
                  razorpaySignature: manualSignature,
                })
              }
              disabled={!manualPaymentId || !manualSignature || verifyMutation.isPending}
              className="h-11 rounded-full bg-slate-950 px-6 text-white hover:bg-slate-800"
            >
              {verifyMutation.isPending ? "Verifying..." : "Verify"}
            </Button>
          </div>
        </div>
      )}

      <div className="glass-panel overflow-hidden rounded-[1.8rem]">
        <div className="border-b border-white/70 p-6">
          <h2 className="flex items-center gap-3 text-xl font-black text-slate-950">
            <ShieldCheck className="h-5 w-5 text-emerald-700" />
            Subscription Payment History
          </h2>
          <p className="mt-1 text-sm font-medium text-slate-500">Organization subscription payments are stored separately from member payments.</p>
        </div>

        <div className="divide-y divide-slate-100">
          {subscription.paymentHistory.length === 0 ? (
            <div className="p-8 text-center text-sm font-semibold text-slate-500">No subscription ledger rows yet.</div>
          ) : (
            subscription.paymentHistory.map((payment) => (
              <div key={payment.id} className="grid gap-4 p-5 md:grid-cols-[1fr_auto_auto] md:items-center">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-lg font-black text-slate-950">{money.format(payment.amount || 0)}</span>
                    <Badge className={payment.paymentStatus === "PAID" ? statusStyles.ACTIVE : payment.paymentStatus === "FAILED" ? statusStyles.DEACTIVATED : statusStyles.TRIAL}>
                      {payment.paymentStatus}
                    </Badge>
                  </div>
                  <div className="mt-2 flex flex-wrap gap-4 text-xs font-semibold text-slate-500">
                    <span className="inline-flex items-center gap-1"><CalendarDays className="h-3.5 w-3.5" /> {dateText(payment.subscriptionStart)} - {dateText(payment.subscriptionEnd)}</span>
                    <span>Order: {payment.razorpayOrderId || "manual"}</span>
                  </div>
                </div>
                <div className="text-sm font-bold text-slate-700">{dateText(payment.paymentDate)}</div>
                <div className="flex items-center gap-1 text-sm font-black text-emerald-700">
                  <IndianRupee className="h-4 w-4" />
                  SaaS Ledger
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

