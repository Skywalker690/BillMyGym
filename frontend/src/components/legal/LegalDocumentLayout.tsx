import type { ReactNode } from "react";

export interface LegalSection {
  title: string;
  content: ReactNode;
}

interface LegalDocumentLayoutProps {
  documentLabel: string;
  title: string;
  effectiveDate: string;
  introduction: ReactNode;
  sections: LegalSection[];
  contacts: Array<{ label: string; value: string }>;
}

export default function LegalDocumentLayout({
  documentLabel,
  title,
  effectiveDate,
  introduction,
  sections,
  contacts,
}: LegalDocumentLayoutProps) {
  return (
    <div className="min-h-screen bg-slate-50 px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl">
        <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <header className="border-b border-slate-200 px-6 py-6 sm:px-8">
            <div className="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-700">{documentLabel}</div>
            <h1 className="mt-3 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">{title}</h1>
            <div className="mt-3 text-sm font-medium text-slate-500">Effective Date: {effectiveDate}</div>
          </header>

          <div className="px-6 py-6 sm:px-8">
            <div className="prose prose-slate max-w-none text-sm leading-7 text-slate-700">{introduction}</div>
          </div>

          <div className="border-t border-slate-200">
            {sections.map((section) => (
              <section key={section.title} className="border-b border-slate-200 px-6 py-6 last:border-b-0 sm:px-8">
                <h2 className="text-xl font-bold tracking-tight text-slate-950">{section.title}</h2>
                <div className="prose prose-slate mt-4 max-w-none text-sm leading-7 text-slate-700">{section.content}</div>
              </section>
            ))}
          </div>

          <footer className="border-t border-slate-200 px-6 py-6 sm:px-8">
            <h2 className="text-lg font-bold text-slate-950">Contact Information</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {contacts.map((contact) => (
                <div key={contact.label} className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                  <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{contact.label}</div>
                  <div className="mt-2 text-sm font-medium text-slate-800">{contact.value}</div>
                </div>
              ))}
            </div>
          </footer>
        </div>
      </div>
    </div>
  );
}
