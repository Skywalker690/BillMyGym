import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Building2, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

const orgSchema = z.object({
  name: z.string().min(1, "Organization name is required"),
  district: z.string().min(1, "District is required"),
  organizationType: z.enum(["REGULAR", "PILOT"]),
  adminName: z.string().min(1, "Admin name is required"),
  adminEmail: z.string().email("Invalid email address"),
  adminPhone: z.string().min(1, "Admin phone is required"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  logoUrl: z.string().url("Must be a valid URL").optional().or(z.literal("")),
  mapLocationLink: z.string().url("Must be a valid URL").optional().or(z.literal("")),
  address: z.string().optional(),
  description: z.string().optional(),
});

type OrgFormValues = z.infer<typeof orgSchema>;

export default function OrganizationForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState(1);

  const { register, handleSubmit, trigger, watch, formState: { errors, isSubmitting } } = useForm<OrgFormValues>({
    resolver: zodResolver(orgSchema),
    mode: "onTouched",
    defaultValues: {
      organizationType: "REGULAR",
    },
  });
  const values = watch();

  const mutation = useMutation({
    mutationFn: (data: OrgFormValues) => api.post("/auth/register", {
      organizationName: data.name,
      district: data.district,
      logoUrl: data.logoUrl || null,
      mapLocationLink: data.mapLocationLink || null,
      address: data.address || null,
      adminFullName: data.adminName,
      adminPhoneNumber: data.adminPhone,
      adminEmail: data.adminEmail,
      organizationDescription: data.description || null,
      organizationType: data.organizationType,
      password: data.password,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization provisioned successfully");
      navigate("/app/organizations");
    },
    onError: () => toast.error("Failed to provision organization"),
  });

  const onSubmit = (data: OrgFormValues) => {
    mutation.mutate(data);
  };

  const nextStep = async () => {
    let isValid = false;
    if (step === 1) {
      isValid = await trigger(["name", "district", "organizationType"]);
    } else if (step === 2) {
      isValid = await trigger(["adminName", "adminEmail", "adminPhone", "password"]);
    }
    
    if (isValid) setStep(step + 1);
  };

  return (
    <div className="p-8 max-w-3xl mx-auto space-y-6">
      <div className="flex items-center space-x-4 mb-8 bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <Button variant="ghost" size="icon" onClick={() => navigate("/app/organizations")} className="rounded-full mr-2 hover:bg-white/50 dark:hover:bg-white/10">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">Provision Organization</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Create a new tenant workspace and admin account.</p>
        </div>
      </div>

      <div className="flex items-center mb-10 bg-white/30 dark:bg-slate-900/30 p-4 rounded-2xl backdrop-blur-md">
        <div className={`flex flex-col items-center flex-1 ${step >= 1 ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'} transition-colors`}>
           <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold mb-2 shadow-sm ${step >= 1 ? 'bg-indigo-600 text-white shadow-indigo-500/30' : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'} transition-colors`}>1</div>
           <span className="text-sm font-bold uppercase tracking-wider">Org Info</span>
        </div>
        <div className={`h-1 flex-1 rounded-full ${step >= 2 ? 'bg-indigo-600 dark:bg-indigo-500' : 'bg-slate-200 dark:bg-slate-800'} transition-colors`}></div>
        <div className={`flex flex-col items-center flex-1 ${step >= 2 ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'} transition-colors`}>
           <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold mb-2 shadow-sm ${step >= 2 ? 'bg-indigo-600 text-white shadow-indigo-500/30' : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'} transition-colors`}>2</div>
           <span className="text-sm font-bold uppercase tracking-wider">Admin Account</span>
        </div>
        <div className={`h-1 flex-1 rounded-full ${step >= 3 ? 'bg-indigo-600 dark:bg-indigo-500' : 'bg-slate-200 dark:bg-slate-800'} transition-colors`}></div>
        <div className={`flex flex-col items-center flex-1 ${step >= 3 ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'} transition-colors`}>
           <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold mb-2 shadow-sm ${step >= 3 ? 'bg-indigo-600 text-white shadow-indigo-500/30' : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'} transition-colors`}>3</div>
           <span className="text-sm font-bold uppercase tracking-wider">Review & Submit</span>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)}>
        {step === 1 && (
          <div className="glass-panel overflow-hidden">
            <div className="p-6 border-b border-black/5 dark:border-white/5">
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building2 className="w-5 h-5 mr-3 text-indigo-600 dark:text-indigo-400" /> Organization Details</h2>
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1 ml-8">Basic information about the new gym or facility.</p>
            </div>
            <div className="p-6 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="name" className="font-semibold text-slate-700 dark:text-slate-300">Business Name <span className="text-red-500">*</span></Label>
                  <Input id="name" {...register("name")} placeholder="e.g. Iron Athletics" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.name && <p className="text-red-500 text-sm font-medium">{errors.name.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="district" className="font-semibold text-slate-700 dark:text-slate-300">District <span className="text-red-500">*</span></Label>
                  <Input id="district" {...register("district")} placeholder="e.g. Downtown" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.district && <p className="text-red-500 text-sm font-medium">{errors.district.message}</p>}
                </div>
              </div>

              <div className="rounded-[1.5rem] border border-emerald-100 bg-emerald-50/60 p-5">
                <Label htmlFor="organizationType" className="font-semibold text-slate-700 dark:text-slate-300">Access Type <span className="text-red-500">*</span></Label>
                <select
                  id="organizationType"
                  {...register("organizationType")}
                  className="mt-2 w-full h-11 px-4 py-2 rounded-xl border border-black/5 dark:border-white/10 bg-white/75 dark:bg-slate-900/50 text-slate-900 dark:text-slate-100 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500/50 backdrop-blur-sm"
                >
                  <option value="REGULAR">Regular - 14 day trial, then monthly billing</option>
                  <option value="PILOT">Pilot - 90 day free license, billing bypassed</option>
                </select>
                {errors.organizationType && <p className="text-red-500 text-sm font-medium mt-2">{errors.organizationType.message}</p>}
                <p className="mt-2 text-xs font-semibold text-emerald-800">
                  Regular organizations start in TRIAL. Pilot organizations start ACTIVE and bypass subscription enforcement until the pilot window ends.
                </p>
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="address" className="font-semibold text-slate-700 dark:text-slate-300">Full Address</Label>
                <Input id="address" {...register("address")} placeholder="123 Fitness Way, City" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="logoUrl" className="font-semibold text-slate-700 dark:text-slate-300">Logo URL</Label>
                  <Input id="logoUrl" type="url" {...register("logoUrl")} placeholder="https://..." className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.logoUrl && <p className="text-red-500 text-sm font-medium">{errors.logoUrl.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="mapLocationLink" className="font-semibold text-slate-700 dark:text-slate-300">Google Maps Link</Label>
                  <Input id="mapLocationLink" type="url" {...register("mapLocationLink")} placeholder="https://maps.google.com/..." className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.mapLocationLink && <p className="text-red-500 text-sm font-medium">{errors.mapLocationLink.message}</p>}
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="description" className="font-semibold text-slate-700 dark:text-slate-300">Description</Label>
                <textarea 
                  id="description" 
                  {...register("description")} 
                  className="w-full p-4 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 min-h-[100px] backdrop-blur-sm shadow-sm"
                />
              </div>

              <div className="flex justify-end pt-6 border-t border-black/5 dark:border-white/5">
                <Button type="button" onClick={nextStep} className="rounded-full px-8 shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-bold h-11">Next Step</Button>
              </div>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="glass-panel overflow-hidden">
            <div className="p-6 border-b border-black/5 dark:border-white/5">
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><User className="w-5 h-5 mr-3 text-indigo-600 dark:text-indigo-400" /> Admin Credentials</h2>
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1 ml-8">This will be the first ORG_ADMIN account for this organization.</p>
            </div>
            <div className="p-6 space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="adminName" className="font-semibold text-slate-700 dark:text-slate-300">Full Name <span className="text-red-500">*</span></Label>
                  <Input id="adminName" {...register("adminName")} placeholder="Manager Name" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.adminName && <p className="text-red-500 text-sm font-medium">{errors.adminName.message}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="adminPhone" className="font-semibold text-slate-700 dark:text-slate-300">Phone Number <span className="text-red-500">*</span></Label>
                  <Input id="adminPhone" {...register("adminPhone")} placeholder="+1 234 567 890" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                  {errors.adminPhone && <p className="text-red-500 text-sm font-medium">{errors.adminPhone.message}</p>}
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="adminEmail" className="font-semibold text-slate-700 dark:text-slate-300">Email Address (Login ID) <span className="text-red-500">*</span></Label>
                <Input id="adminEmail" type="email" {...register("adminEmail")} placeholder="manager@gym.com" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                {errors.adminEmail && <p className="text-red-500 text-sm font-medium">{errors.adminEmail.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="font-semibold text-slate-700 dark:text-slate-300">Temporary Password <span className="text-red-500">*</span></Label>
                <Input id="password" type="password" {...register("password")} placeholder="Temporary password" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                {errors.password && <p className="text-red-500 text-sm font-medium">{errors.password.message}</p>}
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400">Admin will be able to change this after login.</p>
              </div>

              <div className="flex justify-between pt-6 border-t border-black/5 dark:border-white/5">
                <Button type="button" variant="outline" onClick={() => setStep(1)} className="rounded-full px-6 bg-white/50 dark:bg-slate-800/50 backdrop-blur-sm border-white/50 dark:border-white/20 hover:bg-white/80 dark:hover:bg-slate-700/80 font-medium">Back</Button>
                <Button type="button" onClick={nextStep} className="rounded-full px-8 shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-bold h-11">Review details</Button>
              </div>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="glass-panel overflow-hidden">
            <div className="p-6 border-b border-black/5 dark:border-white/5">
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Review & Provision</h2>
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">Verify all details before creating the instance.</p>
            </div>
            <div className="p-6 space-y-6">
              <div className="bg-white/50 dark:bg-slate-800/50 p-6 rounded-2xl border border-black/5 dark:border-white/5 shadow-sm space-y-4">
                 <div>
                   <h4 className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest mb-3">Workspace</h4>
                   <div className="grid grid-cols-2 gap-3 text-sm">
                      <div className="text-slate-500 dark:text-slate-400 font-medium">Name:</div><div className="font-bold text-slate-900 dark:text-white">{values.name || "Untitled organization"}</div>
                      <div className="text-slate-500 dark:text-slate-400 font-medium">District:</div><div className="font-bold text-slate-900 dark:text-white">{values.district || "-"}</div>
                      <div className="text-slate-500 dark:text-slate-400 font-medium">Access Type:</div><div className="font-bold text-slate-900 dark:text-white">{values.organizationType || "REGULAR"}</div>
                      <div className="text-slate-500 dark:text-slate-400 font-medium">Admin:</div><div className="font-bold text-slate-900 dark:text-white">{values.adminName || "-"}</div>
                      <div className="text-slate-500 dark:text-slate-400 font-medium">Email:</div><div className="font-bold text-slate-900 dark:text-white">{values.adminEmail || "-"}</div>
                   </div>
                   <p className="text-xs font-medium text-indigo-600 dark:text-indigo-400 mt-4 text-center">All data looks good. We will execute the provision on the backend.</p>
                 </div>
              </div>

              <div className="flex justify-between pt-6 border-t border-black/5 dark:border-white/5">
                <Button type="button" variant="outline" onClick={() => setStep(2)} className="rounded-full px-6 bg-white/50 dark:bg-slate-800/50 backdrop-blur-sm border-white/50 dark:border-white/20 hover:bg-white/80 dark:hover:bg-slate-700/80 font-medium">Back</Button>
                <Button type="submit" disabled={isSubmitting} className="rounded-full px-8 shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-bold h-11">
                  {isSubmitting ? "Provisioning..." : "Create Organization"}
                </Button>
              </div>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
