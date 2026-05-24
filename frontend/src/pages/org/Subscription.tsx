import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import {
  ArrowRight,
  BarChart3,
  CalendarDays,
  Check,
  CreditCard,
  Crown,
  FileDown,
  Info,
  IndianRupee,
  LifeBuoy,
  LockKeyhole,
  Mail,
  MessageCircle,
  LogOut,
  ShieldCheck,
  Sparkles,
  Users,
  Zap,
  type LucideIcon,
} from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/context/AuthContext";
import api from "@/lib/api";
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

type CheckoutPlanKey = "STARTER" | "GROWTH" | "PRO";
type SubscriptionPlanKey = CheckoutPlanKey | "PILOT";

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

type PaymentHistoryPage = {
  items: SubscriptionPayment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

type SubscriptionPricing = {
  baseAmount: number;
  totalAmount: number;
  activeMembers: number;
  includedMembers?: number | null;
  overageMembers: number;
  overageRate: number;
  overageAmount: number;
};

type SubscriptionStatus = {
  organizationId: number;
  organizationName: string;
  organizationType: "REGULAR" | "PILOT" | "DEMO";
  organizationStatus: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  subscriptionPlan: SubscriptionPlanKey;
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
  subscriptionPlan: CheckoutPlanKey;
  amount: number;
  pricing?: SubscriptionPricing;
  amountInPaise: number;
  currency: string;
  subscriptionStart: string;
  subscriptionEnd: string;
  paymentStatus: string;
};

type PurchaseSuccessState = {
  plan: CheckoutPlanKey;
  amount: number;
};

type PlanCard = {
  key: CheckoutPlanKey;
  name: string;
  price: number;
  limit: string;
  tagline: string;
  description: string;
  icon: LucideIcon;
  accent: string;
  glow: string;
  badge?: string;
  features: string[];
  demos: string[];
};

const checkoutPlans: PlanCard[] = [
  {
    key: "STARTER",
    name: "Starter",
    price: 299,
    limit: "100 members",
    tagline: "Essential front-desk control",
    description: "A clean operating system for memberships, reminders, payment tracking, and the day-to-day member desk.",
    icon: Users,
    accent: "from-slate-900 to-slate-700",
    glow: "bg-slate-200",
    features: ["Member management", "Membership renewals", "Reminders", "WhatsApp and email digest", "Payment tracking"],
    demos: ["Member desk", "Daily follow-up", "Core operations"],
  },
  {
    key: "GROWTH",
    name: "Growth",
    price: 699,
    limit: "300 members",
    tagline: "Revenue visibility built in",
    description: "Adds dashboards, monthly analytics, usage insights, report delivery, and a rolling 6-month financial reporting window.",
    icon: BarChart3,
    accent: "from-emerald-600 to-teal-500",
    glow: "bg-emerald-200",
    badge: "Most Popular",
    features: ["Revenue dashboard", "Pending revenue", "Monthly analytics", "Last 6 months of reports", "WhatsApp monthly reports", "Email monthly reports"],
    demos: ["Revenue trends", "6-month reports", "Growth insights"],
  },
  {
    key: "PRO",
    name: "Pro",
    price: 1499,
    limit: "Unlimited members",
    tagline: "Operator-grade command center",
    description: "Unlocks the full workspace with unlimited scale, custom-range reports, exports, member messaging, and premium support.",
    icon: Crown,
    accent: "from-indigo-700 to-sky-500",
    glow: "bg-indigo-200",
    features: ["Everything in Growth", "Unlimited members", "Multi-admin support", "Custom date range reports", "Report exports", "Member messaging", "Premium support", "Future integrations"],
    demos: ["Unlimited scale", "Custom-range reporting", "Priority workflow"],
  },
];

const implementedDemos = [
  {
    icon: ShieldCheck,
    title: "Plan gates",
    body: "Advanced analytics, exports, monthly reports, and premium features are shown only for eligible plans.",
  },
  {
    icon: Users,
    title: "Member limits",
    body: "Member creation checks the active member count before allowing a new member.",
  },
  {
    icon: MessageCircle,
    title: "Monthly reports",
    body: "Growth gets a rolling 6-month report view, while Pro adds custom date ranges and exports.",
  },
];

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const PRO_INCLUDED_MEMBERS = 600;
const PRO_OVERAGE_RATE = 3;

const dateText = (value?: string | null) => {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
};

const statusStyles: Record<string, string> = {
  ACTIVE: "border-emerald-200 bg-emerald-100 text-emerald-800",
  TRIAL: "border-amber-200 bg-amber-100 text-amber-800",
  PAUSED: "border-orange-200 bg-orange-100 text-orange-800",
  DEACTIVATED: "border-rose-200 bg-rose-100 text-rose-800",
  PILOT: "border-sky-200 bg-sky-100 text-sky-800",
  DEMO: "border-cyan-200 bg-cyan-100 text-cyan-800",
  REGULAR: "border-slate-200 bg-slate-100 text-slate-800",
  STARTER: "border-slate-200 bg-slate-100 text-slate-800",
  GROWTH: "border-emerald-200 bg-emerald-100 text-emerald-800",
  PRO: "border-indigo-200 bg-indigo-100 text-indigo-800",
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

function preferredCheckoutPlan(subscription?: SubscriptionStatus): CheckoutPlanKey {
  if (!subscription || subscription.subscriptionPlan === "PILOT") {
    return "GROWTH";
  }
  return subscription.subscriptionPlan;
}

function estimatedPlanPrice(plan: PlanCard, subscription?: SubscriptionStatus) {
  if (plan.key !== "PRO") {
    return plan.price;
  }
  const activeMembers = subscription?.pricing?.activeMembers ?? subscription?.activeMembers ?? 0;
  return plan.price + Math.max(activeMembers - PRO_INCLUDED_MEMBERS, 0) * PRO_OVERAGE_RATE;
}

function proPricingPreview(subscription?: SubscriptionStatus) {
  const activeMembers = subscription?.pricing?.activeMembers ?? subscription?.activeMembers ?? 0;
  const overageMembers = Math.max(activeMembers - PRO_INCLUDED_MEMBERS, 0);
  return {
    activeMembers,
    includedMembers: PRO_INCLUDED_MEMBERS,
    overageMembers,
    overageRate: PRO_OVERAGE_RATE,
    overageAmount: overageMembers * PRO_OVERAGE_RATE,
    totalAmount: 1499 + overageMembers * PRO_OVERAGE_RATE,
  };
}

export default function Subscription({ immersive = false, showPlans = false }: { immersive?: boolean; showPlans?: boolean }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { logout } = useAuth();
  const [selectedPlan, setSelectedPlan] = useState<CheckoutPlanKey>("GROWTH");
  const [manualOrder, setManualOrder] = useState<CreateOrderResponse | null>(null);
  const [manualPaymentId, setManualPaymentId] = useState("");
  const [manualSignature, setManualSignature] = useState("");
  const [purchaseSuccess, setPurchaseSuccess] = useState<PurchaseSuccessState | null>(null);
  const [paymentHistory, setPaymentHistory] = useState<SubscriptionPayment[]>([]);
  const [paymentHistoryPage, setPaymentHistoryPage] = useState<PaymentHistoryPage | null>(null);
  const [useGymCoins, setUseGymCoins] = useState(false);

  const { data: subscription, isLoading } = useQuery<SubscriptionStatus>({
    queryKey: ["subscriptionStatus"],
    queryFn: async () => {
      const res = await api.get("/subscription/status");
      return res.data;
    },
    staleTime: 60_000,
  });

  useEffect(() => {
    setSelectedPlan(preferredCheckoutPlan(subscription));
  }, [subscription?.subscriptionPlan]);

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
    }, 2600);
    return () => window.clearTimeout(redirectTimer);
  }, [navigate, purchaseSuccess]);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const planRoute = immersive ? "/app/subscription/recovery/plans" : "/app/subscription/plans";
  const overviewRoute = immersive ? "/app/subscription/recovery" : "/app/subscription";

  const verifyMutation = useMutation({
    mutationFn: (payload: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
      api.post("/subscription/verify", payload, {
        headers: {
          "X-Idempotency-Key": crypto.randomUUID(),
        },
      }),
    onSuccess: () => {
      const completedPlan = manualOrder?.subscriptionPlan ?? selectedPlan;
      const completedAmount = manualOrder?.amount ?? checkoutPlans.find((plan) => plan.key === completedPlan)?.price ?? 0;
      toast.success("Subscription payment verified.");
      setPurchaseSuccess({ plan: completedPlan, amount: completedAmount });
      setManualOrder(null);
      setManualPaymentId("");
      setManualSignature("");
      queryClient.invalidateQueries({ queryKey: ["subscriptionStatus"] });
    },
    onError: (error: any) => toast.error(error?.response?.data?.message || "Payment verification failed."),
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

  const createOrderMutation = useMutation({
    mutationFn: async (plan: CheckoutPlanKey) => {
      const res = await api.post(
        "/subscription/create",
        { subscriptionPlan: plan, useGymCoins },
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
        toast.success(`${subscriptionPlanLabel(order.subscriptionPlan)} activated in local payment mode.`);
        setPurchaseSuccess({ plan: order.subscriptionPlan, amount: order.amount });
        setManualOrder(null);
        queryClient.invalidateQueries({ queryKey: ["subscriptionStatus"] });
        return;
      }

      if (!order.keyId || order.razorpayOrderId.startsWith("order_local_")) {
        toast.info("Subscription order created. Checkout keys are not configured, so manual verification is available.");
        return;
      }

      const loaded = await loadRazorpayScript();
      if (!loaded || !window.Razorpay) {
        toast.error("Unable to load secure checkout. You can verify the payment manually after collecting payment details.");
        return;
      }

      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: "BillMyGym",
        description: `${subscriptionPlanLabel(order.subscriptionPlan)} monthly SaaS subscription`,
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
    return <div className="min-h-screen overflow-x-hidden p-8">Loading subscription...</div>;
  }

  if (!subscription) {
    return <div className="min-h-screen overflow-x-hidden p-8">Subscription status is unavailable.</div>;
  }

  if (purchaseSuccess) {
    if (!immersive) {
      return (
        <div className="space-y-8">
          <div className="relative overflow-hidden rounded-[2rem] border border-emerald-100 bg-white/90 p-8 text-center shadow-sm backdrop-blur md:p-10">
            <div className="absolute -left-16 top-10 h-32 w-32 rounded-full bg-emerald-200/70 blur-3xl" />
            <div className="absolute -right-10 bottom-8 h-36 w-36 rounded-full bg-amber-200/70 blur-3xl" />
            <div className="relative z-10">
              <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-[1.75rem] bg-emerald-100 text-emerald-700 shadow-sm">
                <ShieldCheck className="h-10 w-10" />
              </div>
              <Badge className="mt-6 border-emerald-200 bg-emerald-100 text-emerald-800">Payment Successful</Badge>
              <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-950">Congratulations, your plan is live.</h1>
              <p className="mx-auto mt-4 max-w-2xl text-base font-semibold leading-7 text-slate-600">
                Your {purchaseSuccess.plan} subscription has been activated successfully for {money.format(purchaseSuccess.amount)} per month.
                We&apos;re taking you back into BillMyGym now.
              </p>
              <Button onClick={() => navigate("/app/dashboard", { replace: true })} className="mt-8 h-12 rounded-full bg-slate-950 px-8 font-black text-white hover:bg-slate-800">
                Enter BillMyGym
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div className="min-h-screen overflow-x-hidden bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.24),_transparent_28rem),radial-gradient(circle_at_bottom_right,_rgba(251,191,36,0.18),_transparent_26rem),linear-gradient(180deg,_#ecfdf5_0%,_#f8fafc_100%)] px-4 py-6 sm:px-6 lg:px-8">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-3">
          <div className="rounded-full border border-emerald-200 bg-white/80 px-4 py-2 text-sm font-black tracking-[0.18em] text-emerald-700 shadow-sm">
            BILLMYGYM
          </div>
          <Button onClick={handleLogout} variant="outline" className="rounded-full border-white/80 bg-white/80 px-4 font-bold text-slate-700">
            <LogOut className="mr-2 h-4 w-4" />
            Log out
          </Button>
        </div>

        <div className="mx-auto flex min-h-[calc(100vh-6rem)] max-w-4xl items-center justify-center">
          <div className="relative w-full overflow-hidden rounded-[2.2rem] border border-white/80 bg-white/88 p-8 text-center shadow-[0_32px_90px_rgba(15,23,42,0.14)] backdrop-blur md:p-12">
            <div className="absolute -left-16 top-10 h-32 w-32 rounded-full bg-emerald-200/70 blur-3xl" />
            <div className="absolute -right-10 bottom-8 h-36 w-36 rounded-full bg-amber-200/70 blur-3xl" />
            <div className="relative z-10">
              <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-[1.75rem] bg-emerald-100 text-emerald-700 shadow-sm">
                <ShieldCheck className="h-10 w-10" />
              </div>
              <Badge className="mt-6 border-emerald-200 bg-emerald-100 text-emerald-800">Payment Successful</Badge>
              <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-950 md:text-5xl">Congratulations, your plan is live.</h1>
              <p className="mx-auto mt-4 max-w-2xl text-base font-semibold leading-7 text-slate-600">
                Your {purchaseSuccess.plan} subscription has been activated successfully for {money.format(purchaseSuccess.amount)} per month.
                We&apos;re taking you back into BillMyGym now.
              </p>

              <div className="mx-auto mt-8 grid max-w-2xl gap-4 md:grid-cols-3">
                <div className="rounded-[1.5rem] border border-emerald-100 bg-emerald-50 p-5">
                  <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Plan</div>
                  <div className="mt-2 text-2xl font-black text-slate-950">{purchaseSuccess.plan}</div>
                </div>
                <div className="rounded-[1.5rem] border border-emerald-100 bg-emerald-50 p-5">
                  <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Amount</div>
                  <div className="mt-2 text-2xl font-black text-slate-950">{money.format(purchaseSuccess.amount)}</div>
                </div>
                <div className="rounded-[1.5rem] border border-emerald-100 bg-emerald-50 p-5">
                  <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Redirect</div>
                  <div className="mt-2 text-2xl font-black text-slate-950">2s</div>
                </div>
              </div>

              <Button onClick={() => navigate("/app/dashboard", { replace: true })} className="mt-8 h-12 rounded-full bg-slate-950 px-8 font-black text-white hover:bg-slate-800">
                Enter BillMyGym
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const isPilot = subscription.organizationType === "PILOT" || subscription.organizationType === "DEMO";
  const isActive = subscription.organizationStatus === "ACTIVE" && !isPilot;
  const isTrial = subscription.organizationStatus === "TRIAL";
  const isPaused = subscription.organizationStatus === "PAUSED";
  const isDeactivated = subscription.organizationStatus === "DEACTIVATED";
  const hasInactiveSubscription = isPaused || isDeactivated;
  const canCheckout = !isPilot && !isActive;
  const selectedPlanDetails = checkoutPlans.find((plan) => plan.key === selectedPlan) ?? checkoutPlans[1];
  const selectedPlanEstimatedPrice = estimatedPlanPrice(selectedPlanDetails, subscription);
  const proPreview = proPricingPreview(subscription);
  const isCurrentPro = subscription.subscriptionPlan === "PRO";
  const currentPlanStudioName = subscription.subscriptionPlan === "PILOT"
    ? "Founding Partner Access"
    : subscriptionPlanLabel(subscription.subscriptionPlan);
  const memberLimitLabel = subscription.memberLimit?.unlimited
    ? `${subscription.memberLimit.usedMembers} / Unlimited`
    : `${subscription.memberLimit?.usedMembers ?? subscription.activeMembers} / ${subscription.memberLimit?.memberLimit ?? 0}`;
  const lifecycleCards = isPilot
    ? [{ label: "Founding Partner Access", value: `Access Ends: ${dateText(subscription.pilotEndsAt)}`, emphasis: "text-sky-700" }]
    : isActive
      ? [
          {
            label: "Current Billing Period",
            value: `${dateText(subscription.currentPeriodStart)} - ${dateText(subscription.currentPeriodEnd)}`,
            emphasis: "text-emerald-700",
          },
          { label: "Next Renewal", value: dateText(subscription.subscriptionDueDate), emphasis: "text-emerald-700" },
        ]
      : hasInactiveSubscription
        ? [
            { label: organizationStatusLabel(subscription.organizationStatus), value: dateText(subscription.subscriptionDueDate), emphasis: "text-orange-700" },
            { label: "Select A Plan", value: "Choose below to restore access", emphasis: "text-orange-700" },
          ]
        : [{ label: "Free Trial Access", value: `Access Ends: ${dateText(subscription.trialEndsAt)}`, emphasis: "text-amber-700" }];

  const handlePlanCheckout = (plan: CheckoutPlanKey) => {
    setSelectedPlan(plan);
    if (!canCheckout) {
      return;
    }
    createOrderMutation.mutate(plan);
  };

  const buttonText = (plan: PlanCard) => {
    if (createOrderMutation.isPending && selectedPlan === plan.key) {
      return "Opening checkout...";
    }
    if (isPilot) {
      return "Included with founding partner access";
    }
    if (isActive) {
      return subscription.subscriptionPlan === plan.key ? "Active plan" : "Available at renewal";
    }
    if (isTrial) {
      return `Start ${plan.name}`;
    }
    return "Select & Pay";
  };

  return (
    <div
      className={`${
        immersive
          ? "bg-[radial-gradient(circle_at_top_left,_rgba(16,185,129,0.16),_transparent_32rem),linear-gradient(180deg,_#f8fafc_0%,_#ecfdf5_48%,_#f8fafc_100%)]"
          : "bg-transparent"
      } overflow-x-hidden pb-8 pt-4 sm:px-6 sm:pb-10 sm:pt-6 lg:px-8 lg:pb-12 lg:pt-8`}
    >
      <div className="mx-auto w-full max-w-7xl space-y-8">
        {immersive && (
          <div className="flex items-center justify-between gap-3">
            <div className="rounded-full border border-emerald-200 bg-white/80 px-4 py-2 text-sm font-black tracking-[0.18em] text-emerald-700 shadow-sm">
              BILLMYGYM
            </div>
            <Button onClick={handleLogout} variant="outline" className="rounded-full border-white/80 bg-white/80 px-4 font-bold text-slate-700">
              <LogOut className="mr-2 h-4 w-4" />
              Log out
            </Button>
          </div>
        )}

        <div className="relative overflow-hidden rounded-[2rem] border border-white/75 bg-white/80 p-6 shadow-[0_24px_80px_rgba(15,23,42,0.10)] backdrop-blur md:p-8">
          <div className={`absolute -right-24 -top-24 h-72 w-72 rounded-full ${selectedPlanDetails.glow} blur-3xl`} />
          <div className="relative z-10 grid gap-8 lg:grid-cols-[1.25fr_0.75fr] lg:items-center">
            <div>
              <div className="mb-4 flex flex-wrap items-center gap-2">
                <Badge className={statusStyles[subscription.organizationStatus]}>{organizationStatusLabel(subscription.organizationStatus)}</Badge>
                {(subscription.organizationType === "PILOT" || subscription.organizationType === "DEMO") && (
                  <Badge className={statusStyles[subscription.organizationType]}>{organizationTypeLabel(subscription.organizationType)}</Badge>
                )}
                <Badge className={statusStyles[subscription.subscriptionPlan]}>{subscriptionPlanLabel(subscription.subscriptionPlan)}</Badge>
              </div>
              <h1 className="max-w-3xl text-4xl font-black tracking-tight text-slate-950 md:text-5xl">
                {showPlans
                  ? "Choose the BillMyGym plan that fits your gym."
                  : hasInactiveSubscription
                  ? "Your subscription expired. Pick a plan to restore access."
                  : isTrial
                    ? "Choose the plan that fits your gym before the trial ends."
                    : "Manage your BillMyGym subscription."}
              </h1>
              <p className="mt-4 max-w-2xl text-base font-semibold leading-7 text-slate-600">
                {showPlans
                  ? `${subscription.organizationName} can continue with Starter, unlock rolling 6-month reports with Growth, or move to Pro for custom-range reporting and exports. Compare the plans below and continue when you're ready.`
                  : isPilot
                    ? `${subscription.organizationName} is currently running under founding partner access. Use this page to review access status, usage, and subscription guidance.`
                    : `${subscription.organizationName} can continue with Starter, unlock rolling 6-month reports with Growth, or move to Pro for custom-range reporting and exports. Use this page to review your billing status, usage, and renewal guidance.`}
              </p>

              <div className={`mt-6 grid gap-3 ${lifecycleCards.length === 1 ? "sm:grid-cols-1" : "sm:grid-cols-2"}`}>
                {lifecycleCards.map((card) => (
                  <div key={card.label} className="rounded-2xl border border-white/80 bg-white/70 p-4 shadow-sm">
                    <div className={`text-[10px] font-black uppercase tracking-[0.24em] ${card.emphasis}`}>{card.label}</div>
                    <div className="mt-2 text-lg font-black text-slate-950">{card.value}</div>
                  </div>
                ))}
              </div>
            </div>

            {showPlans ? (
              <div className="rounded-[1.8rem] border border-slate-900/5 bg-slate-950 p-6 text-white shadow-xl">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/10 text-emerald-200">
                    <LockKeyhole className="h-7 w-7" />
                  </div>
                  <Badge className="border-emerald-300/30 bg-emerald-300/10 text-emerald-100">Plan selection</Badge>
                </div>
                <div className="mt-6 text-sm font-black uppercase tracking-[0.24em] text-emerald-200">Selected Plan</div>
                <div className="mt-2 flex items-end gap-2">
                  <span className="text-4xl font-black">{selectedPlanDetails.name}</span>
                  <span className="pb-1 text-sm font-bold text-slate-300">{selectedPlanDetails.limit}</span>
                </div>
                <div className="mt-3 flex items-end gap-1">
                  <span className="text-3xl font-black">{money.format(selectedPlanEstimatedPrice)}</span>
                  <span className="pb-1 text-sm font-bold text-slate-300">/ month</span>
                </div>
                {selectedPlanDetails.key === "PRO" && (
                  <div className="mt-4 rounded-2xl border border-white/10 bg-white/[0.06] p-4 text-sm font-semibold text-slate-200">
                    <div className="flex items-center justify-between gap-3">
                      <span>Included active members</span>
                      <span className="font-black text-white">{proPreview.includedMembers}</span>
                    </div>
                    <div className="mt-2 flex items-center justify-between gap-3">
                      <span>Billable overage</span>
                      <span className="font-black text-white">
                        {proPreview.overageMembers} x {money.format(proPreview.overageRate)}
                      </span>
                    </div>
                  </div>
                )}
                <p className="mt-6 text-sm font-semibold leading-6 text-slate-200">
                  Pick the plan that fits your gym now. Once payment is verified, access is restored and the workspace is updated automatically.
                </p>
                <Button
                  onClick={() => navigate(overviewRoute)}
                  variant="outline"
                  className="mt-6 h-11 rounded-full border-white/15 bg-white/10 px-5 font-bold text-white hover:bg-white/15"
                >
                  Back to Billing Overview
                </Button>
              </div>
            ) : (
              <div className="rounded-[1.9rem] border border-emerald-100/80 bg-[linear-gradient(145deg,rgba(15,23,42,0.04),rgba(16,185,129,0.12)),linear-gradient(180deg,rgba(255,255,255,0.92),rgba(240,253,250,0.92))] p-6 shadow-[0_20px_55px_rgba(15,23,42,0.08)] backdrop-blur">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700 shadow-sm">
                  <Sparkles className="h-7 w-7" />
                </div>
                <div className="mt-6 text-sm font-black uppercase tracking-[0.24em] text-emerald-700">Subscription Studio</div>
                <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">Your current plan is {currentPlanStudioName}.</div>
                <div className="mt-5 rounded-[1.5rem] border border-emerald-200/80 bg-white/80 p-4 shadow-sm">
                  <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Currently Selected</div>
                  <div className="mt-2 flex items-end gap-2">
                    <span className="text-3xl font-black text-slate-950">
                      {subscription.subscriptionPlan === "PILOT"
                        ? currentPlanStudioName
                        : checkoutPlans.find((plan) => plan.key === subscription.subscriptionPlan)?.name ?? currentPlanStudioName}
                    </span>
                    <span className="pb-1 text-sm font-bold text-slate-500">
                      {subscription.subscriptionPlan === "PILOT"
                        ? "Priority early access"
                        : checkoutPlans.find((plan) => plan.key === subscription.subscriptionPlan)?.limit ?? "Premium early access"}
                    </span>
                  </div>
                  <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">
                    {subscription.subscriptionPlan === "PILOT"
                      ? "This workspace is currently running under founding partner access."
                      : `${money.format(subscription.pricing?.totalAmount ?? subscription.monthlyAmount)} per month is currently applied to this workspace.`}
                  </p>
                </div>
                {isCurrentPro && (
                  <div className="mt-4 rounded-[1.5rem] border border-sky-100 bg-white/80 p-4 shadow-sm">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="text-[10px] font-black uppercase tracking-[0.22em] text-sky-700">Pro Overage</div>
                        <div className="mt-1 text-sm font-semibold text-slate-600">
                          {proPreview.includedMembers} active members included, then {money.format(proPreview.overageRate)} per extra member.
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-black text-slate-950">{money.format(subscription.pricing?.overageAmount ?? proPreview.overageAmount)}</div>
                        <div className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
                          {subscription.pricing?.overageMembers ?? proPreview.overageMembers} extra
                        </div>
                      </div>
                    </div>
                  </div>
                )}
                <Button
                  onClick={() => navigate(planRoute)}
                  className="mt-6 h-12 rounded-full bg-emerald-600 px-6 font-black text-white shadow-[0_16px_35px_rgba(5,150,105,0.24)] hover:bg-emerald-700"
                >
                  View Subscription Plans
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            )}
          </div>
          <Sparkles className="absolute bottom-6 right-8 h-14 w-14 text-amber-300/50" />
        </div>

        {!showPlans && (
          <div className="grid gap-5 md:grid-cols-3">
          <div className="rounded-[1.75rem] border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur">
            <Users className="h-6 w-6 text-emerald-700" />
            <div className="mt-4 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">
              <span>Members Used</span>
              <div className="group relative">
                <button
                  type="button"
                  className="inline-flex h-5 w-5 items-center justify-center rounded-full border border-emerald-200 bg-white/85 text-emerald-700 shadow-sm"
                  aria-label="Member usage info"
                >
                  <Info className="h-3.5 w-3.5" />
                </button>
                <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-3 w-56 -translate-x-1/2 rounded-2xl border border-emerald-100 bg-slate-950 px-4 py-3 text-left text-[11px] font-bold normal-case tracking-normal text-white opacity-0 shadow-xl transition-opacity duration-200 group-hover:opacity-100">
                  {subscription.memberLimit.deactivatedMembers} deactivated members are included in this usage limit.
                </div>
              </div>
            </div>
            <div className="mt-2 text-3xl font-black text-slate-950">{memberLimitLabel}</div>
              {subscription.memberLimit && !subscription.memberLimit.unlimited && (
                <>
                  <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-200">
                    <div
                      className="h-full rounded-full bg-emerald-600"
                      style={{ width: `${Math.min(Math.max(subscription.memberLimit.usagePercent, 0), 100)}%` }}
                    />
                  </div>
                  <p className="mt-2 text-sm font-bold text-slate-500">{subscription.memberLimit.remainingSlots} slots remaining</p>
                </>
              )}
            </div>
            <div className="rounded-[1.75rem] border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur">
              <MessageCircle className="h-6 w-6 text-emerald-700" />
              <div className="mt-4 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">WhatsApp This Month</div>
              <div className="mt-2 text-3xl font-black text-slate-950">{subscription.whatsappUsage}</div>
              <p className="mt-2 text-sm font-bold text-slate-500">Digest and report usage visibility.</p>
            </div>
            <div className="rounded-[1.75rem] border border-white/80 bg-white/80 p-6 shadow-sm backdrop-blur">
              <Mail className="h-6 w-6 text-emerald-700" />
              <div className="mt-4 text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Email This Month</div>
              <div className="mt-2 text-3xl font-black text-slate-950">{subscription.emailUsage}</div>
              <p className="mt-2 text-sm font-bold text-slate-500">Monthly communication count.</p>
            </div>
          </div>
        )}

        {showPlans && (
          <div className="grid gap-5 xl:grid-cols-3">
            {checkoutPlans.map((plan) => {
              const Icon = plan.icon;
              const isSelected = selectedPlan === plan.key;
              const isCurrent = subscription.subscriptionPlan === plan.key;
              const isPro = plan.key === "PRO";
              const isGrowth = plan.key === "GROWTH";
              const isStarter = plan.key === "STARTER";
              const planMonthlyPrice = estimatedPlanPrice(plan, subscription);
              return (
                <div
                  key={plan.key}
                  className={`group relative flex min-h-full flex-col overflow-hidden rounded-[2.2rem] border p-6 shadow-[0_18px_50px_rgba(15,23,42,0.08)] backdrop-blur transition duration-500 ${
                    isPro
                      ? "bg-[radial-gradient(circle_at_top,rgba(99,102,241,0.2),transparent_32%),linear-gradient(180deg,rgba(15,23,42,0.98),rgba(30,41,59,0.94))] text-white border-indigo-200/30"
                      : isGrowth
                        ? "bg-[radial-gradient(circle_at_top,rgba(16,185,129,0.14),transparent_30%),linear-gradient(180deg,rgba(255,255,255,0.96),rgba(236,253,245,0.96))] text-slate-950 border-emerald-100/80"
                        : "bg-[radial-gradient(circle_at_top,rgba(148,163,184,0.12),transparent_30%),linear-gradient(180deg,rgba(255,255,255,0.96),rgba(248,250,252,0.98))] text-slate-950 border-slate-200/80"
                  } ${
                    isSelected
                      ? isPro
                        ? "border-sky-300/60 ring-4 ring-sky-200/30 -translate-y-1"
                        : isGrowth
                          ? "border-emerald-400 ring-4 ring-emerald-100 -translate-y-1"
                          : "border-slate-400 ring-4 ring-slate-200 -translate-y-1"
                      : isPro
                        ? "border-white/10 hover:-translate-y-2 hover:shadow-[0_26px_70px_rgba(30,41,59,0.28)]"
                        : isGrowth
                          ? "hover:-translate-y-2 hover:shadow-[0_26px_60px_rgba(5,150,105,0.16)]"
                          : "hover:-translate-y-2 hover:shadow-[0_26px_60px_rgba(51,65,85,0.14)]"
                  }`}
                >
                  <div className={`absolute inset-x-0 top-0 h-1.5 bg-gradient-to-r ${plan.accent}`} />
                  <div
                    className={`pointer-events-none absolute inset-0 opacity-0 transition duration-500 group-hover:opacity-100 ${
                      isPro
                        ? "bg-[radial-gradient(circle_at_top_right,rgba(125,211,252,0.18),transparent_24%),radial-gradient(circle_at_bottom_left,rgba(129,140,248,0.18),transparent_26%)]"
                        : isGrowth
                          ? "bg-[radial-gradient(circle_at_top_right,rgba(16,185,129,0.12),transparent_22%),radial-gradient(circle_at_bottom_left,rgba(20,184,166,0.12),transparent_26%)]"
                          : "bg-[radial-gradient(circle_at_top_right,rgba(148,163,184,0.12),transparent_22%),radial-gradient(circle_at_bottom_left,rgba(71,85,105,0.08),transparent_26%)]"
                    }`}
                  />
                  <div className="relative z-10 flex items-start justify-between gap-4">
                    <div className={`flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br ${plan.accent} text-white shadow-lg`}>
                      <Icon className="h-7 w-7" />
                    </div>
                    <div className="flex flex-wrap justify-end gap-2">
                      {plan.badge && <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">{plan.badge}</Badge>}
                      {isCurrent && <Badge className={statusStyles[plan.key]}>Current</Badge>}
                    </div>
                  </div>

                  <div className="relative z-10 mt-6">
                    <div className={`text-sm font-black uppercase tracking-[0.24em] ${isPro ? "text-sky-200" : "text-slate-500"}`}>{plan.tagline}</div>
                    <div className="mt-3 flex items-center justify-between gap-4">
                      <h2 className={`text-3xl font-black ${isPro ? "text-white" : "text-slate-950"}`}>{plan.name}</h2>
                      <div className="group/limit relative">
                        <div className={`flex items-center gap-2 rounded-full px-3 py-1 text-[11px] font-black uppercase tracking-[0.18em] ${isPro ? "bg-white/10 text-sky-100 ring-1 ring-white/10" : "bg-slate-100 text-slate-700"}`}>
                          <span>{plan.limit}</span>
                          {isPro && (
                            <span className="inline-flex h-5 w-5 items-center justify-center rounded-full border border-white/12 bg-white/10 text-sky-100">
                              <Info className="h-3.5 w-3.5" />
                            </span>
                          )}
                        </div>
                        {isPro && (
                          <div className="pointer-events-none absolute bottom-full right-0 z-20 mb-3 w-72 translate-y-1 rounded-2xl border border-sky-200/20 bg-slate-950/96 p-4 text-left opacity-0 shadow-2xl backdrop-blur transition-all duration-200 group-hover/limit:translate-y-0 group-hover/limit:opacity-100">
                            <div className="text-[10px] font-black uppercase tracking-[0.2em] text-sky-200">Member Capacity</div>
                            <div className="mt-2 text-sm font-black text-white">Unlimited members</div>
                            <div className="mt-2 text-xs font-semibold leading-5 text-slate-300">
                              Base pricing covers up to 600 active members. After that, each additional active member is charged at INR 3 per month.
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                    <p className={`mt-3 min-h-[4.5rem] text-sm font-semibold leading-6 ${isPro ? "text-slate-200" : "text-slate-600"}`}>{plan.description}</p>
                    <div className={`mt-5 overflow-hidden rounded-[1.6rem] border p-4 ${
                      isPro
                        ? "border-white/10 bg-white/6"
                        : isGrowth
                          ? "border-emerald-100 bg-white/70 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)]"
                          : "border-slate-200 bg-white/78 shadow-[inset_0_1px_0_rgba(255,255,255,0.85)]"
                    }`}>
                      <div className={`text-[10px] font-black uppercase tracking-[0.24em] ${
                        isPro ? "text-sky-200" : isGrowth ? "text-emerald-700" : "text-slate-500"
                      }`}>Monthly Price</div>
                      <div className="mt-2 flex items-end gap-2">
                        <span className={`text-4xl font-black tracking-tight ${isPro ? "text-white" : "text-slate-950"}`}>{money.format(planMonthlyPrice)}</span>
                        <span className={`pb-1 text-sm font-bold ${isPro ? "text-slate-300" : "text-slate-500"}`}>/ month</span>
                      </div>
                      {isPro && (
                        <div className={`mt-3 rounded-2xl border px-3 py-2 text-xs font-bold leading-5 ${
                          proPreview.overageMembers > 0
                            ? "border-sky-300/20 bg-sky-300/10 text-sky-50"
                            : "border-white/10 bg-white/[0.04] text-slate-300"
                        }`}>
                          {proPreview.overageMembers > 0
                            ? `${proPreview.overageMembers} extra active members adds ${money.format(proPreview.overageAmount)} this month.`
                            : `Base price includes your first ${proPreview.includedMembers} active members.`}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="relative z-10 mt-6 grid gap-3">
                    {plan.features.map((feature) => (
                      <div
                        key={feature}
                        className={`flex items-start gap-3 rounded-[1.1rem] border px-3 py-3 text-sm font-semibold transition duration-300 ${
                          isPro
                            ? "border-white/8 bg-white/[0.04] text-slate-100 group-hover:bg-white/[0.06]"
                            : isGrowth
                              ? "border-emerald-100 bg-white/72 text-slate-700 group-hover:bg-white"
                              : "border-slate-200 bg-white/80 text-slate-700 group-hover:bg-white"
                        }`}
                      >
                        <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${
                          isPro ? "bg-sky-400/15 text-sky-200" : isGrowth ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-700"
                        }`}>
                          <Check className="h-3.5 w-3.5" />
                        </span>
                        {feature}
                      </div>
                    ))}
                  </div>

                  <div className={`relative z-10 mt-6 rounded-[1.6rem] border p-4 ${
                    isPro
                      ? "border-white/10 bg-white/[0.05]"
                      : isGrowth
                        ? "border-emerald-100 bg-emerald-50/60"
                        : "border-slate-200 bg-slate-50/90"
                  }`}>
                    <div className={`text-[10px] font-black uppercase tracking-[0.22em] ${
                      isPro ? "text-sky-200" : isGrowth ? "text-emerald-700" : "text-slate-500"
                    }`}>Best For</div>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {plan.demos.map((demo) => (
                        <span
                          key={demo}
                          className={`rounded-full px-3 py-1 text-xs font-black shadow-sm transition duration-300 ${
                            isPro
                              ? "bg-white/10 text-sky-50 ring-1 ring-white/10"
                              : isGrowth
                                ? "bg-white text-emerald-800"
                                : "bg-white text-slate-700"
                          }`}
                        >
                          {demo}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="relative z-10 mt-6">
                    <Button
                      onClick={() => handlePlanCheckout(plan.key)}
                      disabled={!canCheckout || createOrderMutation.isPending || verifyMutation.isPending}
                      className={`h-12 w-full rounded-full font-black transition duration-300 ${
                        isPro
                          ? "bg-white text-slate-950 hover:bg-sky-50"
                          : isSelected
                            ? "bg-emerald-600 text-white hover:bg-emerald-700"
                            : "bg-slate-950 text-white hover:bg-slate-800"
                      }`}
                    >
                      {buttonText(plan)}
                      {canCheckout && <ArrowRight className="ml-2 h-4 w-4" />}
                    </Button>
                    {isPro && (
                      <div className="mt-3 text-center text-xs font-bold uppercase tracking-[0.18em] text-slate-300">
                        Premium reporting, exports, and priority workflows
                      </div>
                    )}
                    {!isPro && isGrowth && (
                      <div className="mt-3 text-center text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
                        Best value for report-driven gyms
                      </div>
                    )}
                    {isStarter && (
                      <div className="mt-3 text-center text-xs font-bold uppercase tracking-[0.18em] text-slate-600">
                        Lean setup for focused gym operations
                      </div>
                    )}
                    {(subscription?.gymCoinBalance || 0) > 0 && (
                      <div className="mt-4 flex items-center gap-3 rounded-xl border border-emerald-100 bg-emerald-50 p-3 text-sm">
                        <input
                          type="checkbox"
                          id={`useGymCoins-${plan.key}`}
                          checked={useGymCoins}
                          onChange={(e) => setUseGymCoins(e.target.checked)}
                          className="h-4 w-4 rounded border-emerald-300 text-emerald-600 focus:ring-emerald-600"
                        />
                        <Label htmlFor={`useGymCoins-${plan.key}`} className="font-bold text-emerald-800 cursor-pointer">
                          Use {subscription?.gymCoinBalance} GymCoins
                          <span className="block text-xs font-semibold text-emerald-600">
                            1 GymCoin = ₹1. Reduces the checkout amount.
                          </span>
                        </Label>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {!showPlans && (
          <div className="grid gap-5 lg:grid-cols-[0.85fr_1.15fr]">
            <div className="rounded-[2rem] border border-white/80 bg-slate-950 p-6 text-white shadow-xl">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 text-emerald-200">
                <CreditCard className="h-6 w-6" />
              </div>
              <h2 className="mt-5 text-2xl font-black">How renewal works</h2>
              <div className="mt-5 space-y-4">
                {["Open the plans screen when you want to compare options.", "Choose Starter, Growth, or Pro on the next step.", "Access is restored only after signed payment verification."].map((step, index) => (
                  <div key={step} className="flex gap-3">
                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-emerald-300 text-sm font-black text-slate-950">
                      {index + 1}
                    </div>
                    <p className="text-sm font-semibold leading-6 text-slate-200">{step}</p>
                  </div>
                ))}
              </div>
              <div className="mt-6 rounded-2xl border border-white/10 bg-white/5 p-4 text-sm font-semibold leading-6 text-slate-200">
                No premium feature is unlocked by a click alone. The selected plan is applied only when payment is completed and verified.
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              {implementedDemos.map((demo) => {
                const Icon = demo.icon;
                return (
                  <div key={demo.title} className="rounded-[1.75rem] border border-white/80 bg-white/80 p-5 shadow-sm backdrop-blur">
                    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                      <Icon className="h-5 w-5" />
                    </div>
                    <h3 className="mt-4 text-lg font-black text-slate-950">{demo.title}</h3>
                    <p className="mt-2 text-sm font-semibold leading-6 text-slate-600">{demo.body}</p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {manualOrder && (
          <div className="rounded-[1.8rem] border border-white/80 bg-white/85 p-6 shadow-sm backdrop-blur">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-xl font-black text-slate-950">Pending Checkout Verification</h2>
                <p className="mt-1 text-sm font-medium text-slate-500">
                  {subscriptionPlanLabel(manualOrder.subscriptionPlan)} order {manualOrder.razorpayOrderId} covers {dateText(manualOrder.subscriptionStart)} to {dateText(manualOrder.subscriptionEnd)}.
                </p>
              </div>
              <Badge className="border-amber-200 bg-amber-100 text-amber-800">{manualOrder.paymentStatus}</Badge>
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
          </div>
        )}

        {!showPlans && (
          <>
            <div className="overflow-hidden rounded-[1.8rem] border border-white/80 bg-white/85 shadow-sm backdrop-blur">
              <div className="border-b border-slate-100 p-6">
                <h2 className="flex items-center gap-3 text-xl font-black text-slate-950">
                  <ShieldCheck className="h-5 w-5 text-emerald-700" />
                  Subscription Payment History
                </h2>
                <p className="mt-1 text-sm font-medium text-slate-500">Organization subscription payments are stored separately from member payments.</p>
              </div>

              <div className="divide-y divide-slate-100">
                {paymentHistory.length === 0 ? (
                  <div className="p-8 text-center text-sm font-semibold text-slate-500">No subscription ledger rows yet.</div>
                ) : (
                  paymentHistory.map((payment) => (
                    <div key={payment.id} className="grid gap-4 p-5 md:grid-cols-[1fr_auto_auto] md:items-center">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-lg font-black text-slate-950">{money.format(payment.amount || 0)}</span>
                          <Badge
                            className={
                              payment.paymentStatus === "PAID"
                                ? statusStyles.ACTIVE
                                : payment.paymentStatus === "FAILED"
                                  ? statusStyles.DEACTIVATED
                                  : statusStyles.TRIAL
                            }
                          >
                            {payment.paymentStatus}
                          </Badge>
                        </div>
                        <div className="mt-2 flex flex-wrap gap-4 text-xs font-semibold text-slate-500">
                          <span className="inline-flex items-center gap-1">
                            <CalendarDays className="h-3.5 w-3.5" /> {dateText(payment.subscriptionStart)} - {dateText(payment.subscriptionEnd)}
                          </span>
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
              {paymentHistoryPage && paymentHistoryPage.totalElements > paymentHistory.length && (
                <div className="flex flex-col justify-between gap-3 border-t border-slate-100 bg-slate-50/70 p-4 sm:flex-row sm:items-center">
                  <div className="text-sm font-bold text-slate-500">
                    Showing {paymentHistory.length} of {paymentHistoryPage.totalElements} subscription payments
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => loadMoreHistoryMutation.mutate((paymentHistoryPage?.page ?? 0) + 1)}
                    disabled={!paymentHistoryPage.hasNext || loadMoreHistoryMutation.isPending}
                    className="rounded-full border-white/80 bg-white px-5 font-black"
                  >
                    {loadMoreHistoryMutation.isPending ? "Loading..." : "Load older payments"}
                  </Button>
                </div>
              )}
            </div>

            <div className="grid gap-4 rounded-[1.8rem] border border-emerald-100 bg-emerald-50/80 p-5 text-sm font-semibold text-emerald-950 sm:grid-cols-3">
              <div className="flex items-center gap-2">
                <LockKeyhole className="h-4 w-4" />
                Authenticated checkout endpoint
              </div>
              <div className="flex items-center gap-2">
                <FileDown className="h-4 w-4" />
                Custom-range exports gated by Pro
              </div>
              <div className="flex items-center gap-2">
                <LifeBuoy className="h-4 w-4" />
                Pro-ready support and integrations
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
