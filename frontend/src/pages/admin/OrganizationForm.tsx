import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, ArrowRight, Check, ChevronDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

const KERALA_DISTRICTS = [
  "Thiruvananthapuram",
  "Kollam",
  "Pathanamthitta",
  "Alappuzha",
  "Kottayam",
  "Idukki",
  "Ernakulam",
  "Thrissur",
  "Palakkad",
  "Malappuram",
  "Kozhikode",
  "Wayanad",
  "Kannur",
  "Kasaragod",
] as const;

const schema = z.object({
  name: z.string().min(1, "Organization name is required"),
  place: z.string().min(1, "Place is required"),
  district: z.string().min(1, "District is required"),
  regularPlanPrice: z.coerce.number().min(0, "Regular plan price must be zero or more"),
  admissionFee: z.coerce.number().min(0, "Admission fee must be zero or more"),
  subscriptionPlan: z.literal("BILLMYGYM"),
  adminName: z.string().min(1, "Admin name is required"),
  adminEmail: z.string().email("Enter a valid admin email"),
  adminPhone: z.string().min(1, "Admin phone is required"),
  password: z.string().min(8, "Temporary password must be at least 8 characters"),
});

type FormInput = z.input<typeof schema>;
type Values = z.output<typeof schema>;
type Step = 0 | 1;

const stepMeta = [
  { id: 0 as Step, title: "Organization Details", caption: "Name, place, district, and fees." },
  { id: 1 as Step, title: "Admin Details", caption: "Primary admin access and temporary password." },
];

function generateTemporaryPassword() {
  const groups = ["ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%"];
  const allCharacters = groups.join("");
  const bytes = new Uint32Array(14);
  crypto.getRandomValues(bytes);

  const password = [
    ...groups.map((group, index) => group[bytes[index] % group.length]),
    ...Array.from(bytes.slice(groups.length), (byte) => allCharacters[byte % allCharacters.length]),
  ];

  for (let index = password.length - 1; index > 0; index--) {
    const swapIndex = bytes[index] % (index + 1);
    [password[index], password[swapIndex]] = [password[swapIndex], password[index]];
  }

  return password.join("");
}

export default function OrganizationForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>(0);
  const [reviewOpen, setReviewOpen] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    trigger,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, any, Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      subscriptionPlan: "BILLMYGYM",
      password: generateTemporaryPassword(),
      regularPlanPrice: 0,
      admissionFee: 0,
      district: "",
      place: "",
    },
  });

  const watchedName = watch("name");
  const watchedPlace = watch("place");
  const watchedDistrict = watch("district");
  const watchedRegularPlanPrice = watch("regularPlanPrice");
  const watchedAdmissionFee = watch("admissionFee");
  const watchedAdminName = watch("adminName");
  const watchedAdminEmail = watch("adminEmail");
  const watchedAdminPhone = watch("adminPhone");
  const watchedPassword = watch("password");

  const mutation = useMutation({
    mutationFn: (data: Values) =>
      api.post("/auth/register", {
        organizationName: data.name,
        place: data.place,
        district: data.district,
        adminFullName: data.adminName,
        adminPhoneNumber: data.adminPhone,
        adminEmail: data.adminEmail,
        regularPlanPrice: data.regularPlanPrice,
        admissionFee: data.admissionFee,
        subscriptionPlan: "BILLMYGYM",
        password: data.password,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizationsDirectory"] });
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      queryClient.invalidateQueries({ queryKey: ["platformAnalytics"] });
      queryClient.invalidateQueries({ queryKey: ["platformYearlyReport"] });
      toast.success("Organization provisioned successfully");
      navigate("/app/organizations");
    },
    onError: () => toast.error("Failed to provision organization"),
  });

  const onSubmit = (values: Values) => mutation.mutate(values);

  const nextStep = async () => {
    const fieldsByStep: Record<Step, Array<keyof Values>> = {
      0: ["name", "place", "district", "regularPlanPrice", "admissionFee"],
      1: ["adminName", "adminEmail", "adminPhone", "password"],
    };

    const valid = await trigger(fieldsByStep[step]);
    if (!valid) {
      toast.error("Please complete the required fields before moving ahead.");
      return;
    }
    if (step < 1) setStep((current) => (current + 1) as Step);
  };

  const previousStep = () => {
    if (step > 0) setStep((current) => (current - 1) as Step);
  };

  const openReview = async () => {
    const valid = await trigger();
    if (!valid) {
      toast.error("Please complete the required fields before reviewing.");
      return;
    }
    setReviewOpen(true);
  };

  const clearZeroOnFocus = (field: "regularPlanPrice" | "admissionFee") => {
    const currentValue = watch(field);
    if (Number(currentValue ?? 0) === 0) {
      setValue(field, "" as unknown as number, { shouldDirty: true });
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 sm:p-6 lg:p-8">
      <Button variant="ghost" onClick={() => navigate("/app/organizations")} className="px-0 text-slate-600 hover:text-slate-950">
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Organizations
      </Button>

      <div className="grid gap-3 rounded-[1.7rem] border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-2">
        {stepMeta.map((item, index) => {
          const active = step === item.id;
          const complete = step > item.id;
          return (
            <div
              key={item.id}
              className={`rounded-[1.3rem] border px-4 py-3 transition ${
                active
                  ? "border-slate-950 bg-slate-950 text-white"
                  : complete
                    ? "border-emerald-200 bg-emerald-50 text-emerald-950"
                    : "border-slate-200 bg-slate-50 text-slate-600"
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-black ${
                    active ? "bg-white text-slate-950" : complete ? "bg-emerald-500 text-white" : "bg-white text-slate-700"
                  }`}
                >
                  {complete ? <Check className="h-4 w-4" /> : index + 1}
                </div>
                <div>
                  <div className="text-sm font-black">{item.title}</div>
                  <div className={`text-xs font-semibold ${active ? "text-slate-300" : "text-slate-500"}`}>{item.caption}</div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <form className="space-y-6">
        <div className="glass-panel overflow-visible rounded-[2rem]">
          <div className="border-b border-black/5 p-6">
            <div className="text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">
              Step {step + 1} of {stepMeta.length}
            </div>
            <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">{stepMeta[step].title}</h2>
            <p className="mt-1 text-sm font-semibold text-slate-500">{stepMeta[step].caption}</p>
          </div>

          <div className="p-6">
            {step === 0 && (
              <div className="grid gap-5 md:grid-cols-2">
                <Field label="Organization Name" error={errors.name?.message}>
                  <Input {...register("name")} className="h-12 rounded-2xl bg-white/75" placeholder="Gym Name" />
                </Field>

                <Field label="Place" error={errors.place?.message}>
                  <Input {...register("place")} className="h-12 rounded-2xl bg-white/75" placeholder="Kakkanad" />
                </Field>

                <Field label="District" error={errors.district?.message}>
                  <DistrictCombobox
                    value={watchedDistrict}
                    onChange={(value) => {
                      setValue("district", value, { shouldValidate: true, shouldDirty: true });
                    }}
                  />
                </Field>

                <Field label="Regular Plan Price (INR)" error={errors.regularPlanPrice?.message}>
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    {...register("regularPlanPrice", { valueAsNumber: true })}
                    onFocus={() => clearZeroOnFocus("regularPlanPrice")}
                    className="h-12 rounded-2xl bg-white/75"
                  />
                </Field>

                <Field label="Admission Fee (INR)" error={errors.admissionFee?.message}>
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    {...register("admissionFee", { valueAsNumber: true })}
                    onFocus={() => clearZeroOnFocus("admissionFee")}
                    className="h-12 rounded-2xl bg-white/75"
                  />
                </Field>
              </div>
            )}

            {step === 1 && (
              <div className="grid gap-5 md:grid-cols-2">
                <Field label="Admin Name" error={errors.adminName?.message}>
                  <Input {...register("adminName")} className="h-12 rounded-2xl bg-white/75" placeholder="Anand Joseph" />
                </Field>

                <Field label="Admin Email" error={errors.adminEmail?.message}>
                  <Input type="email" {...register("adminEmail")} className="h-12 rounded-2xl bg-white/75" placeholder="admin@gymmax.com" />
                </Field>

                <Field label="Phone Number" error={errors.adminPhone?.message}>
                  <Input {...register("adminPhone")} className="h-12 rounded-2xl bg-white/75" placeholder="+91 98XXXXXXX" />
                </Field>

                <Field label="Temporary Password" error={errors.password?.message}>
                  <Input type="text" {...register("password")} className="h-12 rounded-2xl bg-white/75 font-semibold tracking-[0.08em]" />
                </Field>

                <div className="md:col-span-2 rounded-[1.5rem] border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-slate-700">
                  A strong temporary password has been generated. You can change it before creating the organization.
                </div>
              </div>
            )}

          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-black/5 bg-slate-50/80 px-6 py-5">
            <Button
              type="button"
              variant="outline"
              onClick={previousStep}
              disabled={step === 0}
              className="rounded-full border-white/80 bg-white px-5 font-black"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Previous
            </Button>

            {step < 1 ? (
              <Button type="button" onClick={nextStep} className="rounded-full bg-slate-950 px-6 font-black text-white hover:bg-slate-800">
                Next
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            ) : (
              <Button type="button" onClick={openReview} disabled={isSubmitting || mutation.isPending} className="rounded-full bg-emerald-600 px-7 font-black text-white hover:bg-emerald-700">
                Review & Submit
              </Button>
            )}
          </div>
        </div>
      </form>

      {reviewOpen && (
        <div className="fixed inset-0 z-40 flex items-end justify-center bg-slate-950/35 p-4 sm:items-center">
          <div className="w-full max-w-4xl rounded-[2rem] border border-white/80 bg-white shadow-[0_30px_90px_rgba(15,23,42,0.22)]">
            <div className="border-b border-slate-100 px-6 py-5">
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">Final Review</div>
              <h3 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Check the organization details before creating it.</h3>
            </div>
            <div className="grid gap-4 px-6 py-5 md:grid-cols-2">
              <ReviewColumn
                title="Organization"
                rows={[
                  { label: "Name", value: watchedName },
                  { label: "Place", value: watchedPlace },
                  { label: "District", value: watchedDistrict },
                  { label: "Regular Plan", value: `INR ${Number(watchedRegularPlanPrice ?? 0).toLocaleString("en-IN")}` },
                  { label: "Admission Fee", value: `INR ${Number(watchedAdmissionFee ?? 0).toLocaleString("en-IN")}` },
                ]}
              />
              <ReviewColumn
                title="Admin"
                rows={[
                  { label: "Name", value: watchedAdminName },
                  { label: "Email", value: watchedAdminEmail },
                  { label: "Phone", value: watchedAdminPhone },
                  { label: "Temp Password", value: watchedPassword },
                ]}
              />
            </div>
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 bg-slate-50/80 px-6 py-5">
              <div className="text-sm font-semibold text-slate-500">This review window appears only right before submission.</div>
              <div className="flex gap-3">
                <Button type="button" variant="outline" onClick={() => setReviewOpen(false)} className="rounded-full border-white/80 bg-white px-5 font-black">
                  Back
                </Button>
                <Button
                  type="button"
                  onClick={() => handleSubmit(onSubmit)()}
                  disabled={isSubmitting || mutation.isPending}
                  className="rounded-full bg-emerald-600 px-7 font-black text-white hover:bg-emerald-700"
                >
                  {isSubmitting || mutation.isPending ? "Provisioning..." : "Create Organization"}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function DistrictCombobox({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!wrapperRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filteredDistricts = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return KERALA_DISTRICTS;
    return KERALA_DISTRICTS.filter((district) => district.toLowerCase().includes(normalized));
  }, [query]);

  return (
    <div ref={wrapperRef} className="relative z-30">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        className="flex h-12 w-full items-center justify-between rounded-2xl border border-black/5 bg-white/75 px-4 text-left text-sm font-semibold text-slate-900 shadow-sm outline-none transition hover:border-emerald-200 focus:ring-2 focus:ring-emerald-500/40"
      >
        <span className={value ? "text-slate-900" : "text-slate-400"}>{value || "Choose a Kerala district"}</span>
        <ChevronDown className={`h-4 w-4 text-slate-400 transition ${open ? "rotate-180" : ""}`} />
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-2 w-full overflow-hidden rounded-[1.4rem] border border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.14)]">
          <div className="border-b border-slate-100 p-3">
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search district..."
              className="h-11 rounded-xl bg-slate-50"
              autoFocus
            />
          </div>
          <div className="max-h-72 overflow-y-auto p-2">
            {filteredDistricts.length === 0 ? (
              <div className="rounded-xl px-3 py-6 text-center text-sm font-semibold text-slate-500">
                No district matches your search.
              </div>
            ) : (
              filteredDistricts.map((district) => (
                <button
                  key={district}
                  type="button"
                  onClick={() => {
                    onChange(district);
                    setQuery("");
                    setOpen(false);
                  }}
                  className="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-800"
                >
                  <span>{district}</span>
                  {value === district && <Check className="h-4 w-4 text-emerald-600" />}
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <Label className="font-semibold text-slate-700">{label}</Label>
      {children}
      {error && <p className="text-xs font-semibold text-rose-600">{error}</p>}
    </div>
  );
}

function ReviewColumn({
  title,
  rows,
}: {
  title: string;
  rows: Array<{ label: string; value: string }>;
}) {
  return (
    <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50/70 p-4">
      <div className="text-lg font-black text-slate-950">{title}</div>
      <div className="mt-5 space-y-3">
        {rows.map((row) => (
          <div key={row.label} className="rounded-[1.1rem] border border-white/80 bg-white p-4 shadow-sm">
            <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{row.label}</div>
            <div className="mt-1 break-words text-sm font-black text-slate-950">{row.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
