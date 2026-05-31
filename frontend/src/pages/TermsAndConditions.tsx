import type { ReactNode } from "react";
import LegalDocumentLayout, { type LegalSection } from "@/components/legal/LegalDocumentLayout";
import { useAuth } from "@/context/AuthContext";

export default function TermsAndConditions() {
  const { legalPolicy } = useAuth();
  const title = legalPolicy?.applicationName ?? "BillMyGym";
  const effectiveDate = legalPolicy?.effectiveDate ?? "2026-06-01";
  const contacts = [
    { label: "Support Email", value: legalPolicy?.supportEmail ?? "support@billmygym.com" },
    { label: "Legal Email", value: legalPolicy?.legalEmail ?? "legal@billmygym.com" },
    { label: "Company Address", value: legalPolicy?.companyAddress ?? "BillMyGym, Kakkanad, Ernakulam, Kerala, India" },
  ];

  const sections: LegalSection[] = [
    section("1. Acceptance of Terms", [
      "By accessing or using BillMyGym, you agree to these Terms & Conditions.",
      "You must comply with these terms, all applicable laws, and all platform rules while using the service.",
      "If you do not agree, you must not access or continue using the platform.",
    ]),
    section("2. Eligibility", [
      "You must be at least 18 years old or have lawful authority to create and manage an account on behalf of your organization.",
      "You confirm that the registration details you provide are accurate, complete, and kept current.",
      "You may use the platform only if you are legally permitted to enter into this agreement.",
    ]),
    section("3. Account Responsibilities", [
      "Each account is issued to a specific user or organization administrator.",
      "You are responsible for protecting your credentials, maintaining account security, and controlling access within your team.",
      "You must notify BillMyGym promptly if you suspect unauthorized access or credential compromise.",
    ]),
    section("4. Acceptable Use", [
      "You must not hack, probe, reverse engineer, scrape, overload, or abuse the platform.",
      "You must not upload malware, automate misuse, harass users, impersonate another person, or use the service for illegal conduct.",
      "You must not attempt to bypass product restrictions, security controls, billing logic, or audit protections.",
    ]),
    section("5. User Content", [
      "You retain ownership of the information, member records, messages, and operational content you submit to BillMyGym.",
      "You grant BillMyGym permission to process that content as needed to operate the service, provide features, deliver communications, and maintain security.",
      "You remain responsible for the lawfulness, accuracy, and permissions associated with the content you upload.",
    ]),
    section("6. Intellectual Property", [
      "The BillMyGym platform, code, design, branding, reports, workflows, and trademarks are protected intellectual property.",
      "Except for limited rights needed to use the service, no ownership rights are transferred to you.",
      "You may not copy, redistribute, modify, or create derivative works from protected platform assets unless expressly permitted.",
    ]),
    section("7. Data Usage", [
      "BillMyGym processes organization, member, billing, usage, and operational data to deliver the service and keep it functioning reliably.",
      "Processing may include authentication, audit logging, reminders, analytics, revenue tracking, reporting, support, and compliance operations.",
      "Our Privacy Policy explains these data practices in more detail.",
    ]),
    section("8. Security", [
      "BillMyGym uses reasonable technical and organizational safeguards to protect the platform and customer data.",
      "You are responsible for securing your devices, credentials, and internal access permissions.",
      "You agree to report suspected incidents, breaches, or misuse without unreasonable delay.",
    ]),
    section("9. Platform Availability", [
      "The service may be unavailable from time to time because of maintenance, updates, outages, infrastructure failure, or third-party dependency issues.",
      "BillMyGym may modify, improve, or temporarily suspend parts of the platform to preserve security or reliability.",
      "We do not guarantee uninterrupted or error-free availability.",
    ]),
    section("10. Suspension and Termination", [
      "BillMyGym may suspend or terminate access if we detect policy violations, fraud, security threats, abuse, non-payment, or legal requirements to do so.",
      "We may also restrict access to protect other users, our infrastructure, or our legal obligations.",
      "Termination does not erase obligations that arose before termination, including payment and compliance duties.",
    ]),
    section("11. Limitation of Liability", [
      "The service is provided on an as-is and as-available basis to the fullest extent permitted by law.",
      "BillMyGym is not liable for indirect, incidental, special, consequential, or punitive damages arising from service use or service interruptions.",
      "Where liability cannot be excluded, it is limited to the amount paid for the service in the period permitted by applicable law.",
    ]),
    section("12. Changes to Terms", [
      "BillMyGym may update these Terms & Conditions from time to time.",
      "When changes are material, we may require you to review and accept the latest version before continuing to use the platform.",
      "Continued access after an effective update requires acceptance of the revised terms.",
    ]),
    section("13. Contact Information", [
      `Support Email: ${contacts[0].value}`,
      `Legal Email: ${contacts[1].value}`,
      `Company Address: ${contacts[2].value}`,
    ]),
  ];

  return (
    <LegalDocumentLayout
      documentLabel="Terms & Conditions"
      title={title}
      effectiveDate={effectiveDate}
      introduction={
        <>
          <p>
            BillMyGym is a gym operations platform that helps organizations manage members, memberships, billing,
            reminders, reports, and internal administration. These Terms & Conditions describe the legal relationship
            between you and BillMyGym, the rules for acceptable use, and the responsibilities that apply when you use
            the service.
          </p>
          <p>
            By using BillMyGym, you agree to act lawfully, protect your account, and use the platform only for
            legitimate operational purposes connected to your organization.
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
    content: <BulletList items={items} />,
  };
}

function BulletList({ items }: { items: string[] }) {
  return (
    <ul className="space-y-3 pl-5">
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}
