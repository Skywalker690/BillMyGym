import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { BarChart3, Building2, CalendarRange, IndianRupee } from "lucide-react";
import api from "@/lib/api";

type PlatformYearlyReportMonth = {
  month: number;
  label: string;
  organizationsJoined: number;
  revenue: number;
};

type PlatformYearlyReport = {
  year: number;
  availableYears: number[];
  totalOrganizationsJoined: number;
  totalRevenue: number;
  months: PlatformYearlyReportMonth[];
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export default function PlatformReports() {
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const { data, isLoading } = useQuery<PlatformYearlyReport>({
    queryKey: ["platformYearlyReport", selectedYear],
    queryFn: async () => {
      const res = await api.get("/analytics/platform/reports", {
        params: { year: selectedYear },
      });
      return res.data;
    },
    staleTime: 60_000,
  });

  const report = data ?? {
    year: selectedYear,
    availableYears: [selectedYear],
    totalOrganizationsJoined: 0,
    totalRevenue: 0,
    months: [],
  };

  const maxJoins = Math.max(...report.months.map((item) => item.organizationsJoined), 1);
  const maxRevenue = Math.max(...report.months.map((item) => item.revenue), 1);

  if (isLoading) {
    return <div className="p-8 text-sm font-semibold text-slate-500">Loading platform reports...</div>;
  }

  return (
    <div className="mx-auto w-full max-w-7xl space-y-4 p-3 sm:space-y-6 sm:p-6 lg:p-8">
      <section className="rounded-[1.5rem] border border-slate-200 bg-white p-4 shadow-sm sm:rounded-[2.2rem] sm:p-8">
        <div className="flex flex-col gap-4 sm:gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[9px] font-black uppercase tracking-[0.16em] text-emerald-700 sm:px-4 sm:py-2 sm:text-[11px] sm:tracking-[0.24em]">
              Platform Reports
            </div>
            <h1 className="mt-3 text-2xl font-black tracking-tight text-slate-950 sm:mt-4 sm:text-4xl">Year-wise organization growth and revenue</h1>
            <p className="mt-2 max-w-3xl text-xs font-semibold leading-5 text-slate-500 sm:mt-3 sm:text-sm sm:leading-6">
              Pick a year and review how many organizations joined in each month, along with the monthly platform revenue collected.
            </p>
          </div>
          <div className="w-full max-w-xs">
            <label className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Report Year</label>
            <select
              value={selectedYear}
              onChange={(event) => setSelectedYear(Number(event.target.value))}
              className="mt-2 h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm font-black text-slate-900 shadow-sm outline-none focus:ring-2 focus:ring-emerald-500/40"
            >
              {report.availableYears.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      <section className="grid grid-cols-3 gap-2 sm:gap-4 md:grid-cols-3">
        <SummaryTile icon={CalendarRange} label="Selected Year" value={String(report.year)} detail="Monthly platform report" />
        <SummaryTile icon={Building2} label="Organizations Joined" value={report.totalOrganizationsJoined.toLocaleString("en-IN")} detail="Total new gyms in this year" />
        <SummaryTile icon={IndianRupee} label="Revenue Collected" value={money.format(report.totalRevenue)} detail="Paid platform revenue in this year" />
      </section>

      <section className="overflow-hidden rounded-[1.5rem] border border-slate-200 bg-white shadow-sm sm:rounded-[2rem]">
        <div className="border-b border-slate-100 px-4 py-3 sm:px-5 sm:py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
              <BarChart3 className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-lg font-black tracking-tight text-slate-950 sm:text-2xl">Monthly platform summary</h2>
              <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">Organizations joined and platform revenue for each month of {report.year}.</p>
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-slate-50">
              <tr className="text-xs uppercase tracking-wide text-slate-500">
                <th className="px-5 py-3">Month</th>
                <th className="px-5 py-3">Organizations Joined</th>
                <th className="px-5 py-3">Revenue</th>
                <th className="px-5 py-3">Trend</th>
              </tr>
            </thead>
            <tbody>
              {report.months.map((item) => (
                <tr key={item.month} className="border-t border-slate-100 align-top">
                  <td className="px-5 py-4 text-sm font-black text-slate-950">{item.label}</td>
                  <td className="px-5 py-4 text-sm font-semibold text-slate-700">{item.organizationsJoined.toLocaleString("en-IN")}</td>
                  <td className="px-5 py-4 text-sm font-semibold text-slate-700">{money.format(item.revenue)}</td>
                  <td className="px-5 py-4">
                    <div className="space-y-3">
                      <div>
                        <div className="mb-1 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Joins</div>
                        <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-teal-500"
                            style={{ width: `${(item.organizationsJoined / maxJoins) * 100}%` }}
                          />
                        </div>
                      </div>
                      <div>
                        <div className="mb-1 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Revenue</div>
                        <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-sky-500"
                            style={{ width: `${(item.revenue / maxRevenue) * 100}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function SummaryTile({
  icon: Icon,
  label,
  value,
  detail,
}: {
  icon: typeof CalendarRange;
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <div className="rounded-[1.1rem] border border-slate-200 bg-white p-3 shadow-sm sm:rounded-[1.6rem] sm:p-5">
      <Icon className="h-5 w-5 text-emerald-700 sm:h-6 sm:w-6" />
      <div className="mt-3 text-[8px] font-black uppercase tracking-[0.1em] text-slate-500 sm:mt-4 sm:text-[10px] sm:tracking-[0.22em]">{label}</div>
      <div className="mt-1 text-xl font-black tracking-tight text-slate-950 sm:mt-2 sm:text-3xl">{value}</div>
      <div className="mt-1 line-clamp-2 text-[10px] font-semibold text-slate-500 sm:text-sm">{detail}</div>
    </div>
  );
}
