import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bell, CheckCircle2, Clock3, Search, SendHorizonal, XCircle } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";
import { notificationMessageLabel, notificationTypeLabel } from "@/lib/subscriptionLabels";
import { cn } from "@/lib/utils";

export default function Notifications() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [channelFilter, setChannelFilter] = useState("ALL");

  const { data: notifications, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const res = await api.get("/notifications");
      return res.data;
    },
  });

  if (isLoading) {
    return <div className="p-8">Loading notifications...</div>;
  }

  const filteredNotifications =
    notifications?.filter((n: any) => {
      const matchesSearch =
        n.recipient.toLowerCase().includes(searchTerm.toLowerCase()) ||
        n.message.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesStatus = statusFilter === "ALL" || n.status === statusFilter;
      const matchesChannel = channelFilter === "ALL" || n.channel === channelFilter;

      return matchesSearch && matchesStatus && matchesChannel;
    }) || [];

  const sentCount = notifications?.filter((item: any) => item.status === "SENT").length ?? 0;
  const pendingCount = notifications?.filter((item: any) => item.status === "PENDING" || item.status === "RETRYING").length ?? 0;
  const failedCount = notifications?.filter((item: any) => item.status === "FAILED").length ?? 0;

  return (
    <div className="mx-auto max-w-7xl space-y-8 p-8">
      <section className="billmygym-shell rounded-[2rem] p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Admin Inbox</div>
            <h1 className="mt-2 flex items-center gap-3 text-4xl font-black tracking-tight text-slate-950">
              <Bell className="h-8 w-8 text-emerald-700" />
              Notifications Log
            </h1>
            <p className="mt-2 max-w-2xl text-sm font-medium leading-6 text-slate-600">
              Review every digest and admin message delivered from the reminder engine, with delivery state and retry context.
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <StatChip label="Sent" value={sentCount} tone="emerald" />
            <StatChip label="Pending" value={pendingCount} tone="amber" />
            <StatChip label="Failed" value={failedCount} tone="rose" />
          </div>
        </div>
      </section>

      <section className="glass-panel rounded-[1.8rem] p-4">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="relative w-full flex-1 md:max-w-md">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Search by recipient or message..."
              className="h-11 rounded-xl border-black/5 bg-white/60 pl-11"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <div className="flex w-full gap-4 md:w-auto">
            <select
              className="h-11 flex-1 rounded-xl border border-black/5 bg-white/60 px-4 text-sm font-semibold text-slate-700 md:flex-none"
              value={channelFilter}
              onChange={(e) => setChannelFilter(e.target.value)}
            >
              <option value="ALL">All Channels</option>
              <option value="WHATSAPP">WhatsApp</option>
              <option value="EMAIL">Email</option>
            </select>
            <select
              className="h-11 flex-1 rounded-xl border border-black/5 bg-white/60 px-4 text-sm font-semibold text-slate-700 md:flex-none"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="SENT">Sent</option>
              <option value="FAILED">Failed</option>
              <option value="PENDING">Pending</option>
              <option value="RETRYING">Retrying</option>
            </select>
          </div>
        </div>
      </section>

      <section className="glass-panel overflow-hidden rounded-[1.9rem]">
        {filteredNotifications.length === 0 ? (
          <div className="p-16 text-center text-sm font-semibold text-slate-500">No notifications found matching your filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-black/5 bg-white/30 text-slate-500">
                <tr>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Status</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Type / Channel</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Recipient</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Message</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Timing</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5">
                {filteredNotifications.map((n: any) => (
                  <tr key={n.id} className="transition-colors hover:bg-white/50">
                    <td className="px-6 py-5">
                      <NotificationStatus status={n.status} />
                      {n.status === "FAILED" && n.failureReason && (
                        <p className="mt-2 max-w-[180px] truncate text-xs font-semibold text-rose-600" title={n.failureReason}>
                          {n.failureReason}
                        </p>
                      )}
                    </td>
                    <td className="px-6 py-5">
                      <div className="font-black capitalize text-slate-950">{notificationTypeLabel(n.type)}</div>
                      <div className="mt-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{n.channel}</div>
                    </td>
                    <td className="px-6 py-5 font-semibold text-slate-800">{n.recipient}</td>
                    <td className="px-6 py-5 max-w-sm">
                      <p className="truncate font-medium text-slate-600" title={notificationMessageLabel(n.message)}>{notificationMessageLabel(n.message)}</p>
                    </td>
                    <td className="px-6 py-5 text-xs font-semibold text-slate-500">
                      <div>Created: {new Date(n.createdAt).toLocaleString()}</div>
                      {n.sentAt && <div className="mt-1 text-emerald-700">Sent: {new Date(n.sentAt).toLocaleString()}</div>}
                      {n.attemptCount > 1 && <div className="mt-1 text-amber-700">Attempts: {n.attemptCount}</div>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function StatChip({ label, value, tone }: { label: string; value: number; tone: "emerald" | "amber" | "rose" }) {
  const styles = {
    emerald: "bg-emerald-50 border-emerald-100 text-emerald-800",
    amber: "bg-amber-50 border-amber-100 text-amber-800",
    rose: "bg-rose-50 border-rose-100 text-rose-800",
  }[tone];

  return (
    <div className={cn("rounded-[1.3rem] border px-4 py-3", styles)}>
      <div className="text-[10px] font-black uppercase tracking-[0.18em] opacity-70">{label}</div>
      <div className="mt-1 text-2xl font-black tracking-tight">{value}</div>
    </div>
  );
}

function NotificationStatus({ status }: { status: string }) {
  if (status === "SENT") {
    return (
      <Badge className="rounded-full border-emerald-200 bg-emerald-100 px-3 py-1 font-black tracking-[0.14em] text-emerald-800">
        <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" /> Sent
      </Badge>
    );
  }
  if (status === "FAILED") {
    return (
      <Badge className="rounded-full border-rose-200 bg-rose-100 px-3 py-1 font-black tracking-[0.14em] text-rose-800">
        <XCircle className="mr-1.5 h-3.5 w-3.5" /> Failed
      </Badge>
    );
  }
  return (
    <Badge className="rounded-full border-amber-200 bg-amber-100 px-3 py-1 font-black tracking-[0.14em] text-amber-800">
      {status === "RETRYING" ? <SendHorizonal className="mr-1.5 h-3.5 w-3.5" /> : <Clock3 className="mr-1.5 h-3.5 w-3.5" />}
      {status === "RETRYING" ? "Retrying" : "Pending"}
    </Badge>
  );
}
