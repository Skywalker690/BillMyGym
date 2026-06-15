import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import { AlertTriangle, CheckCircle2, ChevronDown, Clock3, Info, MessageCircleMore, Pencil, PhoneOff, Plus, RefreshCw, Search, Send, ShieldAlert, Sparkles, Trash2, XCircle, Zap } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/context/AuthContext";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import api from "@/lib/api";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";
import { readCachedSubscriptionStatus, type CachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";
import { DEMO_ORGANIZATION_ID, demoMembersPage, demoReminderPreview } from "@/lib/demoWorkspaceData";
import { readDemoBaileysRecipient, readDemoBaileysUrl, runDemoBaileysReminders } from "@/lib/demoBaileysClient";

const PAGE_SIZE = 40;
const INDIAN_TIME_ZONE = "Asia/Kolkata";

type MemberListSubscriptionStatus = CachedSubscriptionStatus & {
  customReminderUsage?: number | string | null;
  customReminderLimit?: number | string | null;
  planAccess?: unknown;
};

const formatDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        timeZone: INDIAN_TIME_ZONE,
      }).format(new Date(`${value}T00:00:00+05:30`))
    : "-";

const money = (value?: number | string | null) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));

export default function MembersList() {
  const { effectivePlanAccess, isDemoWorkspace } = useDemoPlanPreview();
  const { user } = useAuth();
  const [searchInput, setSearchInput] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [memberTab, setMemberTab] = useState<"ALL" | "ACTIVE" | "DEACTIVATED">("ACTIVE");
  const [page, setPage] = useState(0);
  const [quickRenewMember, setQuickRenewMember] = useState<any | null>(null);
  const [deactivateMember, setDeactivateMember] = useState<any | null>(null);
  const [reminderMember, setReminderMember] = useState<any | null>(null);
  const queryClient = useQueryClient();
  const isDemoDirectory = isDemoWorkspace || user?.organizationId === DEMO_ORGANIZATION_ID;

  const { data, isLoading } = useQuery({
    queryKey: ["members", page, appliedSearch, memberTab],
    queryFn: async () => {
      const res = await api.get("/members", {
        params: {
          page,
          size: PAGE_SIZE,
          search: appliedSearch || undefined,
          memberStatus: memberTab === "ALL" ? undefined : memberTab,
        },
      });
      return res.data;
    },
    enabled: !isDemoDirectory,
  });

  const { data: membershipPlans } = useQuery<any[]>({
    queryKey: ["membershipPlans", user?.organizationId],
    queryFn: async () => {
      if (isDemoDirectory) return [];
      const res = await api.get("/membership-plans");
      return res.data.plans;
    },
    enabled: Boolean(user?.organizationId),
  });

  const subscriptionStatus = readCachedSubscriptionStatus() as MemberListSubscriptionStatus | null;

  const reminderPreviewQuery = useQuery({
    queryKey: ["whatsappReminderPreview", reminderMember?.id],
    queryFn: async () => {
      const res = await api.get(`/members/${reminderMember.id}/whatsapp-reminder/preview`);
      return res.data;
    },
    enabled: Boolean(reminderMember?.id) && !isDemoDirectory,
    retry: false,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => isDemoDirectory ? Promise.resolve({ data: { success: true } } as any) : api.delete(`/members/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      setDeactivateMember(null);
      toast.success("Member deactivated successfully");
    },
    onError: () => toast.error("Failed to deactivate member"),
  });

  const quickRenewMutation = useMutation({
    mutationFn: async ({ member, planId }: { member: any; planId: number }) => {
      if (isDemoDirectory) {
        return { data: { success: true } };
      }
      const membership = member.currentMembership;
      if (!planId) {
        throw new Error("A membership plan must be selected for quick renew");
      }
      const startDate = resolveQuickRenewStartDate(membership?.endDate);
      const membershipResponse = await api.post(
        `/members/${member.id}/memberships`,
        {
          durationMonths: 1,
          membershipPlanId: planId,
          startDate,
        },
        {
          headers: {
            "X-Idempotency-Key": crypto.randomUUID(),
          },
        }
      );
      const renewalAmount = Number(membershipResponse.data?.planAmount ?? 0);

      await api.post(
        "/payments",
        {
          membershipId: membershipResponse.data.id,
          amount: renewalAmount,
          paymentDate: new Date().toISOString().slice(0, 10),
          paymentType: membership ? "RENEWAL" : "NEW_MEMBERSHIP",
          paymentStatus: "PAID",
        },
        {
          headers: {
            "X-Idempotency-Key": crypto.randomUUID(),
          },
        }
      );
    },
    onSuccess: (_, member) => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      setQuickRenewMember(null);
      toast.success(`${member.fullName} renewed for 1 month`);
    },
    onError: (error: any) => {
      toast.error(error?.message || "Failed to renew member");
    },
  });

  const sendReminderMutation = useMutation({
    mutationFn: async (memberOrId: any) => {
      if (!isDemoDirectory) {
        return api.post(`/members/${memberOrId}/whatsapp-reminder/send`);
      }
      const member = typeof memberOrId === "object"
        ? memberOrId
        : reminderMember;
      const preview = demoReminderPreview(member);
      const result = await runDemoBaileysReminders([{
        memberId: member?.id,
        memberName: member?.fullName,
        phone: readDemoBaileysRecipient() || member?.phone,
        dueDate: preview.dueDate,
        pendingAmount: preview.pendingAmount,
        message: preview.message,
      }], readDemoBaileysUrl());
      return {
        data: {
          sent: result.messagesSent > 0,
          failureReason: result.failedMessages > 0 ? "Connect demo WhatsApp from the Reminders page before sending." : undefined,
        },
      } as any;
    },
    onSuccess: (response) => {
      const result = response.data;
      queryClient.invalidateQueries({ queryKey: ["reminderReport"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      if (result?.sent) {
        setReminderMember(null);
        toast.success("Reminder sent successfully");
      } else {
        toast.error(result?.failureReason || "Unable to send reminder");
      }
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Unable to send reminder");
    },
  });

  const demoData = useMemo(() => demoMembersPage({
    page,
    size: PAGE_SIZE,
    search: appliedSearch,
    memberStatus: memberTab,
  }), [appliedSearch, memberTab, page]);
  const dataForView = isDemoDirectory ? demoData : data;
  const reminderPreview = isDemoDirectory && reminderMember ? demoReminderPreview(reminderMember) : reminderPreviewQuery.data;

  const members = useMemo(() => {
    const items = [...(dataForView?.members ?? [])];
    items.sort((left: any, right: any) => {
      const rankDifference = renewalPriority(left) - renewalPriority(right);
      if (rankDifference !== 0) {
        return rankDifference;
      }

      const leftDate = resolveMembershipSortDate(left.currentMembership);
      const rightDate = resolveMembershipSortDate(right.currentMembership);
      if (leftDate !== rightDate) {
        return leftDate - rightDate;
      }

      return String(left.fullName ?? "").localeCompare(String(right.fullName ?? ""));
    });
    return items;
  }, [dataForView?.members]);
  const summary = dataForView?.summary ?? {};
  const memberLimit = summary.memberLimit;
  const canUseMemberMessaging = isDemoDirectory || Boolean(effectivePlanAccess(subscriptionStatus?.planAccess).canUsePremiumFeatures);
  const memberTabs = [
    { id: "ALL" as const, label: "All Members", count: summary.totalMembers ?? 0 },
    { id: "ACTIVE" as const, label: "Active", count: summary.activeMembers ?? 0 },
    { id: "DEACTIVATED" as const, label: "Deactivated", count: summary.deactivatedMembers ?? 0 },
  ];

  if (isLoading && !isDemoDirectory) {
    return <div className="p-8">Loading members...</div>;
  }

  return (
    <div className="mx-auto min-w-0 max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
      <section className="billmygym-shell rounded-[2rem] p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Member Operations</div>
            <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">Members Directory</h1>
            <p className="mt-2 max-w-2xl text-sm font-medium leading-6 text-slate-600">
              Members needing due-soon or overdue attention are shown first.
            </p>
          </div>
          <Link to="/app/members/new">
            <Button className="h-12 rounded-full bg-slate-950 px-6 font-bold text-white hover:bg-slate-800">
              <Plus className="mr-2 h-4 w-4" />
              Add Member
            </Button>
          </Link>
        </div>
      </section>

      <section className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
        <SummaryTile label="Total Members" value={summary.totalMembers} tone="slate" />
        <SummaryTile label="Active Members" value={summary.activeMembers} tone="emerald" />
        <SummaryTile label="Deactivated Members" value={summary.deactivatedMembers} tone="indigo" />
        <SummaryTile label="Due Today" value={summary.expiringSoonMemberships} tone="amber" />
      </section>

      <section className="glass-panel rounded-[1.8rem] p-4">
        <div className="flex flex-col gap-4">
          <div className="flex flex-wrap gap-2">
            {memberTabs.map((tab) => {
              const active = memberTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => {
                    setPage(0);
                    setMemberTab(tab.id);
                  }}
                  className={cn(
                    "inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-black transition-colors",
                    active
                      ? "border-emerald-200 bg-emerald-100 text-emerald-800 shadow-sm"
                      : "border-black/5 bg-white/60 text-slate-600 hover:bg-white",
                  )}
                >
                  <span>{tab.label}</span>
                  <span className={cn("rounded-full px-2 py-0.5 text-xs", active ? "bg-white/85 text-emerald-700" : "bg-slate-100 text-slate-500")}>
                    {tab.count}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex w-full flex-1 flex-col gap-3 md:max-w-2xl md:flex-row">
              <div className="relative flex-1">
                <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <Input
                  placeholder="Search by name or phone..."
                  className="h-11 rounded-xl border-black/5 bg-white/60 pl-11"
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      setPage(0);
                      setAppliedSearch(searchInput.trim());
                    }
                  }}
                />
              </div>
              <Button
                type="button"
                onClick={() => {
                  setPage(0);
                  setAppliedSearch(searchInput.trim());
                }}
                className="h-11 rounded-xl bg-slate-950 px-5 font-bold text-white hover:bg-slate-800"
              >
                <Search className="mr-2 h-4 w-4" />
                Search
              </Button>
            </div>
          </div>
        </div>
      </section>

      <section className="glass-panel overflow-hidden rounded-[1.9rem]">
        {members.length === 0 ? (
          <div className="p-14 text-center text-sm font-semibold text-slate-500">No members found matching your filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-[600px] lg:min-w-[800px] w-full text-left text-xs sm:text-sm">
              <thead className="border-b border-black/5 bg-white/30 text-slate-500">
                <tr>
                  <th className="px-2 sm:px-6 py-2 sm:py-4 text-[10px] sm:text-xs font-black uppercase tracking-[0.18em] sm:tracking-[0.2em]">Member</th>
                  <th className="px-2 sm:px-6 py-2 sm:py-4 text-[10px] sm:text-xs font-black uppercase tracking-[0.18em] sm:tracking-[0.2em]">Period</th>
                  <th className="px-2 sm:px-6 py-2 sm:py-4 text-[10px] sm:text-xs font-black uppercase tracking-[0.18em] sm:tracking-[0.2em]">Membership</th>
                  <th className="px-2 sm:px-6 py-2 sm:py-4 text-right text-[10px] sm:text-xs font-black uppercase tracking-[0.18em] sm:tracking-[0.2em]">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5">
                {members.map((member: any) => {
                  const membership = member.currentMembership;
                  const status = membership?.displayStatus ?? (membership?.expired ? "EXPIRED" : membership?.expiringSoon ? "EXPIRING_SOON" : membership?.status ?? "NO_MEMBERSHIP");
                  return (
                    <tr key={member.id} className="transition-colors hover:bg-white/50">
                      <td className="min-w-[140px] sm:min-w-[200px] px-2 sm:px-6 py-2 sm:py-4">
                        <div className="flex items-start justify-between gap-2 sm:gap-3">
                          <div className="min-w-0">
                            <div className="font-black text-slate-950 text-sm sm:text-base">{member.fullName}</div>
                            <div className="mt-0.5 sm:mt-1 text-[10px] sm:text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">{member.phone || "No phone"}</div>
                            <div className="mt-1 sm:mt-2">
                              <Badge
                                className={cn(
                                  "rounded-full border-transparent px-2 sm:px-2.5 py-0.5 sm:py-1 text-[8px] sm:text-xs font-black tracking-[0.12em] sm:tracking-[0.14em]",
                                  member.status === "DEACTIVATED" ? "bg-rose-100 text-rose-700" : "bg-emerald-100 text-emerald-700",
                                )}
                              >
                                {member.status}
                              </Badge>
                            </div>
                          </div>
                          <div className="flex shrink-0 items-center gap-1 sm:gap-2">
                            {canSendMemberWhatsApp(member, canUseMemberMessaging) && (
                              <button
                                type="button"
                                className={cn(
                                  "inline-flex h-7 sm:h-9 w-7 sm:w-9 items-center justify-center rounded-full border shadow-sm transition-colors",
                                  hasMemberPhone(member)
                                    ? "border-emerald-100 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                                    : "cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400"
                                )}
                                aria-label={`Send WhatsApp reminder to ${member.fullName}`}
                                title={hasMemberPhone(member) ? "Send WhatsApp reminder" : "Add a phone number to send WhatsApp reminder"}
                                onClick={() => {
                                  if (!hasMemberPhone(member)) {
                                    return;
                                  }
                                  sendReminderMutation.reset();
                                  setReminderMember(member);
                                }}
                                disabled={!hasMemberPhone(member)}
                              >
                                <MessageCircleMore className="h-3.5 sm:h-4 w-3.5 sm:w-4" />
                              </button>
                            )}
                            <Link
                              to={`/app/members/${member.id}/edit`}
                              className="inline-flex h-7 sm:h-9 w-7 sm:w-9 items-center justify-center rounded-full border border-black/5 bg-white/70 text-slate-500 shadow-sm transition-colors hover:bg-white hover:text-emerald-700"
                              aria-label={`Edit ${member.fullName}`}
                              title="Edit member"
                            >
                              <Pencil className="h-3.5 sm:h-4 w-3.5 sm:w-4" />
                            </Link>
                          </div>
                        </div>
                      </td>
                      <td className="px-2 sm:px-6 py-2 sm:py-4 text-[10px] sm:text-xs font-semibold text-slate-500">
                        <div>{formatDate(membership?.startDate)}</div>
                        <div className="mt-0.5 sm:mt-1">to {formatDate(membership?.endDate)}</div>
                      </td>
                      <td className="px-2 sm:px-6 py-2 sm:py-4">
                        <StatusBadge status={status} />
                      </td>
                      <td className="px-2 sm:px-6 py-2 sm:py-4">
                        <div className="flex flex-wrap justify-end gap-1 sm:gap-2">
                          {canRenewMember(member) && (
                            <Button
                              type="button"
                              variant="outline"
                              className="h-8 sm:h-10 rounded-full border-emerald-200 bg-emerald-50 px-2 sm:px-4 text-[10px] sm:text-sm font-bold text-emerald-700 shadow-sm transition-colors hover:bg-emerald-100 hover:text-emerald-800"
                              onClick={() => setQuickRenewMember(member)}
                              disabled={quickRenewMutation.isPending}
                            >
                              <Zap className="mr-1 sm:mr-2 h-3 sm:h-4 w-3 sm:w-4" />
                              <span className="hidden sm:inline">Quick Renew</span>
                              <span className="sm:hidden">Renew</span>
                            </Button>
                          )}
                          <DropdownMenu>
                            <DropdownMenuTrigger
                              render={
                                <Button
                                  type="button"
                                  variant="outline"
                                  className="h-8 sm:h-10 rounded-full border-black/10 bg-white/80 px-2 sm:px-4 text-[10px] sm:text-sm font-bold text-slate-700 shadow-sm transition-colors hover:bg-white hover:text-slate-950"
                                >
                                  <span className="hidden sm:inline">Actions</span>
                                  <span className="sm:hidden">⋯</span>
                                  <ChevronDown className="ml-1 sm:ml-2 h-3 sm:h-4 w-3 sm:w-4" />
                                </Button>
                              }
                            />
                            <DropdownMenuContent align="end" className="w-48 rounded-2xl border border-white/80 bg-white/95 p-2 shadow-[0_18px_40px_-24px_rgba(15,23,42,0.32)] backdrop-blur-xl">
                              {canRenewMember(member) && (
                                <DropdownMenuItem
                                  className="rounded-xl px-3 py-2 font-semibold"
                                  onClick={() => {
                                    window.location.href = `/app/members/${member.id}/renew`;
                                  }}
                                >
                                  <RefreshCw className="h-4 w-4" />
                                  Renew
                                </DropdownMenuItem>
                              )}
                              {member.status !== "DEACTIVATED" && (
                                <DropdownMenuItem
                                  variant="destructive"
                                  className="rounded-xl px-3 py-2 font-semibold"
                                  onClick={() => setDeactivateMember(member)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                  Deactivate
                                </DropdownMenuItem>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm font-semibold text-slate-500">
          Page {(dataForView?.page ?? 0) + 1} of {Math.max(dataForView?.totalPages ?? 1, 1)} · {dataForView?.totalElements ?? 0} members
        </p>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" disabled={dataForView?.first} onClick={() => setPage((value) => Math.max(value - 1, 0))}>
            Previous
          </Button>
          <Button variant="outline" disabled={dataForView?.last} onClick={() => setPage((value) => value + 1)}>
            Next
          </Button>
        </div>
      </div>

      <section className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-2 lg:items-start">
        <CustomReminderLimitCard
          canUseMemberMessaging={canUseMemberMessaging}
          used={Number(subscriptionStatus?.customReminderUsage ?? 0)}
          limit={subscriptionStatus?.customReminderLimit == null ? null : Number(subscriptionStatus.customReminderLimit)}
        />
        <MemberLimitCard
          memberLimit={memberLimit}
          totalMembers={summary.totalMembers ?? 0}
          deactivatedMembers={summary.deactivatedMembers ?? 0}
        />
      </section>

      {quickRenewMember && (
        <QuickRenewModal
          member={quickRenewMember}
          membershipPlans={membershipPlans ?? []}
          isSubmitting={quickRenewMutation.isPending}
          onCancel={() => {
            if (!quickRenewMutation.isPending) {
              setQuickRenewMember(null);
            }
          }}
          onConfirm={(planId) => quickRenewMutation.mutate({ member: quickRenewMember, planId })}
        />
      )}

      {deactivateMember && (
        <DeactivateMemberModal
          member={deactivateMember}
          isSubmitting={deleteMutation.isPending}
          onCancel={() => {
            if (!deleteMutation.isPending) {
              setDeactivateMember(null);
            }
          }}
          onConfirm={() => deleteMutation.mutate(deactivateMember.id)}
        />
      )}

      <AnimatePresence>
        {reminderMember && (
          <WhatsappReminderModal
            member={reminderMember}
            preview={reminderPreview}
            isLoading={!isDemoDirectory && reminderPreviewQuery.isLoading}
            error={isDemoDirectory ? null : reminderPreviewQuery.error}
            isSubmitting={sendReminderMutation.isPending}
            hasFailed={sendReminderMutation.data?.data?.sent === false}
            onCancel={() => {
              if (!sendReminderMutation.isPending) {
                setReminderMember(null);
                sendReminderMutation.reset();
              }
            }}
            onConfirm={() => sendReminderMutation.mutate(reminderMember)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function renewalPriority(member: any) {
  if (member.status === "DEACTIVATED") {
    return 5;
  }

  const membership = member.currentMembership;
  if (!membership) {
    return 0;
  }
  if (membership.status === "OVERDUE") {
    return 0;
  }
  if (membership.expiringSoon) {
    return 2;
  }
  if (membership.status === "ACTIVE") {
    return 3;
  }
  return 4;
}

function resolveMembershipSortDate(membership?: any) {
  if (!membership?.endDate) {
    return Number.MIN_SAFE_INTEGER;
  }
  const timestamp = new Date(membership.endDate).getTime();
  return Number.isNaN(timestamp) ? Number.MIN_SAFE_INTEGER : timestamp;
}

function canRenewMember(member: any) {
  return member.status !== "DEACTIVATED" && member.currentMembership?.status !== "ACTIVE";
}

function canSendMemberWhatsApp(member: any, canUseMemberMessaging: boolean) {
  return canUseMemberMessaging && member.status === "ACTIVE" && member.currentMembership?.status === "OVERDUE";
}

function hasMemberPhone(member: any) {
  return Boolean(member?.phone && String(member.phone).trim());
}

function resolveQuickRenewStartDate(currentEndDate?: string) {
  if (!currentEndDate) {
    return todayInIndiaIso();
  }
  const [year, month, day] = currentEndDate.split("-").map(Number);
  const baseDate = new Date(Date.UTC(year, month - 1, day));
  baseDate.setUTCDate(baseDate.getUTCDate() + 1);
  return baseDate.toISOString().slice(0, 10);
}

function todayInIndiaIso() {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: INDIAN_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

function WhatsappReminderModal({
  member,
  preview,
  isLoading,
  error,
  isSubmitting,
  hasFailed,
  onCancel,
  onConfirm,
}: {
  member: any;
  preview?: any;
  isLoading: boolean;
  error: unknown;
  isSubmitting: boolean;
  hasFailed: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const messageLines = String(preview?.message ?? "").split("\n");
  const blockedMessage = preview?.disabledReason || (error ? "Unable to prepare WhatsApp reminder" : null);
  const sendDisabled = isLoading || Boolean(error) || !preview?.canSend || isSubmitting;

  return (
    <div className="fixed inset-0 z-[130] flex items-end justify-center overflow-y-auto p-0 sm:items-center sm:p-4 lg:p-6">
      <motion.button
        type="button"
        className="absolute inset-0 bg-slate-950/45 backdrop-blur-md"
        aria-label="Close WhatsApp reminder dialog"
        onClick={onCancel}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
      />
      <motion.div
        className="relative z-10 flex max-h-[calc(100dvh-0.75rem)] w-full max-w-3xl flex-col overflow-hidden rounded-t-[2rem] border border-white/80 bg-[linear-gradient(145deg,rgba(255,255,255,0.98),rgba(236,253,245,0.96))] shadow-[0_30px_90px_-30px_rgba(15,23,42,0.52)] sm:max-h-[calc(100dvh-2rem)] sm:rounded-[2rem]"
        initial={{ opacity: 0, y: 48, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 36, scale: 0.98 }}
        transition={{ type: "spring", stiffness: 260, damping: 26 }}
      >
        <div className="absolute -right-16 -top-16 h-44 w-44 rounded-full bg-emerald-300/30 blur-3xl" />
        <div className="absolute bottom-0 left-8 h-36 w-36 rounded-full bg-sky-200/30 blur-3xl" />
        <div className="relative z-10 overflow-y-auto p-5 sm:p-6 lg:p-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">
                <MessageCircleMore className="h-3.5 w-3.5" />
                WhatsApp Reminder
              </div>
              <h2 className="mt-4 text-2xl font-black tracking-tight text-slate-950">Send overdue reminder?</h2>
            </div>
            <button
              type="button"
              className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-white/80 bg-white/80 text-slate-500 shadow-sm transition-colors hover:bg-white hover:text-slate-900"
              onClick={onCancel}
              aria-label="Close reminder dialog"
            >
              <XCircle className="h-4 w-4" />
            </button>
          </div>

          <div className="mt-5 rounded-[1.35rem] border border-white/80 bg-white/78 p-4 shadow-sm">
            <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Member</div>
              <div className="mt-2 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                <div className="min-w-0 break-words text-xl font-black text-slate-950">{preview?.memberName ?? member.fullName}</div>
              <div className="shrink-0 text-sm font-bold text-slate-500">{preview?.phone ?? member.phone ?? "No phone number"}</div>
            </div>
          </div>

          <div className="mt-4 overflow-hidden rounded-[1.6rem] border border-emerald-100 bg-[#e7f7ef] shadow-sm">
            <div className="flex items-center gap-2 bg-[#075e54] px-4 py-3 text-white">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white/15">
                <MessageCircleMore className="h-4 w-4" />
              </div>
              <div>
                <div className="text-sm font-black">{preview?.gymName ?? "Gym"}</div>
                <div className="text-[11px] font-semibold text-white/75">WhatsApp preview</div>
              </div>
            </div>
            <div className="p-4">
              {isLoading ? (
                <div className="rounded-2xl bg-white px-4 py-5 text-sm font-semibold text-slate-500 shadow-sm">
                  Preparing reminder preview...
                </div>
              ) : error ? (
                <div className="rounded-2xl bg-white px-4 py-5 text-sm font-semibold text-rose-700 shadow-sm">
                  {member.phone ? "Unable to prepare WhatsApp reminder." : "This member does not have a phone number yet. Add it from Edit Member to enable WhatsApp reminders."}
                </div>
              ) : (
                <div className="max-w-full break-words rounded-2xl rounded-tl-sm bg-white px-4 py-3 text-sm font-semibold leading-6 text-slate-800 shadow-sm sm:max-w-[92%]">
                  {messageLines.map((line, index) => (
                    <div key={`${line}-${index}`} className={line.trim() === "" ? "h-3" : ""}>
                      {line}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <ReminderMetric label="Due Date" value={formatReminderDate(preview?.dueDate)} />
            <ReminderMetric label="Due Amount" value={money(preview?.pendingAmount)} />
          </div>

          {preview?.monthlyCustomReminderLimit ? (
            <div className="mt-4 rounded-[1.25rem] border border-emerald-100 bg-emerald-50/80 p-4 shadow-sm">
              <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Custom Reminder Usage</div>
              <div className="mt-2 grid gap-3 sm:grid-cols-[auto_1fr] sm:items-center">
                <div className="text-lg font-black text-slate-950">
                  {preview?.monthlyCustomReminderUsed ?? 0} / {preview?.monthlyCustomReminderLimit}
                </div>
                <div className="min-w-0 rounded-full border border-white/80 bg-white/85 px-3 py-1.5 text-sm font-bold text-emerald-700 sm:justify-self-end">
                  {preview?.monthlyCustomReminderRemaining ?? 0} remaining this month
                </div>
              </div>
            </div>
          ) : null}

          {blockedMessage && (
            <div className="mt-4 flex items-start gap-3 rounded-[1.25rem] border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-900">
              <PhoneOff className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{blockedMessage}</span>
            </div>
          )}

          <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-full border-white/80 bg-white/80 px-6 font-bold"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="button"
              className="h-11 rounded-full bg-[#128c7e] px-6 font-bold text-white hover:bg-[#0f766a]"
              onClick={onConfirm}
              disabled={sendDisabled}
            >
              <Send className="mr-2 h-4 w-4" />
              {isSubmitting ? "Sending..." : hasFailed ? "Retry Reminder" : "Send Reminder"}
            </Button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

function ReminderMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.25rem] border border-white/80 bg-white/75 p-4 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-black text-slate-950">{value}</div>
    </div>
  );
}

function formatReminderDate(value?: string) {
  return value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: INDIAN_TIME_ZONE,
      }).format(new Date(`${value}T00:00:00+05:30`))
    : "-";
}

function QuickRenewModal({
  member,
  membershipPlans,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  member: any;
  membershipPlans: any[];
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: (planId: number) => void;
}) {
  const membership = member.currentMembership;
  const [selectedPlanId, setSelectedPlanId] = useState<number>(membership?.membershipPlanId || (membershipPlans?.[0]?.id ?? 0));
  const selectedPlan = membershipPlans?.find((p) => p.id === selectedPlanId);
  const planName = selectedPlan?.name ?? membership?.planName ?? "Current plan";
  const monthlyPlanPrice = Number(selectedPlan?.price ?? membership?.monthlyPlanPrice ?? membership?.planAmount ?? 0);
  const nextStartDate = resolveQuickRenewStartDate(membership?.endDate);
  const nextEndDate = new Date(`${nextStartDate}T00:00:00`);
  nextEndDate.setMonth(nextEndDate.getMonth() + 1);
  nextEndDate.setDate(nextEndDate.getDate() - 1);

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/45 backdrop-blur-md"
        aria-label="Close quick renew dialog"
        onClick={onCancel}
      />
      <div className="relative z-10 w-full max-w-2xl overflow-hidden rounded-[2rem] border border-white/80 bg-[linear-gradient(145deg,rgba(255,255,255,0.97),rgba(236,253,245,0.94))] shadow-[0_30px_90px_-30px_rgba(15,23,42,0.45)]">
        <div className="absolute -right-14 -top-16 h-44 w-44 rounded-full bg-emerald-300/35 blur-3xl" />
        <div className="absolute bottom-0 left-10 h-36 w-36 rounded-full bg-amber-200/40 blur-3xl" />
        <div className="relative z-10 p-8">
          <div className="inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">
            <Sparkles className="h-3.5 w-3.5" />
            Quick Renew
          </div>
          <h2 className="mt-5 text-3xl font-black tracking-tight text-slate-950">Renew {member.fullName} instantly?</h2>

          <div className="mt-6 grid grid-cols-2 gap-4 md:grid-cols-3">
            <RenewMetric label="Member" value={member.fullName} tone="slate" />
            <RenewMetric label="Current Expiry" value={formatDate(membership?.endDate)} tone="amber" />
            <RenewMetric label="Next Expiry" value={formatDate(nextEndDate.toISOString().slice(0, 10))} tone="emerald" className="col-span-2 md:col-span-1" />
          </div>

          <div className="mt-5 rounded-[1.6rem] border border-white/80 bg-white/80 p-5 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Collected On Confirm</div>
                <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">
                  INR {monthlyPlanPrice.toLocaleString("en-IN")}
                </div>
              </div>
              <div className="flex flex-col gap-2">
                {membershipPlans && membershipPlans.length > 1 ? (
                  <select
                    value={selectedPlanId}
                    onChange={(e) => setSelectedPlanId(Number(e.target.value))}
                    className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-800 outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent appearance-none cursor-pointer pr-10 relative"
                    style={{ backgroundImage: `url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23065f46' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e")`, backgroundRepeat: "no-repeat", backgroundPosition: "right 1rem center", backgroundSize: "1em" }}
                  >
                    {(membershipPlans ?? []).map((plan) => (
                      <option key={plan.id} value={plan.id}>
                        {plan.name}
                      </option>
                    ))}
                    {!membershipPlans?.length && (
                      <option value={membership?.membershipPlanId}>{planName}</option>
                    )}
                  </select>
                ) : (
                  <div className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-800 text-right">
                    {planName}
                  </div>
                )}
                <div className="text-[11px] font-semibold text-slate-500 text-right">
                  Start date: {formatDate(nextStartDate)}
                </div>
              </div>
            </div>
          </div>

          <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-full border-white/80 bg-white/80 px-6 font-bold"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              No
            </Button>
            <Button
              type="button"
              className="h-11 rounded-full bg-emerald-600 px-6 font-bold text-white hover:bg-emerald-700"
              onClick={() => onConfirm(selectedPlanId)}
              disabled={isSubmitting || !selectedPlanId}
            >
              {isSubmitting ? "Renewing..." : "Yes, Quick Renew"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function DeactivateMemberModal({
  member,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  member: any;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const membership = member.currentMembership;
  const status = membership?.expiringSoon ? "Due Today" : membership?.status?.replace("_", " ") ?? "No Membership";

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-slate-950/55 backdrop-blur-md"
        aria-label="Close deactivate member dialog"
        onClick={onCancel}
      />
      <div className="relative z-10 w-full max-w-2xl overflow-hidden rounded-[2rem] border border-white/80 bg-[linear-gradient(145deg,rgba(255,255,255,0.98),rgba(255,241,242,0.94))] shadow-[0_30px_90px_-30px_rgba(15,23,42,0.45)]">
        <div className="absolute -right-16 -top-16 h-44 w-44 rounded-full bg-rose-300/35 blur-3xl" />
        <div className="absolute bottom-0 left-8 h-36 w-36 rounded-full bg-amber-200/35 blur-3xl" />
        <div className="relative z-10 p-8">
          <div className="inline-flex items-center gap-2 rounded-full border border-rose-200 bg-rose-50 px-4 py-2 text-[11px] font-black uppercase tracking-[0.24em] text-rose-700">
            <AlertTriangle className="h-3.5 w-3.5" />
            Member Deactivation
          </div>
          <h2 className="mt-5 text-3xl font-black tracking-tight text-slate-950">Deactivate {member.fullName}?</h2>
          <p className="mt-3 max-w-xl text-sm font-semibold leading-6 text-slate-600">
            This removes the member from the active directory and renewal workflow while preserving their history and payment records.
          </p>

          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <RenewMetric label="Member" value={member.fullName} tone="slate" />
            <RenewMetric label="Phone" value={member.phone ?? "-"} tone="amber" />
            <RenewMetric label="Membership" value={status} tone="emerald" />
          </div>

          <div className="mt-5 rounded-[1.6rem] border border-rose-100 bg-white/85 p-5 shadow-sm">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">What Changes</div>
                <div className="mt-2 text-lg font-black tracking-tight text-slate-950">Status becomes deactivated</div>
              </div>
              <div className="rounded-2xl border border-rose-100 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-800">
                History and payments stay intact
              </div>
            </div>
          </div>

          <div className="mt-7 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-full border-white/80 bg-white/80 px-6 font-bold"
              onClick={onCancel}
              disabled={isSubmitting}
            >
              Keep Active
            </Button>
            <Button
              type="button"
              className="h-11 rounded-full bg-rose-600 px-6 font-bold text-white hover:bg-rose-700"
              onClick={onConfirm}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Deactivating..." : "Deactivate Member"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function RenewMetric({ label, value, tone, className }: { label: string; value: string; tone: "slate" | "amber" | "emerald"; className?: string }) {
  const toneClasses = {
    slate: "border-slate-200 bg-white/80 text-slate-900",
    amber: "border-amber-200 bg-amber-50/80 text-amber-900",
    emerald: "border-emerald-200 bg-emerald-50/80 text-emerald-900",
  }[tone];

  return (
    <div className={`rounded-[1.4rem] border p-4 shadow-sm ${toneClasses} ${className ?? ""}`}>
      <div className="text-[10px] font-black uppercase tracking-[0.22em] opacity-70">{label}</div>
      <div className="mt-2 text-lg font-black">{value}</div>
    </div>
  );
}

function MemberLimitCard({
  memberLimit,
  totalMembers,
  deactivatedMembers,
}: {
  memberLimit?: {
    plan?: string;
    usedMembers?: number;
    memberLimit?: number | null;
    unlimited?: boolean;
    remainingSlots?: number;
    usagePercent?: number;
  };
  totalMembers: number;
  deactivatedMembers: number;
}) {
  const usedMembers = Number(memberLimit?.usedMembers ?? totalMembers ?? 0);
  const limit = memberLimit?.memberLimit ?? null;
  const unlimited = Boolean(memberLimit?.unlimited);
  const usagePercent = unlimited ? 0 : Number(memberLimit?.usagePercent ?? 0);
  const remainingSlots = Number(memberLimit?.remainingSlots ?? 0);
  const activePricing = !unlimited && limit !== null;
  const planLabel = memberLimit?.plan === "BILLMYGYM" || !memberLimit?.plan ? "BillMyGym" : memberLimit.plan;

  return (
    <div className="glass-panel w-full rounded-xl sm:rounded-[1.7rem] p-3 sm:p-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 sm:gap-4">
        <div>
          <div className="flex items-center gap-1 sm:gap-2 text-[8px] sm:text-[10px] font-black uppercase tracking-[0.1em] sm:tracking-[0.22em] text-emerald-700">
            <span>Members Used</span>
            <div className="group relative">
              <button
                type="button"
                className="inline-flex h-4 w-4 sm:h-5 sm:w-5 items-center justify-center rounded-full border border-emerald-200 bg-white/85 text-emerald-700 shadow-sm"
                aria-label="Member usage info"
              >
                <Info className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
              </button>
              <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 sm:mb-3 w-48 sm:w-56 -translate-x-1/2 rounded-2xl border border-emerald-100 bg-slate-950 px-3 py-2 sm:px-4 sm:py-3 text-center sm:text-left text-[10px] sm:text-[11px] font-bold normal-case tracking-normal text-white opacity-0 shadow-xl transition-opacity duration-200 group-hover:opacity-100">
                {deactivatedMembers} deactivated members stay in your directory, but only active members affect BillMyGym pricing.
              </div>
            </div>
          </div>
          <div className="mt-1 sm:mt-2 text-lg sm:text-3xl font-black tracking-tight text-slate-950">{unlimited ? `${usedMembers} Active` : `${usedMembers} / ${limit ?? 0}`}</div>
        </div>
        <Badge className="w-fit text-[8px] sm:text-xs px-1.5 py-0 sm:px-2.5 sm:py-0.5 border-slate-200 bg-white/80 text-slate-700">{planLabel}</Badge>
      </div>
      {activePricing ? (
        <>
          <div className="mt-3 sm:mt-5 h-2 sm:h-3 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-emerald-600 transition-all duration-300"
              style={{ width: `${Math.min(Math.max(usagePercent, 0), 100)}%` }}
            />
          </div>
          <div className="mt-2 sm:mt-3 flex items-center justify-between gap-2 sm:gap-3 text-[9px] sm:text-sm font-bold text-slate-600">
            <span>{usagePercent}%</span>
            <span className="truncate">{remainingSlots} left</span>
          </div>
          <div className="mt-1 sm:mt-3 hidden sm:block text-sm font-semibold text-slate-500">Total members can remain unlimited. Billing reacts only to active members.</div>
        </>
      ) : (
        <div className="mt-2 sm:mt-4 rounded-lg sm:rounded-2xl border border-emerald-100 bg-emerald-50 px-2 sm:px-4 py-1.5 sm:py-3 text-[9px] sm:text-sm font-bold text-emerald-800">
          Unlimited active-member scale
        </div>
      )}
    </div>
  );
}

function CustomReminderLimitCard({
  canUseMemberMessaging,
  used,
  limit,
}: {
  canUseMemberMessaging: boolean;
  used: number;
  limit: number | null;
}) {
    if (!canUseMemberMessaging) {
      return (
        <div className="glass-panel w-full rounded-xl sm:rounded-[1.7rem] p-3 sm:p-5">
          <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.1em] sm:tracking-[0.22em] text-slate-500">Custom WhatsApp Reminders</div>
          <div className="mt-1 sm:mt-2 text-lg sm:text-2xl font-black tracking-tight text-slate-950">Included with BillMyGym</div>
          <div className="mt-1 sm:mt-3 hidden sm:block text-sm font-semibold text-slate-500">Manual overdue reminders are included for every organization.</div>
        </div>
      );
    }
  
    const safeLimit = Number(limit ?? 50);
    const percentage = Math.min(Math.round((used / safeLimit) * 100), 100);
  
    return (
      <div className="glass-panel w-full rounded-xl sm:rounded-[1.7rem] p-3 sm:p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 sm:gap-4">
          <div>
            <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.1em] sm:tracking-[0.22em] text-emerald-700">Custom WhatsApp Reminders</div>
            <div className="mt-1 sm:mt-2 text-lg sm:text-3xl font-black tracking-tight text-slate-950">{used} / {safeLimit}</div>
          </div>
          <Badge className="w-fit text-[8px] sm:text-xs px-1.5 py-0 sm:px-2.5 sm:py-0.5 border-emerald-100 bg-emerald-50 text-emerald-700">{Math.max(safeLimit - used, 0)} left</Badge>
        </div>
        <div className="mt-3 sm:mt-5 h-2 sm:h-3 overflow-hidden rounded-full bg-slate-200">
          <div
            className="h-full rounded-full bg-[linear-gradient(90deg,#059669,#22c55e,#f59e0b)] transition-all duration-300"
            style={{ width: `${Math.max(percentage, used > 0 ? 8 : 0)}%` }}
          />
        </div>
        <div className="mt-1 sm:mt-3 hidden sm:block text-sm font-semibold text-slate-500">
          Only manual member-wise overdue reminders count toward this 50-per-month limit.
        </div>
      </div>
    );
}

function SummaryTile({ label, value, tone }: { label: string; value?: number; tone: "slate" | "emerald" | "amber" | "indigo" }) {
  const toneClasses = {
    slate: "from-slate-900/10 via-slate-400/5 to-transparent",
    emerald: "from-emerald-500/18 via-emerald-300/8 to-transparent",
    amber: "from-amber-500/18 via-amber-300/8 to-transparent",
    indigo: "from-indigo-500/18 via-sky-300/8 to-transparent",
  }[tone];

  return (
    <div className="glass-panel relative overflow-hidden rounded-[1.4rem] p-3 sm:rounded-[1.7rem] sm:p-5">
      <div className={cn("absolute inset-0 bg-gradient-to-br", toneClasses)} />
      <div className="relative z-10">
        <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.1em] sm:tracking-[0.22em] text-slate-500">{label}</div>
        <h3 className="mt-1 sm:mt-2 text-2xl sm:text-3xl font-black tracking-tight text-slate-950">{value ?? 0}</h3>
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const active = status === "ACTIVE";
  const expiring = status === "EXPIRING_SOON";
  const overdue = status === "OVERDUE";
  const expired = status === "EXPIRED";
  return (
    <Badge
      variant="outline"
      className={cn(
        "flex w-fit items-center gap-1.5 rounded-full border-transparent px-3 py-1 font-black tracking-[0.14em]",
        active && "bg-emerald-100 text-emerald-800",
        expiring && "bg-amber-100 text-amber-800",
        overdue && "bg-rose-100 text-rose-800",
        expired && "bg-slate-900 text-white",
        status === "NO_MEMBERSHIP" && "bg-slate-100 text-slate-600",
      )}
    >
      {active && <CheckCircle2 className="h-3.5 w-3.5" />}
      {expiring && <Clock3 className="h-3.5 w-3.5" />}
      {overdue && <ShieldAlert className="h-3.5 w-3.5" />}
      {expired && <ShieldAlert className="h-3.5 w-3.5" />}
      {!active && !expiring && !overdue && !expired && status === "NO_MEMBERSHIP" && <XCircle className="h-3.5 w-3.5" />}
      {status.replace("_", " ")}
    </Badge>
  );
}

function CoverageBadge({ membership }: { membership?: any }) {
  if (!membership) {
    return (
      <Badge variant="outline" className="rounded-full border-transparent bg-slate-100 px-2.5 py-1 font-black tracking-[0.14em] text-slate-700">
        N/A
      </Badge>
    );
  }

  if (membership.expired) {
    return (
      <Badge variant="outline" className="rounded-full border-transparent bg-slate-900 px-2.5 py-1 font-black tracking-[0.14em] text-white">
        Expired
      </Badge>
    );
  }

  if (membership.status === "OVERDUE") {
    return (
      <Badge variant="outline" className="rounded-full border-transparent bg-rose-100 px-2.5 py-1 font-black tracking-[0.14em] text-rose-800">
        Due
      </Badge>
    );
  }

  if (membership.expiringSoon) {
    return (
        <Badge variant="outline" className="rounded-full border-transparent bg-amber-100 px-2.5 py-1 font-black tracking-[0.14em] text-amber-800">
          Due Today
        </Badge>
    );
  }

  return (
    <Badge variant="outline" className="rounded-full border-transparent bg-emerald-100 px-2.5 py-1 font-black tracking-[0.14em] text-emerald-800">
      Covered
    </Badge>
  );
}
