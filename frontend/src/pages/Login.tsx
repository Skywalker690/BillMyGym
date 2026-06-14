import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import api from "../lib/api";
import { fetchAndCacheSubscriptionStatus } from "@/lib/subscriptionStatusCache";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: { credential?: string }) => void;
          }) => void;
          renderButton: (
            element: HTMLElement,
            options: Record<string, string | number | boolean>
          ) => void;
        };
      };
    };
  }
}

const googleClientId = '387096326869-e7c724kbhg8s3bg7mc3gth36mmhf24mu.apps.googleusercontent.com';

const slideshowImages = [
  "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1599058917212-d750089bc07e?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=1200&q=80",
];

function loadGoogleScript() {
  return new Promise<boolean>((resolve) => {
    if (window.google?.accounts?.id) {
      resolve(true);
      return;
    }

    const existing = document.querySelector<HTMLScriptElement>('script[data-google-identity="true"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(true), { once: true });
      existing.addEventListener("error", () => resolve(false), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.dataset.googleIdentity = "true";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.head.appendChild(script);
  });
}

export default function Login() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [activeSlide, setActiveSlide] = useState(0);
  const googleButtonShellRef = useRef<HTMLDivElement | null>(null);
  const googleButtonRef = useRef<HTMLDivElement | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();
  const onboardingEmail = searchParams.get("email") ?? "";
  const onboardingPassword = searchParams.get("temporaryPassword") ?? "";
  const isOnboardingLogin = searchParams.get("onboarding") === "true" && Boolean(onboardingEmail || onboardingPassword);

  const completeLogin = async (payload: any, fallbackEmail: string) => {
    const { accessToken, role, organizationId, user } = payload;

    const nextUser = {
      ...user,
      role: user?.role ?? role,
      organizationId,
      name: user?.name ?? user?.email ?? fallbackEmail,
    };

    login(accessToken, nextUser);

    if (nextUser.role === "ORG_ADMIN") {
      try {
        await fetchAndCacheSubscriptionStatus();
      } catch {
        // Subscription guards still run on protected APIs; this cache is only for shell state.
      }
    }

    if (nextUser.consentRequired || !nextUser.termsAccepted || !nextUser.privacyAccepted) {
      navigate("/consent");
    } else if (nextUser.role === "SUPER_ADMIN") {
      navigate("/app/platform-overview");
    } else {
      navigate("/app/dashboard");
    }

    toast.success(`Welcome back, ${nextUser.name}`);
  };

  const executeLogin = async (loginEmail: string, loginPassword: string) => {
    setIsLoading(true);
    try {
      const response = await api.post("/auth/login", { email: loginEmail, password: loginPassword });
      await completeLogin(response.data, loginEmail);
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.message || "Unable to sign in right now.";
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleCredential = async (credential?: string) => {
    if (!credential) {
      toast.error("Google sign-in did not return a valid token.");
      return;
    }

    setIsGoogleLoading(true);
    try {
      const response = await api.post("/auth/google-login", { idToken: credential });
      await completeLogin(response.data, "Google User");
    } catch (error: any) {
      const message = error?.response?.data?.message || "Google sign-in is unavailable right now.";
      toast.error(message);
    } finally {
      setIsGoogleLoading(false);
    }
  };

  useEffect(() => {
    if (!isOnboardingLogin) {
      return;
    }
    setEmail(onboardingEmail);
    setPassword(onboardingPassword);
    setShowPassword(true);
  }, [isOnboardingLogin, onboardingEmail, onboardingPassword]);

  useEffect(() => {
    let active = true;
    let cleanupResizeObserver: (() => void) | undefined;
    let pendingFrame: number | undefined;

    const setupGoogle = async () => {
      if (!googleClientId || !googleButtonShellRef.current || !googleButtonRef.current) {
        return;
      }

      const loaded = await loadGoogleScript();
      if (!loaded || !active || !googleButtonShellRef.current || !googleButtonRef.current || !window.google?.accounts?.id) {
        return;
      }

      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: ({ credential }) => {
          void handleGoogleCredential(credential);
        },
      });

      const renderGoogleButton = () => {
        if (!active || !googleButtonShellRef.current || !googleButtonRef.current || !window.google?.accounts?.id) {
          return;
        }

        if (pendingFrame) {
          window.cancelAnimationFrame(pendingFrame);
        }

        pendingFrame = window.requestAnimationFrame(() => {
          if (!active || !googleButtonShellRef.current || !googleButtonRef.current || !window.google?.accounts?.id) {
            return;
          }

          const availableWidth = Math.floor(googleButtonShellRef.current.clientWidth);
          const buttonWidth = Math.max(200, Math.min(400, availableWidth - 2));

          googleButtonRef.current.innerHTML = "";
          window.google.accounts.id.renderButton(googleButtonRef.current, {
            theme: "outline",
            size: "large",
            shape: "rectangular",
            width: buttonWidth,
            text: "continue_with",
          });
        });
      };

      renderGoogleButton();

      if ("ResizeObserver" in window && googleButtonShellRef.current) {
        const resizeObserver = new ResizeObserver(renderGoogleButton);
        resizeObserver.observe(googleButtonShellRef.current);
        cleanupResizeObserver = () => resizeObserver.disconnect();
      } else {
        window.addEventListener("resize", renderGoogleButton);
        cleanupResizeObserver = () => window.removeEventListener("resize", renderGoogleButton);
      }
    };

    void setupGoogle();

    return () => {
      active = false;
      if (pendingFrame) {
        window.cancelAnimationFrame(pendingFrame);
      }
      cleanupResizeObserver?.();
    };
  }, []);

  useEffect(() => {
    const interval = window.setInterval(() => {
      setActiveSlide((current) => {
        let next = current;
        while (next === current) {
          next = Math.floor(Math.random() * slideshowImages.length);
        }
        return next;
      });
    }, 3800);

    return () => window.clearInterval(interval);
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await executeLogin(email, password);
  };

  return (
    <div className="min-h-screen overflow-hidden bg-[#f4f1ec] px-3 py-3 sm:px-4 sm:py-4 lg:px-6 lg:py-5">
      <div className="mx-auto flex min-h-[calc(100vh-1.5rem)] max-w-7xl items-center sm:min-h-[calc(100vh-2rem)] lg:min-h-[calc(100vh-2.5rem)]">
        <div className="grid h-full max-h-[calc(100vh-1.5rem)] w-full overflow-hidden rounded-[2rem] border border-[#ebe4da] bg-[#f7f4ef] shadow-[0_24px_80px_rgba(62,39,20,0.10)] sm:max-h-[calc(100vh-2rem)] lg:grid-cols-[0.86fr_1.14fr] lg:max-h-[calc(100vh-2.5rem)]">
          <section className="flex items-center overflow-y-auto px-7 py-7 sm:px-9 sm:py-8 lg:px-12 lg:py-8">
            <div className="w-full max-w-sm">
              <div className="mb-8 lg:mb-9">
                <h1 className="text-[3.15rem] font-semibold leading-[0.95] tracking-tight text-[#24211d] sm:text-[3.5rem] lg:text-[4rem]">
                  Login in to
                  <br />
                  your account
                </h1>
              </div>

              <form className="space-y-4" onSubmit={handleSubmit}>
                {isOnboardingLogin && (
                  <div className="border border-orange-200 bg-orange-50 px-4 py-3 text-sm font-medium leading-6 text-orange-950">
                    <div className="font-semibold text-orange-900">Temporary credentials are pre-filled for testing.</div>
                    <div className="mt-1 break-all">Email: {onboardingEmail || "-"}</div>
                    <div className="break-all">Temporary password: {onboardingPassword || "-"}</div>
                    <div className="mt-2">
                      Sign in once, then use{" "}
                      <Link to={`/forgot-password?email=${encodeURIComponent(onboardingEmail)}`} className="font-semibold underline underline-offset-2">
                        Forgot Password
                      </Link>{" "}
                      to reset this password.
                    </div>
                  </div>
                )}
                <div>
                  <label htmlFor="email" className="mb-2 block text-sm font-medium text-[#8b857d]">
                    Email Address
                  </label>
                  <Input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="billmygym@gmail.com"
                    className="h-12 w-full rounded-[1rem] border border-[#e8e0d6] bg-white/95 px-4 text-[15px] font-medium text-[#282520] shadow-[0_10px_24px_rgba(62,39,20,0.04)] placeholder:text-[#a6a09a] focus-visible:border-[#f0b58c] focus-visible:ring-2 focus-visible:ring-[#f0b58c]/25"
                  />
                </div>

                <div>
                  <label htmlFor="password" className="mb-2 block text-sm font-medium text-[#8b857d]">
                    Password
                  </label>
                  <div className="relative">
                    <Input
                      id="password"
                      name="password"
                      type={showPassword ? "text" : "password"}
                      autoComplete="current-password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="********"
                      className="h-12 w-full rounded-[1rem] border border-[#e8e0d6] bg-white/95 px-4 pr-11 text-[15px] font-medium text-[#282520] shadow-[0_10px_24px_rgba(62,39,20,0.04)] placeholder:text-[#a6a09a] focus-visible:border-[#f0b58c] focus-visible:ring-2 focus-visible:ring-[#f0b58c]/25"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((current) => !current)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9d968d] transition hover:text-[#4b4640]"
                      aria-label={showPassword ? "Hide password" : "Show password"}
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div className="pt-1">
                  <Link to="/forgot-password" className="text-sm font-medium text-[#2a261f] underline underline-offset-2">
                    Forgot Password?
                  </Link>
                </div>

                <Button
                  type="submit"
                  disabled={isLoading || isGoogleLoading}
                  className="mt-2 h-12 w-full rounded-[1rem] bg-[#f07b2f] text-base font-semibold text-white shadow-[0_16px_30px_rgba(240,123,47,0.24)] hover:bg-[#df6d23]"
                >
                  {isLoading ? "Logging in..." : "Log in"}
                </Button>
              </form>

              <div className="mt-6">
                <div className="flex items-center gap-3">
                  <div className="h-px flex-1 bg-[#e4ddd4]" />
                  <span className="text-[11px] font-semibold uppercase tracking-[0.22em] text-[#b1aaa2]">or</span>
                  <div className="h-px flex-1 bg-[#e4ddd4]" />
                </div>

                {googleClientId ? (
                  <div
                    ref={googleButtonShellRef}
                    className="mt-5 flex h-12 w-full items-center justify-center rounded-[1rem]"
                  >
                    <div
                      ref={googleButtonRef}
                      className="flex w-full justify-center [&>div>div]:!w-full [&>div]:!w-full [&_div[role=button]]:!box-border [&_div[role=button]]:!w-full [&_div[role=button]]:!max-w-full [&_div[role=button]]:!rounded-[1rem] [&_div[role=button]]:!border-0 [&_div[role=button]]:!shadow-none [&_iframe]:!max-w-full"
                    />
                  </div>
                ) : (
                  <div className="mt-5 border border-[#ece2d6] bg-white px-4 py-3 text-sm text-[#7f776e]">
                    Google login is ready here and only needs `VITE_GOOGLE_CLIENT_ID` configured.
                  </div>
                )}

                {isGoogleLoading && (
                  <p className="mt-3 text-center text-sm font-medium text-[#8b857d]">Completing Google sign-in...</p>
                )}
              </div>
            </div>
          </section>

          <section className="relative hidden bg-[#f7f4ef] p-4 sm:p-5 lg:block">
            <div className="relative h-full overflow-hidden rounded-[1.2rem]">
              {slideshowImages.map((image, index) => (
                <div
                  key={image}
                  className={`absolute inset-0 transition-opacity duration-[1400ms] ${
                    index === activeSlide ? "opacity-100" : "opacity-0"
                  }`}
                >
                  <img
                    src={image}
                    alt="Premium gym and fitness visual"
                    className="h-full w-full object-cover"
                  />
                  <div className="absolute inset-0 bg-[linear-gradient(180deg,rgba(19,17,14,0.10),rgba(19,17,14,0.24))]" />
                </div>
              ))}

              <div className="absolute bottom-5 left-5 right-5 flex items-end justify-between gap-4">
                <div className="max-w-md rounded-[1.2rem] bg-black/20 px-5 py-4 text-white backdrop-blur-md">
                  <div className="text-[11px] font-semibold uppercase tracking-[0.26em] text-white/70">BillMyGym</div>
                  <p className="mt-2 text-base font-medium leading-7 lg:text-lg">
                    Premium gym operations, member billing, reminders, and growth workflows in one polished workspace.
                  </p>
                </div>

                <div className="flex gap-2 rounded-full bg-black/18 px-3 py-2 backdrop-blur-md">
                  {slideshowImages.map((_, index) => (
                    <span
                      key={index}
                      className={`h-2.5 w-2.5 rounded-full transition ${
                        index === activeSlide ? "bg-white" : "bg-white/45"
                      }`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
