import LegalDocumentLayout, { type LegalSection } from "@/components/legal/LegalDocumentLayout";
import { useAuth } from "@/context/AuthContext";

export default function PrivacyPolicy() {
  const { legalPolicy } = useAuth();
  const title = legalPolicy?.applicationName ?? "BillMyGym";
  const effectiveDate = legalPolicy?.effectiveDate ?? "2026-06-01";
  const contacts = [
    { label: "Privacy Email", value: legalPolicy?.privacyEmail ?? "privacy@billmygym.com" },
    { label: "Support Email", value: legalPolicy?.supportEmail ?? "support@billmygym.com" },
    { label: "Company Address", value: legalPolicy?.companyAddress ?? "BillMyGym, Kakkanad, Ernakulam, Kerala, India" },
  ];

  const sections: LegalSection[] = [
    section("1. Information We Collect", [
      "Account information such as your name, email address, phone number, role, and profile details.",
      "Technical information such as device details, browser details, operating system, IP address, and security signals.",
      "Usage information such as feature usage, activity records, logs, audit trails, reminders, and support interactions.",
    ]),
    section("2. How We Use Information", [
      "We use information to authenticate users, secure accounts, deliver features, generate reports, send communications, provide support, and improve the product.",
      "Operational use may include reminders, analytics, subscription billing, audit logging, fraud prevention, and service reliability monitoring.",
    ]),
    section("3. Legal Basis for Processing", [
      "We process data based on consent where required, contractual necessity to provide the service, legal obligations, and legitimate interests in operating and securing the platform.",
    ]),
    section("4. Data Sharing", [
      "We may share data with hosting providers, cloud providers, infrastructure vendors, analytics providers, and legal authorities when required by law.",
      "We do not sell user data.",
    ]),
    section("5. Data Security", [
      "We use safeguards such as encryption in transit, controlled access, monitoring, and operational backups.",
      "Where supported by infrastructure and operational design, we also use encryption at rest and environment-level isolation.",
      "No system is perfectly secure, but we work to reduce risk and respond to issues quickly.",
    ]),
    section("6. Data Retention", [
      "We retain active account data while accounts are in use and for a reasonable period afterward to support operations, compliance, and recovery.",
      "Deletion requests may be subject to retention duties, fraud prevention needs, contractual commitments, and backup cycles.",
      "Backup retention may continue for a limited period until routine backup expiry occurs.",
    ]),
    section("7. Cookies and Sessions", [
      "BillMyGym uses authentication tokens, session storage, security cookies, and related preferences necessary to keep users signed in and protect the application.",
      "We do not use advertising tracking cookies in the product experience described here.",
    ]),
    section("8. User Rights", [
      "Depending on applicable law, users may request access, export, correction, deletion, or restriction of certain data.",
      "Users may also withdraw consent where processing relies on consent, though some services may no longer be available afterward.",
    ]),
    section("9. Children's Privacy", [
      "BillMyGym is not intended for children. Users must meet the minimum legal age and authority requirements applicable to their account and organization.",
    ]),
    section("10. International Transfers", [
      "If our infrastructure or providers operate across regions, data may be processed or transferred internationally as needed to deliver the service.",
      "When such transfers occur, we use reasonable safeguards consistent with applicable legal requirements.",
    ]),
    section("11. Changes to Privacy Policy", [
      "We may update this Privacy Policy from time to time to reflect product, legal, or operational changes.",
      "When updates are material, we may require users to review and accept the latest version before continuing to use the platform.",
    ]),
    section("12. Contact Information", [
      `Privacy Email: ${contacts[0].value}`,
      `Support Email: ${contacts[1].value}`,
      `Company Address: ${contacts[2].value}`,
    ]),
  ];

  return (
    <LegalDocumentLayout
      documentLabel="Privacy Policy"
      title={title}
      effectiveDate={effectiveDate}
      introduction={
        <>
          <p>
            This Privacy Policy explains what information BillMyGym collects, why that information is collected, how
            it is protected, and what rights users may have in relation to their data.
          </p>
          <p>
            We collect only the information needed to operate the platform, secure accounts, deliver features, support
            billing and communications, and improve the service for legitimate business use.
          </p>
        </>
      }
      sections={sections}
      contacts={contacts}
    />
  );
}

function section(title: string, items: string[]): LegalSection {
  return {
    title,
    content: (
      <ul className="space-y-3 pl-5">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    ),
  };
}
