import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Settings as SettingsIcon, MessageSquare, Mail, BellRing, Building } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

export default function Settings() {
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery({
    queryKey: ["settings"],
    queryFn: async () => {
      const res = await api.get("/settings");
      return res.data;
    }
  });

  const { register, handleSubmit, reset, watch, formState: { isSubmitting } } = useForm({
    defaultValues: {
      brandingName: "",
      reminderDaysBeforeExpiry: 3,
      whatsappEnabled: false,
      emailEnabled: false,
    }
  });

  const watchConfig = watch();

  useEffect(() => {
    if (settings) {
      reset({
        brandingName: settings.brandingName || "",
        reminderDaysBeforeExpiry: settings.reminderDaysBeforeExpiry || 3,
        whatsappEnabled: settings.whatsappEnabled || false,
        emailEnabled: settings.emailEnabled || false,
      });
    }
  }, [settings, reset]);

  const mutation = useMutation({
    mutationFn: (data: any) => api.patch("/settings", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["settings"] });
      toast.success("Settings saved successfully.");
    },
    onError: () => {
      toast.error("Failed to save settings.");
    }
  });

  const onSubmit = (data: any) => {
    mutation.mutate(data);
  };

  if (isLoading) {
    return <div className="p-8">Loading settings...</div>;
  }

  return (
    <div className="p-8 max-w-5xl mx-auto space-y-8">
      <div className="flex items-center space-x-4 mb-8 bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div className="bg-indigo-100/50 dark:bg-indigo-900/40 p-4 rounded-2xl text-indigo-600 dark:text-indigo-400">
          <SettingsIcon className="w-8 h-8" />
        </div>
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white tracking-tight">Organization Settings</h1>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1 font-medium">Configure automated reminders and brand preferences.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <form id="settings-form" onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="glass-panel overflow-hidden">
              <div className="p-6 border-b border-black/5 dark:border-white/5">
                <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><Building className="w-5 h-5 mr-3 text-indigo-500" /> Branding Configuration</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">This name will appear in automated communications.</p>
              </div>
              <div className="p-6 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="brandingName" className="font-semibold text-slate-700 dark:text-slate-300">Sender Name</Label>
                  <Input id="brandingName" {...register("brandingName")} placeholder="e.g. Iron Athletics Gym" className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm" />
                </div>
              </div>
            </div>

            <div className="glass-panel overflow-hidden">
              <div className="p-6 border-b border-black/5 dark:border-white/5">
                <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><BellRing className="w-5 h-5 mr-3 text-amber-500" /> Reminder Timing</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Control when automated reminders are triggered.</p>
              </div>
              <div className="p-6 space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="reminderDaysBeforeExpiry" className="font-semibold text-slate-700 dark:text-slate-300">Days before expiry to flag members</Label>
                  <Input 
                    id="reminderDaysBeforeExpiry" 
                    type="number" 
                    min="1" 
                    max="30" 
                    {...register("reminderDaysBeforeExpiry", { valueAsNumber: true })} 
                    className="bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
                  />
                  <p className="text-xs text-slate-500 font-medium">Members hitting this threshold will show up in the automated digest.</p>
                </div>
              </div>
            </div>

            <div className="glass-panel overflow-hidden">
              <div className="p-6 border-b border-black/5 dark:border-white/5">
                <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center"><MessageSquare className="w-5 h-5 mr-3 text-green-500" /> Delivery Channels</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Select which channels to use for automated messages.</p>
              </div>
              <div className="p-6 space-y-6">
                <div className="flex items-center justify-between p-5 border border-black/5 dark:border-white/10 rounded-2xl bg-white/40 dark:bg-slate-900/40 hover:bg-white/60 dark:hover:bg-slate-800/60 transition-colors">
                  <div className="flex items-center space-x-4">
                    <div className="p-3 bg-green-100/50 dark:bg-green-900/40 text-green-600 dark:text-green-400 rounded-xl">
                      <MessageSquare className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-900 dark:text-white">WhatsApp Digest</h4>
                      <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-0.5">Receive an admin summary via WhatsApp.</p>
                    </div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" className="sr-only peer" {...register("whatsappEnabled")} />
                    <div className="w-14 h-7 bg-slate-200/50 dark:bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-green-500"></div>
                  </label>
                </div>

                <div className="flex items-center justify-between p-5 border border-black/5 dark:border-white/10 rounded-2xl bg-white/40 dark:bg-slate-900/40 hover:bg-white/60 dark:hover:bg-slate-800/60 transition-colors">
                  <div className="flex items-center space-x-4">
                    <div className="p-3 bg-blue-100/50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-xl">
                      <Mail className="w-5 h-5" />
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-900 dark:text-white">Email Digest</h4>
                      <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-0.5">Receive an admin summary over email.</p>
                    </div>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input type="checkbox" className="sr-only peer" {...register("emailEnabled")} />
                    <div className="w-14 h-7 bg-slate-200/50 dark:bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-4">
              <Button type="submit" form="settings-form" disabled={isSubmitting} className="w-full md:w-auto px-10 rounded-full shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold">
                {isSubmitting ? "Saving..." : "Save Preferences"}
              </Button>
            </div>
          </form>
        </div>

        {/* Live Preview Panel */}
        <div className="lg:col-span-1 glass-panel rounded-[2rem] overflow-hidden shadow-xl sticky top-8 border-t border-l border-white/60 dark:border-white/10 flex flex-col h-[500px]">
          <div className="bg-slate-900/90 backdrop-blur-md text-white p-5 text-center text-sm font-bold tracking-widest uppercase border-b border-white/10">
            Preview
          </div>
          <div className="p-6 bg-slate-100/50 dark:bg-slate-900/30 flex-1 flex flex-col items-center justify-center relative overflow-hidden">
             {watchConfig.whatsappEnabled ? (
                <div className="bg-white p-4 rounded-xl shadow-sm text-sm border space-y-2 max-w-[280px] mx-auto ml-auto relative">
                  <div className="absolute -top-3 -left-3 text-[10px] bg-green-500 text-white px-2 py-1 rounded-full font-bold shadow-sm">WhatsApp</div>
                  <div className="font-bold text-slate-800 flex justify-between">
                    <span>{watchConfig.brandingName || "BillMyGym"} System</span>
                  </div>
                  <p className="text-slate-600 leading-relaxed whitespace-pre-line">
                    Members needing renewal:
                    
                    1. John — Expired Today
                    2. Rahul — Overdue 2 days
                    3. Alex — Overdue 1 day
                    
                    Total pending: 3
                  </p>
                  <div className="text-right text-[10px] text-slate-400 mt-1">12:30 PM</div>
                </div>
             ) : watchConfig.emailEnabled ? (
                <div className="bg-white p-5 rounded-lg shadow-sm text-sm border space-y-3">
                  <div className="border-b pb-3 border-slate-100">
                    <p className="text-xs text-slate-400 uppercase tracking-widest font-semibold mb-1">Incoming Email</p>
                    <p className="font-semibold text-slate-800">Membership Renewal Summary</p>
                    <p className="text-xs text-slate-500 mt-1">From: {watchConfig.brandingName || "BillMyGym"} System</p>
                  </div>
                  <div className="text-slate-600 text-sm leading-relaxed whitespace-pre-line">
                    Members needing attention:

                    John
                    Rahul
                    Alex

                    Please review pending renewals.
                  </div>
                </div>
             ) : (
               <div className="flex flex-col items-center justify-center h-full text-slate-400">
                 <BellRing className="w-12 h-12 mb-4 opacity-50" />
                 <p className="text-center text-sm">Enable a channel to see preview</p>
               </div>
             )}
          </div>
        </div>
      </div>
    </div>
  );
}

