import { useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Building2, CreditCard, IndianRupee, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";

export default function OrganizationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: organization, isLoading } = useQuery({
    queryKey: ["organization", id],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}`);
      return res.data;
    }
  });

  const { data: subscriptionStatus } = useQuery({
    queryKey: ["organizationSubscriptionStatus", id],
    queryFn: async () => {
      const res = await api.get(`/subscription/organizations/${id}/status`);
      return res.data;
    },
    enabled: Boolean(id),
    staleTime: 60_000,
  });

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm();

  useEffect(() => {
    if (organization) {
      reset({
        name: organization.name,
        district: organization.district,
        description: organization.description || "",
        adminName: organization.adminFullName,
        adminPhone: organization.adminPhoneNumber,
        adminEmail: organization.adminEmail,
        organizationType: organization.organizationType || "REGULAR",
        status: organization.status,
      });
    }
  }, [organization, reset]);

  const updateMutation = useMutation({
    mutationFn: (data: any) => api.patch(`/organizations/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organization", id] });
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization details updated successfully");
    },
    onError: () => toast.error("Failed to update organization"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/organizations/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization deleted successfully");
      navigate("/app/organizations");
    },
    onError: () => toast.error("Failed to delete organization"),
  });

  const onSubmit = (data: any) => {
    updateMutation.mutate({
      name: data.name,
      district: data.district,
      logoUrl: organization.logoUrl,
      mapLocationLink: organization.mapLocationLink,
      address: organization.address,
      description: data.description || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      monthlyMembershipFee: organization.monthlyMembershipFee,
      organizationType: data.organizationType,
      status: data.status,
    });
  };

  if (isLoading) return <div className="p-8">Loading...</div>;
  if (!organization) return <div className="p-8">Organization not found.</div>;

  const formatDate = (value?: string | null) => {
    if (!value) return "Not set";
    return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
  };

  const money = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  });

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-6">
      <div className="flex justify-between items-center mb-8 bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/app/organizations")} className="rounded-full mr-2 hover:bg-white/50 dark:hover:bg-white/10">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-slate-900 dark:text-white flex items-center tracking-tight">
              {organization.name}
              <Badge variant={organization.status === "ACTIVE" ? "default" : "secondary"} className="ml-3 rounded-full px-3 font-semibold uppercase tracking-wider text-xs">
                {organization.status}
              </Badge>
              <Badge variant="outline" className="ml-2 rounded-full px-3 font-semibold uppercase tracking-wider text-xs">
                {organization.organizationType || "REGULAR"}
              </Badge>
            </h1>
            <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium font-mono text-sm">Tenant ID: {organization.id}</p>
          </div>
        </div>
        <Button 
          variant="destructive" 
          onClick={() => {
            if(confirm("Are you sure you want to soft delete this organization? This action will suspend all operations for the tenant.")) {
              deleteMutation.mutate();
            }
          }}
          disabled={deleteMutation.isPending}
          className="rounded-full shadow-lg font-semibold"
        >
          <Trash2 className="w-4 h-4 mr-2" />
          {deleteMutation.isPending ? "Deleting..." : "Delete Organization"}
        </Button>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Organization Settings</h2>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">Manage workspace details and status.</p>
          </div>
          <div className="p-6 space-y-6">
             <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name" className="font-semibold text-slate-700 dark:text-slate-300">Business Name</Label>
                <Input id="name" {...register("name")} className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="district" className="font-semibold text-slate-700 dark:text-slate-300">District</Label>
                <Input id="district" {...register("district")} className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="status" className="font-semibold text-slate-700 dark:text-slate-300">Account Status</Label>
                <select 
                  id="status" 
                  {...register("status")}
                  className="w-full h-11 px-4 py-2 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm"
                >
                  <option value="TRIAL">Trial</option>
                  <option value="ACTIVE">Active</option>
                  <option value="PAUSED">Paused</option>
                  <option value="DEACTIVATED">Deactivated</option>
                </select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="organizationType" className="font-semibold text-slate-700 dark:text-slate-300">Organization Type</Label>
                <select
                  id="organizationType"
                  {...register("organizationType")}
                  className="w-full h-11 px-4 py-2 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm"
                >
                  <option value="REGULAR">Regular</option>
                  <option value="PILOT">Pilot</option>
                </select>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 rounded-[1.5rem] border border-emerald-100 bg-emerald-50/60 p-5">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Trial Window</p>
                <p className="mt-2 text-sm font-bold text-slate-900">{formatDate(organization.trialStartDate)} - {formatDate(organization.trialEndDate)}</p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Pilot Window</p>
                <p className="mt-2 text-sm font-bold text-slate-900">{formatDate(organization.pilotStartDate)} - {formatDate(organization.pilotEndDate)}</p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Subscription Due</p>
                <p className="mt-2 text-sm font-bold text-slate-900">{formatDate(organization.subscriptionDueDate)}</p>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description" className="font-semibold text-slate-700 dark:text-slate-300">Provider Notes</Label>
              <textarea 
                id="description" 
                {...register("description")} 
                className="w-full p-4 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 min-h-[80px] backdrop-blur-sm shadow-sm"
                placeholder="Internal notes about this client..."
              />
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Primary Admin</h2>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">The ORG_ADMIN contact for this instance.</p>
          </div>
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="adminName" className="font-semibold text-slate-700 dark:text-slate-300">Full Name</Label>
              <Input id="adminName" {...register("adminName")} className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="adminPhone" className="font-semibold text-slate-700 dark:text-slate-300">Phone Number</Label>
              <Input id="adminPhone" {...register("adminPhone")} className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="adminEmail" className="font-semibold text-slate-700 dark:text-slate-300">Email Address</Label>
              <Input id="adminEmail" {...register("adminEmail")} type="email" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center">
              <CreditCard className="w-5 h-5 mr-3 text-emerald-600" /> SaaS Subscription Ledger
            </h2>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">Platform subscription payments are separated from member payments.</p>
          </div>
          <div className="p-6 space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="rounded-2xl border border-emerald-100 bg-emerald-50/60 p-4">
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Plan</p>
                <p className="mt-2 text-lg font-black text-slate-950">{money.format(subscriptionStatus?.monthlyAmount || 299)}/mo</p>
              </div>
              <div className="rounded-2xl border border-white/80 bg-white/65 p-4">
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Period</p>
                <p className="mt-2 text-sm font-black text-slate-950">{formatDate(subscriptionStatus?.currentPeriodStart)} - {formatDate(subscriptionStatus?.currentPeriodEnd)}</p>
              </div>
              <div className="rounded-2xl border border-white/80 bg-white/65 p-4">
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">WhatsApp Usage</p>
                <p className="mt-2 text-lg font-black text-slate-950">{subscriptionStatus?.whatsappUsage ?? 0}</p>
              </div>
              <div className="rounded-2xl border border-white/80 bg-white/65 p-4">
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Email Usage</p>
                <p className="mt-2 text-lg font-black text-slate-950">{subscriptionStatus?.emailUsage ?? 0}</p>
              </div>
            </div>

            <div className="overflow-hidden rounded-2xl border border-white/80 bg-white/55">
              {subscriptionStatus?.paymentHistory?.length ? (
                subscriptionStatus.paymentHistory.map((payment: any) => (
                  <div key={payment.id} className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_auto_auto] md:items-center">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-lg font-black text-slate-950">{money.format(payment.amount || 0)}</span>
                        <Badge variant={payment.paymentStatus === "PAID" ? "default" : "secondary"} className="rounded-full uppercase tracking-wider">
                          {payment.paymentStatus}
                        </Badge>
                      </div>
                      <p className="mt-1 text-xs font-semibold text-slate-500">
                        {formatDate(payment.subscriptionStart)} - {formatDate(payment.subscriptionEnd)} · Order {payment.razorpayOrderId || "manual"}
                      </p>
                    </div>
                    <div className="text-sm font-bold text-slate-700">{formatDate(payment.paymentDate)}</div>
                    <div className="flex items-center gap-1 text-sm font-black text-emerald-700">
                      <IndianRupee className="h-4 w-4" /> Subscription
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-6 text-center text-sm font-semibold text-slate-500">No subscription payment records yet.</div>
              )}
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-6">
          <Button type="submit" disabled={isSubmitting} className="px-8 rounded-full shadow-lg shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-bold h-12 text-base">
            {isSubmitting ? "Saving..." : "Save Changes"}
          </Button>
        </div>
      </form>
    </div>
  );
}
