import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import {
  ArrowRight,
  BarChart3,
  CalendarDays,
  Check,
  CreditCard,
  FileDown,
  IndianRupee,
  LogOut,
  Mail,
  MessageCircle,
  ShieldCheck,
  Users,
} from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import api from "@/lib/api";
import { fetchAndCacheSubscriptionStatus, readCachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";
import {
  organizationStatusDescription,
  organizationStatusLabel,
  organizationTypeLabel,
  subscriptionPlanLabel,
} from "@/lib/subscriptionLabels";

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

type SubscriptionPlanKey = "BILLMYGYM";

type SubscriptionPricing = {
  baseAmount: number;
  totalAmount: number;
  activeMembers: number;
  includedMembers?: number | null;
  overageMembers: number;
  overageRate: number;
  overageAmount: number;
};

type SubscriptionInvoice = {
  baseAmount: number;
  includedActiveMembers?: number | null;
  currentActiveMembers: number;
  additionalActiveMembers: number;
  overageRate: number;
  overageAmount: number;
  totalAmount: number;
};

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
  invoice?: SubscriptionInvoice | null;
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
  organizationId: number;
  organizationName: string;
  organizationType: "REGULAR" | "DEMO";
  organizationStatus: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  subscriptionPlan: SubscriptionPlanKey;
  monthlyAmount: number;
  lifetimeSubscriptionRevenue?: number;
  trialEndsAt?: string | null;
  subscriptionDueDate?: string | null;
  currentPeriodStart?: string | null;
  currentPeriodEnd?: string | null;
  whatsappUsage: number;
  emailUsage: number;
  activeMembers: number;
  customReminderUsage?: number;
  customReminderLimit?: number | null;
  memberLimit?: {
    usedMembers: number;
    deactivatedMembers: number;
    memberLimit?: number | null;
    unlimited: boolean;
    remainingSlots: number;
    usagePercent: number;
  };
  pricing?: SubscriptionPricing;
  paymentHistory: SubscriptionPayment[];
  paymentHistoryPage?: PaymentHistoryPage;
  gymCoinBalance?: number;
};

type CreateOrderResponse = {
  razorpayOrderId: string;
  keyId?: string | null;
  paymentsEnabled: boolean;
  subscriptionPlan: SubscriptionPlanKey;
  amount: number;
  pricing?: SubscriptionPricing;
  amountInPaise: number;
  currency: string;
  subscriptionStart: string;
  subscriptionEnd: string;
  paymentStatus: string;
};

type PurchaseSuccessState = {
  amount: number;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const statusStyles: Record<string, string> = {
  ACTIVE: "border-emerald-200 bg-emerald-100 text-emerald-800",
  TRIAL: "border-amber-200 bg-amber-100 text-amber-800",
  PAUSED: "border-orange-200 bg-orange-100 text-orange-800",
  DEACTIVATED: "border-rose-200 bg-rose-100 text-rose-800",
  DEMO: "border-cyan-200 bg-cyan-100 text-cyan-800",
  REGULAR: "border-slate-200 bg-slate-100 text-slate-800",
  PAID: "border-emerald-200 bg-emerald-100 text-emerald-800",
  PENDING: "border-amber-200 bg-amber-100 text-amber-800",
  FAILED: "border-rose-200 bg-rose-100 text-rose-800",
  REFUNDED: "border-slate-200 bg-slate-100 text-slate-700",
  BILLMYGYM: "border-emerald-200 bg-emerald-100 text-emerald-800",
};

const includedFeatures = [
  { icon: Users, label: "Member and membership management" },
  { icon: CalendarDays, label: "Membership renewals and expiry tracking" },
  { icon: BarChart3, label: "Revenue dashboards, reports, analytics" },
  { icon: FileDown, label: "Custom date ranges and report exports" },
  { icon: MessageCircle, label: "WhatsApp reminders and renewal digests" },
  { icon: Mail, label: "Email reminders and monthly reports" },
];

const dateText = (value?: string | null) => {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
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

function fallbackPricing(subscription: SubscriptionStatus): SubscriptionPricing {
  const activeMembers = Number(subscription.activeMembers ?? 0);
  const includedMembers = 100;
  const overageRate = 4;
  const overageMembers = Math.max(activeMembers - includedMembers, 0);
  const overageAmount = overageMembers * overageRate;
  return {
    baseAmount: 299,
    totalAmount: 299 + overageAmount,
    activeMembers,
    includedMembers,
    overageMembers,
    overageRate,
    overageAmount,
  };
}

function invoiceFromPricing(pricing: SubscriptionPricing): SubscriptionInvoice {
  return {
    baseAmount: pricing.baseAmount,
    includedActiveMembers: pricing.includedMembers ?? 100,
    currentActiveMembers: pricing.activeMembers,
    additionalActiveMembers: pricing.overageMembers,
    overageRate: pricing.overageRate,
    overageAmount: pricing.overageAmount,
    totalAmount: pricing.totalAmount,
  };
}

export default function Subscription({ immersive = false }: { immersive?: boolean; showPlans?: boolean }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { logout } = useAuth();
  const [manualOrder, setManualOrder] = useState<CreateOrderResponse | null>(null);
  const [manualPaymentId, setManualPaymentId] = useState("");
  const [manualSignature, setManualSignature] = useState("");
  const [purchaseSuccess, setPurchaseSuccess] = useState<PurchaseSuccessState | null>(null);
  const [paymentHistory, setPaymentHistory] = useState<SubscriptionPayment[]>([]);
  const [paymentHistoryPage, setPaymentHistoryPage] = useState<PaymentHistoryPage | null>(null);
  const [useGymCoins, setUseGymCoins] = useState(false);

  const { data: subscription, isLoading } = useQuery<SubscriptionStatus | null>({
    queryKey: ["subscriptionStatus"],
    queryFn: async () => {
      const cachedStatus = readCachedSubscriptionStatus();
      if (cachedStatus) {
        return cachedStatus as SubscriptionStatus;
      }
      return await fetchAndCacheSubscriptionStatus() as SubscriptionStatus;
    },
    initialData: () => readCachedSubscriptionStatus() as SubscriptionStatus | undefined,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });

  useEffect(() => {
    if (!subscription) {
      setPaymentHistory([]);
      setPaymentHistoryPage(null);
      return;
    }
    setPaymentHistory(subscription.paymentHistory ?? []);
    setPaymentHistoryPage(
      subscription.paymentHistoryPage ?? {
        items: subscription.paymentHistory ?? [],
        page: 0,
        size: 5,
        totalElements: subscription.paymentHistory?.length ?? 0,
        totalPages: subscription.paymentHistory?.length ? 1 : 0,
        hasNext: false,
      }
    );
  }, [subscription]);

  useEffect(() => {
    if (!purchaseSuccess) {
      return;
    }
    const redirectTimer = window.setTimeout(() => {
      navigate("/app/dashboard", { replace: true });
    }, 2200);
    return () => window.clearTimeout(redirectTimer);
  }, [navigate, purchaseSuccess]);

  const verifyMutation = useMutation({
    mutationFn: (payload: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
      api.post("/subscription/verify", payload, {
        headers: {
          "X-Idempotency-Key": crypto.randomUUID(),
        },
      }),
    onSuccess: async () => {
      toast.success("Subscription payment verified.");
      setPurchaseSuccess({ amount: manualOrder?.amount ?? 0 });
      setManualOrder(null);
      setManualPaymentId("");
      setManualSignature("");
      try {
        const nextStatus = await fetchAndCacheSubscriptionStatus();
        queryClient.setQueryData(["subscriptionStatus"], nextStatus);
      } catch {
        // The successful verification response is authoritative enough for the success state.
      }
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || "Payment verification failed."),
  });

  const createOrderMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post(
        "/subscription/create",
        { subscriptionPlan: "BILLMYGYM", useGymCoins },
        {
          headers: {
            "X-Idempotency-Key": crypto.randomUUID(),
          },
        }
      );
      return res.data as CreateOrderResponse;
    },
    onSuccess: async (order) => {
      setManualOrder(order);

      if (!order.paymentsEnabled) {
        toast.success("BillMyGym activated in local payment mode.");
        setPurchaseSuccess({ amount: order.amount });
        setManualOrder(null);
        try {
          const nextStatus = await fetchAndCacheSubscriptionStatus();
          queryClient.setQueryData(["subscriptionStatus"], nextStatus);
        } catch {
          // The local activation succeeded; cache refresh can recover on the next login.
        }
        return;
      }

      if (!order.keyId || order.razorpayOrderId.startsWith("order_local_")) {
        toast.info("Subscription order created. Checkout keys are not configured, so manual verification is available.");
        return;
      }

      const loaded = await loadRazorpayScript();
      if (!loaded || !window.Razorpay) {
        toast.error("Unable to load secure checkout. Manual verification is available after payment.");
        return;
      }

      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: "BillMyGym",
        description: "BillMyGym monthly subscription",
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

  const loadMoreHistoryMutation = useMutation({
    mutationFn: async (page: number) => {
      const res = await api.get("/subscription/payments", {
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
    onError: () => toast.error("Could not load older subscription payments."),
  });

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  if (isLoading) {
    return <div className="min-h-screen overflow-x-hidden p-8">Loading subscription...</div>;
  }

  if (!subscription) {
    return <div className="min-h-screen overflow-x-hidden p-8">Subscription status is unavailable.</div>;
  }

  const pricing = subscription.pricing ?? fallbackPricing(subscription);
  const invoice = invoiceFromPricing(pricing);
  const isDemo = subscription.organizationType === "DEMO";
  const isActive = subscription.organizationStatus === "ACTIVE" && !isDemo;
  const isTrial = subscription.organizationStatus === "TRIAL";
  const hasInactiveSubscription = subscription.organizationStatus === "PAUSED" || subscription.organizationStatus === "DEACTIVATED";
  const canCheckout = !isDemo && !isActive;
  const latestPayment = paymentHistory[0];
  const paymentStatus = latestPayment?.paymentStatus ?? (isDemo ? "DEMO" : isTrial ? "TRIAL" : subscription.organizationStatus);
  const includedActiveMembers = invoice.includedActiveMembers ?? 100;
  const includedUsagePercent = includedActiveMembers > 0
    ? Math.min(Math.round((Math.min(invoice.currentActiveMembers, includedActiveMembers) / includedActiveMembers) * 100), 100)
    : 0;
  const gymCoins = Number(subscription.gymCoinBalance ?? 0);
  const estimatedCheckoutAmount = useGymCoins ? Math.max(pricing.totalAmount - gymCoins, 0) : pricing.totalAmount;
  const currentPeriod = subscription.currentPeriodStart && subscription.currentPeriodEnd
    ? `${dateText(subscription.currentPeriodStart)} - ${dateText(subscription.currentPeriodEnd)}`
    : isTrial
      ? `Trial through ${dateText(subscription.trialEndsAt ?? subscription.subscriptionDueDate)}`
      : `Renews on ${dateText(subscription.subscriptionDueDate)}`;

  if (purchaseSuccess) {
    return (
      <div className={immersive ? "min-h-screen bg-slate-950 px-4 py-8 text-white" : "space-y-8"}>
        <div className={immersive ? "mx-auto max-w-3xl rounded-[2rem] border border-white/10 bg-white/8 p-8 text-center shadow-2xl" : "rounded-[2rem] border border-emerald-100 bg-white/90 p-8 text-center shadow-sm"}>
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
            <ShieldCheck className="h-8 w-8" />
          </div>
          <Badge className="mt-6 border-emerald-200 bg-emerald-100 text-emerald-800">Payment Successful</Badge>
          <h1 className={immersive ? "mt-5 text-4xl font-black tracking-tight text-white" : "mt-5 text-4xl font-black tracking-tight text-slate-950"}>
            BillMyGym is active.
          </h1>
          <p className={immersive ? "mx-auto mt-4 max-w-2xl text-sm font-semibold leading-6 text-slate-300" : "mx-auto mt-4 max-w-2xl text-sm font-semibold leading-6 text-slate-600"}>
            Your subscription payment of {money.format(purchaseSuccess.amount)} has been processed. You are being returned to the dashboard.
          </p>
          <Button onClick={() => navigate("/app/dashboard", { replace: true })} className="mt-8 h-12 rounded-full bg-emerald-600 px-8 font-black text-white hover:bg-emerald-700">
            Enter BillMyGym
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={immersive ? "min-h-screen overflow-x-hidden bg-slate-950 px-4 py-6 text-white sm:px-6 lg:px-8" : "space-y-8"}>
      {immersive && (
        <div className="mx-auto mb-8 flex max-w-7xl items-center justify-between gap-3">
          <div className="rounded-full border border-white/10 bg-white/10 px-4 py-2 text-sm font-black tracking-[0.18em] text-emerald-100">
            BILLMYGYM
          </div>
          <Button onClick={handleLogout} variant="outline" className="rounded-full border-white/20 bg-white/10 px-4 font-bold text-white hover:bg-white/15 hover:text-white">
            <LogOut className="mr-2 h-4 w-4" />
            Log out
          </Button>
        </div>
      )}

      <div className={immersive ? "mx-auto max-w-7xl space-y-6" : "space-y-8"}>
        <section className={immersive ? "rounded-[2rem] border border-white/10 bg-white/[0.06] p-6 shadow-2xl sm:p-8" : "billmygym-shell overflow-hidden rounded-[2rem] p-6 sm:p-8"}>
          <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge className={statusStyles[subscription.organizationStatus]}>{organizationStatusLabel(subscription.organizationStatus)}</Badge>
                <Badge className={statusStyles[subscription.organizationType]}>{organizationTypeLabel(subscription.organizationType)}</Badge>
                <Badge className={statusStyles.BILLMYGYM}>{subscriptionPlanLabel(subscription.subscriptionPlan)}</Badge>
              </div>
              <h1 className={immersive ? "mt-5 max-w-3xl text-4xl font-black tracking-tight text-white sm:text-5xl" : "mt-5 max-w-3xl text-4xl font-black tracking-tight text-slate-950 sm:text-5xl"}>
                One plan. Full product access. Pricing follows active members.
              </h1>
              <p className={immersive ? "mt-4 max-w-2xl text-sm font-semibold leading-6 text-slate-300" : "mt-4 max-w-2xl text-sm font-semibold leading-6 text-slate-600"}>
                {organizationStatusDescription(subscription.organizationStatus)} BillMyGym includes every feature for every organization. Expired, inactive, frozen, cancelled, and deactivated members do not add to the subscription bill.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Button
                  onClick={() => createOrderMutation.mutate()}
                  disabled={!canCheckout || createOrderMutation.isPending || verifyMutation.isPending}
                  className="h-12 rounded-full bg-emerald-600 px-6 font-black text-white hover:bg-emerald-700"
                >
                  {isDemo
                    ? "Billing disabled for demo"
                    : isActive
                      ? "Subscription is active"
                      : createOrderMutation.isPending
                        ? "Opening checkout..."
                        : hasInactiveSubscription
                          ? `Pay ${money.format(estimatedCheckoutAmount)} and restore access`
                          : `Start for ${money.format(estimatedCheckoutAmount)}`}
                  {canCheckout && <ArrowRight className="ml-2 h-4 w-4" />}
                </Button>
                {!immersive && (
                  <Button variant="outline" onClick={() => navigate("/app/dashboard")} className="h-12 rounded-full border-white/80 bg-white/75 px-6 font-black text-slate-800 hover:bg-white">
                    Back to dashboard
                  </Button>
                )}
              </div>
            </div>

            <div className={immersive ? "rounded-[1.7rem] border border-white/10 bg-slate-900/80 p-5" : "rounded-[1.7rem] border border-white/80 bg-white/80 p-5 shadow-sm"}>
              <div className="flex items-center justify-between gap-4">
                <div>
                  <div className={immersive ? "text-[10px] font-black uppercase tracking-[0.22em] text-emerald-200" : "text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700"}>Estimated next invoice</div>
                  <div className={immersive ? "mt-2 text-5xl font-black tracking-tight text-white" : "mt-2 text-5xl font-black tracking-tight text-slate-950"}>{money.format(pricing.totalAmount)}</div>
                </div>
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <CreditCard className="h-7 w-7" />
                </div>
              </div>
              <div className={immersive ? "mt-5 grid gap-3 text-sm font-semibold text-slate-200" : "mt-5 grid gap-3 text-sm font-semibold text-slate-600"}>
                <InfoLine label="Active member usage" value={`${invoice.currentActiveMembers} / ${includedActiveMembers}`} />
                <InfoLine label="Additional member rate" value={`${money.format(invoice.overageRate)} / member`} />
                <InfoLine label="Current billing period" value={currentPeriod} />
                <InfoLine label="Payment status" value={String(paymentStatus).replaceAll("_", " ")} />
              </div>
              {gymCoins > 0 && canCheckout && (
                <label className={immersive ? "mt-5 flex items-center gap-3 rounded-2xl border border-emerald-300/20 bg-emerald-300/10 p-4 text-sm font-bold text-emerald-50" : "mt-5 flex items-center gap-3 rounded-2xl border border-emerald-100 bg-emerald-50 p-4 text-sm font-bold text-emerald-800"}>
                  <input
                    type="checkbox"
                    checked={useGymCoins}
                    onChange={(event) => setUseGymCoins(event.target.checked)}
                    className="h-4 w-4 rounded border-emerald-300 text-emerald-600 focus:ring-emerald-600"
                  />
                  Use {money.format(gymCoins)} GymCoins on checkout
                </label>
              )}
            </div>
          </div>
        </section>

        <section className="grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
          <div className={immersive ? "rounded-[1.8rem] border border-white/10 bg-white/[0.06] p-6" : "glass-panel rounded-[1.8rem] p-6"}>
            <div className={immersive ? "text-[10px] font-black uppercase tracking-[0.24em] text-emerald-200" : "text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700"}>Active member billing</div>
            <h2 className={immersive ? "mt-2 text-2xl font-black text-white" : "mt-2 text-2xl font-black text-slate-950"}>Usage snapshot</h2>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <Metric label="Active / included" value={`${invoice.currentActiveMembers} / ${includedActiveMembers}`} immersive={immersive} />
              <Metric label="Additional" value={invoice.additionalActiveMembers} immersive={immersive} />
            </div>
            <div className="mt-6 h-3 overflow-hidden rounded-full bg-slate-200/70">
              <div className="h-full rounded-full bg-emerald-600 transition-all" style={{ width: `${Math.max(includedUsagePercent, invoice.currentActiveMembers > 0 ? 6 : 0)}%` }} />
            </div>
            <div className={immersive ? "mt-3 flex flex-wrap justify-between gap-3 text-xs font-bold uppercase tracking-[0.16em] text-slate-300" : "mt-3 flex flex-wrap justify-between gap-3 text-xs font-bold uppercase tracking-[0.16em] text-slate-500"}>
              <span>{Math.min(invoice.currentActiveMembers, includedActiveMembers)} included members used</span>
              <span>{invoice.additionalActiveMembers} extra members billed</span>
            </div>
          </div>

          <div className={immersive ? "rounded-[1.8rem] border border-white/10 bg-white/[0.06] p-6" : "glass-panel rounded-[1.8rem] p-6"}>
            <div className={immersive ? "text-[10px] font-black uppercase tracking-[0.24em] text-emerald-200" : "text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700"}>Invoice breakdown</div>
            <h2 className={immersive ? "mt-2 text-2xl font-black text-white" : "mt-2 text-2xl font-black text-slate-950"}>Estimated invoice line items</h2>
            <div className="mt-5 space-y-3">
              <InvoiceRow label="Base subscription fee" value={money.format(invoice.baseAmount)} immersive={immersive} />
              <InvoiceRow label="Active member usage" value={`${invoice.currentActiveMembers} / ${includedActiveMembers}`} immersive={immersive} />
              <InvoiceRow label="Additional members" value={String(invoice.additionalActiveMembers)} immersive={immersive} />
              <InvoiceRow label="Additional charges" value={money.format(invoice.overageAmount)} immersive={immersive} />
              <InvoiceRow label="Total amount" value={money.format(invoice.totalAmount)} strong immersive={immersive} />
            </div>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {includedFeatures.map((feature) => {
            const Icon = feature.icon;
            return (
              <div key={feature.label} className={immersive ? "rounded-[1.4rem] border border-white/10 bg-white/[0.06] p-5" : "rounded-[1.4rem] border border-white/80 bg-white/80 p-5 shadow-sm"}>
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <Icon className="h-5 w-5" />
                </div>
                <div className={immersive ? "mt-4 flex items-start gap-3 text-sm font-bold text-slate-100" : "mt-4 flex items-start gap-3 text-sm font-bold text-slate-700"}>
                  <Check className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
                  <span>{feature.label}</span>
                </div>
              </div>
            );
          })}
        </section>

        {manualOrder && (
          <section className={immersive ? "rounded-[1.8rem] border border-amber-300/20 bg-amber-300/10 p-6" : "rounded-[1.8rem] border border-white/80 bg-white/85 p-6 shadow-sm backdrop-blur"}>
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className={immersive ? "text-xl font-black text-white" : "text-xl font-black text-slate-950"}>Pending checkout verification</h2>
                <p className={immersive ? "mt-1 text-sm font-medium text-slate-300" : "mt-1 text-sm font-medium text-slate-500"}>
                  Order {manualOrder.razorpayOrderId} covers {dateText(manualOrder.subscriptionStart)} to {dateText(manualOrder.subscriptionEnd)}.
                </p>
              </div>
              <Badge className={statusStyles.PENDING}>{manualOrder.paymentStatus}</Badge>
            </div>

            <div className="mt-5 grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
              <div className="space-y-2">
                <Label htmlFor="razorpayPaymentId">Gateway Payment ID</Label>
                <Input
                  id="razorpayPaymentId"
                  value={manualPaymentId}
                  onChange={(event) => setManualPaymentId(event.target.value)}
                  placeholder="pay_..."
                  className="rounded-xl bg-white/75"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="razorpaySignature">Gateway Signature</Label>
                <Input
                  id="razorpaySignature"
                  value={manualSignature}
                  onChange={(event) => setManualSignature(event.target.value)}
                  placeholder="signature"
                  className="rounded-xl bg-white/75"
                />
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
          </section>
        )}

        <section className={immersive ? "overflow-hidden rounded-[1.8rem] border border-white/10 bg-white/[0.06]" : "overflow-hidden rounded-[1.8rem] border border-white/80 bg-white/85 shadow-sm backdrop-blur"}>
          <div className={immersive ? "border-b border-white/10 p-6" : "border-b border-slate-100 p-6"}>
            <h2 className={immersive ? "flex items-center gap-3 text-xl font-black text-white" : "flex items-center gap-3 text-xl font-black text-slate-950"}>
              <ShieldCheck className="h-5 w-5 text-emerald-500" />
              Subscription payment history
            </h2>
            <p className={immersive ? "mt-1 text-sm font-medium text-slate-300" : "mt-1 text-sm font-medium text-slate-500"}>Each invoice stores its base fee, included members, overage, and total.</p>
          </div>

          <div className={immersive ? "divide-y divide-white/10" : "divide-y divide-slate-100"}>
            {paymentHistory.length === 0 ? (
              <div className={immersive ? "p-8 text-center text-sm font-semibold text-slate-300" : "p-8 text-center text-sm font-semibold text-slate-500"}>No subscription ledger rows yet.</div>
            ) : (
              paymentHistory.map((payment) => {
                const paymentInvoice = payment.invoice ?? invoiceFromPricing({
                  ...pricing,
                  totalAmount: payment.amount,
                });
                return (
                  <div key={payment.id} className="grid gap-5 p-5 lg:grid-cols-[1fr_1.2fr_auto] lg:items-center">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className={immersive ? "text-lg font-black text-white" : "text-lg font-black text-slate-950"}>{money.format(payment.amount || 0)}</span>
                        <Badge className={statusStyles[payment.paymentStatus]}>{payment.paymentStatus}</Badge>
                      </div>
                      <div className={immersive ? "mt-2 flex flex-wrap gap-4 text-xs font-semibold text-slate-300" : "mt-2 flex flex-wrap gap-4 text-xs font-semibold text-slate-500"}>
                        <span className="inline-flex items-center gap-1">
                          <CalendarDays className="h-3.5 w-3.5" /> {dateText(payment.subscriptionStart)} - {dateText(payment.subscriptionEnd)}
                        </span>
                        <span>Order: {payment.razorpayOrderId || "manual"}</span>
                      </div>
                    </div>
                    <div className="grid gap-2 sm:grid-cols-3">
                      <MiniInvoice label="Base" value={money.format(paymentInvoice.baseAmount)} immersive={immersive} />
                      <MiniInvoice label="Active" value={`${paymentInvoice.currentActiveMembers}/${paymentInvoice.includedActiveMembers ?? 100}`} immersive={immersive} />
                      <MiniInvoice label="Overage" value={money.format(paymentInvoice.overageAmount)} immersive={immersive} />
                    </div>
                    <div className={immersive ? "flex items-center gap-1 text-sm font-black text-emerald-200" : "flex items-center gap-1 text-sm font-black text-emerald-700"}>
                      <IndianRupee className="h-4 w-4" />
                      SaaS ledger
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {paymentHistoryPage && paymentHistoryPage.totalElements > paymentHistory.length && (
            <div className={immersive ? "flex flex-col justify-between gap-3 border-t border-white/10 bg-white/[0.04] p-4 sm:flex-row sm:items-center" : "flex flex-col justify-between gap-3 border-t border-slate-100 bg-slate-50/70 p-4 sm:flex-row sm:items-center"}>
              <div className={immersive ? "text-sm font-bold text-slate-300" : "text-sm font-bold text-slate-500"}>
                Showing {paymentHistory.length} of {paymentHistoryPage.totalElements} subscription payments
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => loadMoreHistoryMutation.mutate((paymentHistoryPage?.page ?? 0) + 1)}
                disabled={!paymentHistoryPage.hasNext || loadMoreHistoryMutation.isPending}
                className="rounded-full border-white/80 bg-white px-5 font-black text-slate-900"
              >
                {loadMoreHistoryMutation.isPending ? "Loading..." : "Load older payments"}
              </Button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-xl border border-current/10 px-3 py-2">
      <span className="opacity-70">{label}</span>
      <span className="text-right font-black">{value}</span>
    </div>
  );
}

function Metric({ label, value, immersive }: { label: string; value: string | number; immersive: boolean }) {
  return (
    <div className={immersive ? "rounded-[1.2rem] border border-white/10 bg-white/[0.05] p-4" : "rounded-[1.2rem] border border-white/80 bg-white/75 p-4 shadow-sm"}>
      <div className={immersive ? "text-[10px] font-black uppercase tracking-[0.2em] text-slate-300" : "text-[10px] font-black uppercase tracking-[0.2em] text-slate-500"}>{label}</div>
      <div className={immersive ? "mt-2 text-3xl font-black text-white" : "mt-2 text-3xl font-black text-slate-950"}>{value}</div>
    </div>
  );
}

function InvoiceRow({ label, value, strong = false, immersive }: { label: string; value: string; strong?: boolean; immersive: boolean }) {
  return (
    <div className={immersive ? "flex items-center justify-between gap-4 rounded-2xl border border-white/10 bg-white/[0.05] px-4 py-3" : "flex items-center justify-between gap-4 rounded-2xl border border-white/80 bg-white/80 px-4 py-3 shadow-sm"}>
      <span className={immersive ? "text-sm font-semibold text-slate-300" : "text-sm font-semibold text-slate-600"}>{label}</span>
      <span className={strong ? (immersive ? "text-xl font-black text-white" : "text-xl font-black text-slate-950") : (immersive ? "text-sm font-black text-white" : "text-sm font-black text-slate-950")}>{value}</span>
    </div>
  );
}

function MiniInvoice({ label, value, immersive }: { label: string; value: string; immersive: boolean }) {
  return (
    <div className={immersive ? "rounded-xl border border-white/10 bg-white/[0.05] px-3 py-2" : "rounded-xl border border-slate-100 bg-slate-50 px-3 py-2"}>
      <div className={immersive ? "text-[9px] font-black uppercase tracking-[0.18em] text-slate-400" : "text-[9px] font-black uppercase tracking-[0.18em] text-slate-500"}>{label}</div>
      <div className={immersive ? "mt-1 text-sm font-black text-white" : "mt-1 text-sm font-black text-slate-950"}>{value}</div>
    </div>
  );
}
