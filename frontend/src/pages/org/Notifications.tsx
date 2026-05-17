import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bell, Search, CheckCircle, XCircle, Clock } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";

export default function Notifications() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [channelFilter, setChannelFilter] = useState("ALL");

  const { data: notifications, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const res = await api.get("/notifications");
      return res.data;
    }
  });

  if (isLoading) {
    return <div className="p-8">Loading notifications...</div>;
  }

  const filteredNotifications = notifications?.filter((n: any) => {
    const matchesSearch = 
      n.recipient.toLowerCase().includes(searchTerm.toLowerCase()) ||
      n.message.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStatus = statusFilter === "ALL" || n.status === statusFilter;
    const matchesChannel = channelFilter === "ALL" || n.channel === channelFilter;
    
    return matchesSearch && matchesStatus && matchesChannel;
  }) || [];

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex justify-between items-center bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white flex items-center tracking-tight">
            <Bell className="w-8 h-8 mr-3 text-indigo-600 dark:text-indigo-400" /> Notifications Log
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 font-medium ml-11">Audit log of all digest messages sent to organization admins.</p>
        </div>
      </div>

      <div className="glass-panel p-4 flex flex-col md:flex-row justify-between items-center gap-4">
        <div className="relative flex-1 w-full md:max-w-md">
          <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-400 h-4 w-4" />
          <Input 
            placeholder="Search by recipient or message..." 
            className="pl-11 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex gap-4 w-full md:w-auto">
          <select 
            className="flex-1 md:flex-none h-11 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm text-slate-700 dark:text-slate-300"
            value={channelFilter}
            onChange={(e) => setChannelFilter(e.target.value)}
          >
            <option value="ALL">All Channels</option>
            <option value="WHATSAPP">WhatsApp</option>
            <option value="EMAIL">Email</option>
          </select>
          <select 
            className="flex-1 md:flex-none h-11 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm text-slate-700 dark:text-slate-300"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="SENT">Sent</option>
            <option value="FAILED">Failed</option>
            <option value="PENDING">Pending</option>
          </select>
        </div>
      </div>

      <div className="glass-panel overflow-hidden">
        {filteredNotifications.length === 0 ? (
          <div className="p-16 text-center text-slate-500 font-medium">
            <p>No notifications found matching your filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-white/30 dark:bg-white/5 text-slate-500 dark:text-slate-400 border-b border-black/5 dark:border-white/5">
                <tr>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Status</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Type / Channel</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Recipient</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs max-w-xs">Message</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Timing</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5 dark:divide-white/5">
                {filteredNotifications.map((n: any) => (
                  <tr key={n.id} className="hover:bg-white/50 dark:hover:bg-white/5 transition-colors">
                    <td className="px-6 py-5">
                      {n.status === "SENT" && <Badge variant="default" className="bg-green-100 text-green-800 hover:bg-green-100 rounded-full font-semibold px-3"><CheckCircle className="w-3 h-3 mr-1.5"/> Sent</Badge>}
                      {n.status === "FAILED" && <Badge variant="destructive" className="rounded-full font-semibold px-3"><XCircle className="w-3 h-3 mr-1.5"/> Failed</Badge>}
                      {n.status === "PENDING" && <Badge variant="outline" className="text-amber-600 border-amber-600 rounded-full font-semibold px-3"><Clock className="w-3 h-3 mr-1.5"/> Pending</Badge>}
                      {n.status === "FAILED" && n.failureReason && (
                        <p className="text-xs font-medium text-red-500 mt-1.5 max-w-[150px] truncate" title={n.failureReason}>{n.failureReason}</p>
                      )}
                    </td>
                    <td className="px-6 py-5">
                      <div className="font-bold text-slate-900 dark:text-white capitalize">{n.type.replace("_", " ").toLowerCase()}</div>
                      <div className="text-slate-500 dark:text-slate-400 text-xs font-semibold tracking-wider mt-0.5">{n.channel}</div>
                    </td>
                    <td className="px-6 py-5 font-bold text-slate-900 dark:text-white">
                      {n.recipient}
                    </td>
                    <td className="px-6 py-5 max-w-xs cursor-help">
                      <p className="truncate text-slate-600 dark:text-slate-300 font-medium" title={n.message}>{n.message}</p>
                    </td>
                    <td className="px-6 py-5 text-xs font-medium text-slate-500 dark:text-slate-400 space-y-1">
                      <div>Created: {new Date(n.createdAt).toLocaleString()}</div>
                      {n.sentAt && <div className="text-indigo-600 dark:text-indigo-400 font-semibold">Sent: {new Date(n.sentAt).toLocaleString()}</div>}
                      {n.attemptCount > 1 && <div className="text-amber-600 dark:text-amber-500 font-semibold">Attempts: {n.attemptCount}</div>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
