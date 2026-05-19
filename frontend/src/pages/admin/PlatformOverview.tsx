import { useQuery } from "@tanstack/react-query";
import { Activity as ActivityIcon, AlertTriangle, Building2, ChevronRight, Mail, MessageCircle, PauseCircle, Plus, Rocket, ShieldOff, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";

type PlatformAnalytics = {
  totalOrganizations: number;
  activeOrganizations: number;
  trialOrganizations: number;
  pilotOrganizations: number;
  pausedOrganizations: number;
  deactivatedOrganizations: number;
  monthlySaasRevenue: number;
  paymentFailures: number;
  whatsappUsage: number;
  emailUsage: number;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const metricCards = [
  {
    key: "activeOrganizations",
    label: "Active Organizations",
    tone: "text-emerald-700 bg-emerald-50 border-emerald-100",
    icon: ActivityIcon,
  },
  {
    key: "trialOrganizations",
    label: "Trial Organizations",
    tone: "text-amber-700 bg-amber-50 border-amber-100",
    icon: Rocket,
  },
  {
    key: "pilotOrganizations",
    label: "Pilot Organizations",
    tone: "text-sky-700 bg-sky-50 border-sky-100",
    icon: Sparkles,
  },
  {
    key: "pausedOrganizations",
    label: "Paused Organizations",
    tone: "text-orange-700 bg-orange-50 border-orange-100",
    icon: PauseCircle,
  },
  {
    key: "deactivatedOrganizations",
    label: "Deactivated Organizations",
    tone: "text-rose-700 bg-rose-50 border-rose-100",
    icon: ShieldOff,
  },
  {
    key: "paymentFailures",
    label: "Payment Failures",
    tone: "text-red-700 bg-red-50 border-red-100",
    icon: AlertTriangle,
  },
] as const;

export default function PlatformOverview() {
  const { data: analytics, isLoading } = useQuery<PlatformAnalytics>({
    queryKey: ["platformAnalytics"],
    queryFn: async () => {
      const res = await api.get("/analytics/platform");
      return res.data;
    },
    staleTime: 60_000,
  });

  if (isLoading) {
    return <div className="p-8">Loading overview...</div>;
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8 p-8">
      <div className="billmygym-dark-shell relative overflow-hidden rounded-[2rem] p-10 text-white">
        <div className="relative z-10 max-w-3xl">
          <div className="mb-4 inline-flex items-center rounded-full border border-emerald-300/20 bg-emerald-300/10 px-4 py-2 text-[11px] font-black uppercase tracking-[0.28em] text-emerald-200">
            SaaS Command
          </div>
          <h1 className="text-4xl font-black tracking-tight">BillMyGym Platform</h1>
          <p className="mt-4 max-w-xl text-lg font-medium text-emerald-50/80">
            Monitor subscription health, pilot rollout, SaaS revenue, payment failures, and monthly communication usage from one server-computed dashboard.
          </p>
          <div className="mt-8 flex flex-wrap gap-4">
            <Link to="/app/organizations/new">
              <Button size="lg" className="h-12 rounded-full border-0 bg-white px-8 font-bold text-slate-950 hover:bg-emerald-50">
                <Plus className="mr-2 h-5 w-5" /> Provision Organization
              </Button>
            </Link>
            <Link to="/app/organizations">
              <Button size="lg" variant="outline" className="h-12 rounded-full border-white/20 bg-white/5 px-8 font-bold text-white hover:bg-white/10 hover:text-white">
                Open Directory
              </Button>
            </Link>
          </div>
        </div>
        <Building2 className="absolute -bottom-20 -right-20 h-96 w-96 text-white/5" />
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="glass-panel rounded-[1.75rem] p-7 md:col-span-1">
          <Building2 className="h-8 w-8 text-emerald-700" />
          <p className="mt-6 text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Total Organizations</p>
          <h2 className="mt-2 text-5xl font-black tracking-tight text-slate-950">{analytics?.totalOrganizations ?? 0}</h2>
        </div>

        <div className="glass-panel rounded-[1.75rem] p-7 md:col-span-2">
          <p className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Monthly SaaS Revenue</p>
          <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
            <h2 className="text-5xl font-black tracking-tight text-emerald-700">{money.format(analytics?.monthlySaasRevenue ?? 0)}</h2>
            <div className="rounded-full border border-emerald-100 bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-700">
              Payment-ledger derived
            </div>
          </div>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            <div className="rounded-2xl border border-white/80 bg-white/70 p-4">
              <MessageCircle className="h-5 w-5 text-emerald-700" />
              <p className="mt-3 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">WhatsApp Usage</p>
              <p className="mt-1 text-2xl font-black text-slate-950">{analytics?.whatsappUsage ?? 0}</p>
            </div>
            <div className="rounded-2xl border border-white/80 bg-white/70 p-4">
              <Mail className="h-5 w-5 text-emerald-700" />
              <p className="mt-3 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Email Usage</p>
              <p className="mt-1 text-2xl font-black text-slate-950">{analytics?.emailUsage ?? 0}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
        {metricCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.key} className="glass-panel rounded-[1.5rem] p-6">
              <div className={`inline-flex rounded-2xl border p-3 ${card.tone}`}>
                <Icon className="h-6 w-6" />
              </div>
              <p className="mt-5 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{card.label}</p>
              <p className="mt-2 text-4xl font-black text-slate-950">{analytics?.[card.key] ?? 0}</p>
            </div>
          );
        })}
      </div>

      <Link to="/app/organizations" className="block">
        <div className="glass-panel group overflow-hidden rounded-[1.8rem] border-emerald-100/60 transition-all hover:-translate-y-0.5 hover:border-emerald-200">
          <div className="flex items-center justify-between p-8">
            <div className="flex items-center gap-5">
              <div className="rounded-2xl border border-white/80 bg-white/80 p-4 shadow-sm transition-transform group-hover:scale-105">
                <Building2 className="h-8 w-8 text-emerald-700" />
              </div>
              <div>
                <h3 className="text-xl font-black tracking-tight text-slate-950">Manage Organizations Directory</h3>
                <p className="mt-1 text-sm font-medium text-slate-500">Review lifecycle state, pilot/trial windows, billing dates, and tenant ownership.</p>
              </div>
            </div>
            <div className="rounded-full border border-white/80 bg-white/80 p-3 text-emerald-700 shadow-sm transition-all group-hover:bg-emerald-600 group-hover:text-white">
              <ChevronRight className="h-6 w-6" />
            </div>
          </div>
        </div>
      </Link>
    </div>
  );
}

