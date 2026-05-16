import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Building, MapPin, Phone, Mail, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

export default function OrganizationProfile() {
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data: organization, isLoading } = useQuery({
    queryKey: ["orgProfile", user?.organizationId],
    queryFn: async () => {
      if (user?.organizationId) {
        const res = await api.get(`/organizations/${user.organizationId}`);
        return res.data;
      }
      const res = await api.get("/organizations");
      return res.data[0] ?? null;
    },
  });

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm();

  useEffect(() => {
    if (organization) {
      reset({
        name: organization.name || "",
        district: organization.district || "",
        description: organization.description || "",
        adminName: organization.adminFullName || "",
        adminPhone: organization.adminPhoneNumber || "",
        adminEmail: organization.adminEmail || "",
        monthlyMembershipFee: organization.monthlyMembershipFee || 0,
      });
    }
  }, [organization, reset]);

  const mutation = useMutation({
    mutationFn: (data: any) => api.patch(`/organizations/${organization.id}`, {
      name: data.name,
      district: data.district,
      logoUrl: organization.logoUrl,
      mapLocationLink: organization.mapLocationLink,
      address: organization.address,
      description: data.description || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      monthlyMembershipFee: data.monthlyMembershipFee,
      status: organization.status,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orgProfile"] });
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization profile updated.");
    },
    onError: () => toast.error("Failed to update profile"),
  });

  const onSubmit = (data: any) => {
    mutation.mutate(data);
  };

  if (isLoading) {
    return <div className="p-8">Loading profile...</div>;
  }

  if (!organization) {
    return <div className="p-8">No organization found.</div>;
  }

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8">
      <div className="flex items-center space-x-4 mb-4 bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div className="h-16 w-16 bg-indigo-100/50 dark:bg-indigo-900/40 rounded-2xl flex items-center justify-center text-indigo-600 dark:text-indigo-400 ring-4 ring-white/50 dark:ring-white/10 shadow-md">
          <Building className="h-8 w-8" />
        </div>
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white tracking-tight">{organization.name}</h1>
          <p className="text-slate-500 dark:text-slate-400 font-medium">Organization Profile</p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building className="w-5 h-5 mr-3 text-indigo-500" /> General Information</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Public details about the gym.</p>
          </div>
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name" className="font-semibold text-slate-700 dark:text-slate-300">Organization Name</Label>
                <div className="relative">
                  <Building className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                  <Input id="name" {...register("name")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="district" className="font-semibold text-slate-700 dark:text-slate-300">District / Region</Label>
                <div className="relative">
                  <MapPin className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                  <Input id="district" {...register("district")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description" className="font-semibold text-slate-700 dark:text-slate-300">Description</Label>
              <textarea
                id="description"
                {...register("description")}
                className="w-full p-3 pl-4 rounded-xl shadow-sm border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 backdrop-blur-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 min-h-[100px] text-slate-900 dark:text-slate-100"
                placeholder="A short description of your facility..."
              />
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><User className="w-5 h-5 mr-3 text-emerald-500" /> Primary Contact</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Contact info for the main administrator.</p>
          </div>
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="adminName" className="font-semibold text-slate-700 dark:text-slate-300">Admin Full Name</Label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminName" {...register("adminName")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
            </div>
            <div className="space-y-2 md:col-start-1">
              <Label htmlFor="adminPhone" className="font-semibold text-slate-700 dark:text-slate-300">Admin Phone Number</Label>
              <div className="relative">
                <Phone className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminPhone" {...register("adminPhone")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="adminEmail" className="font-semibold text-slate-700 dark:text-slate-300">Admin Email</Label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400" />
                <Input id="adminEmail" {...register("adminEmail")} className="pl-10 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" type="email" />
              </div>
            </div>
          </div>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="p-6 border-b border-black/5 dark:border-white/5">
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building className="w-5 h-5 mr-3 text-amber-500" /> Billing Configuration</h2>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Financial settings for this organization.</p>
          </div>
          <div className="p-6">
            <div className="space-y-2 max-w-sm">
              <Label htmlFor="monthlyMembershipFee" className="font-semibold text-slate-700 dark:text-slate-300">Monthly Membership Fee (INR)</Label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-500 font-bold">Rs</span>
                <Input
                  id="monthlyMembershipFee"
                  type="number"
                  {...register("monthlyMembershipFee", { valueAsNumber: true })}
                  className="pl-9 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4 mt-8">
          <Button type="submit" disabled={isSubmitting} className="px-10 rounded-full shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold h-12">
            {isSubmitting ? "Saving..." : "Save Profile"}
          </Button>
        </div>
      </form>
    </div>
  );
}
