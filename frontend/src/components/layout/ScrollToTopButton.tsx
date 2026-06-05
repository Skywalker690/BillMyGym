import { useEffect, useState } from "react";
import { ArrowUp } from "lucide-react";
import { cn } from "@/lib/utils";

export function ScrollToTopButton({ visible = true }: { visible?: boolean }) {
  const [showButton, setShowButton] = useState(false);

  useEffect(() => {
    if (!visible) {
      setShowButton(false);
      return;
    }

    const handleScroll = () => setShowButton(window.scrollY > 320);
    handleScroll();
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, [visible]);

  return (
    <button
      type="button"
      aria-label="Scroll to top"
      onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
      className={cn(
        "fixed bottom-24 right-4 z-30 inline-flex h-12 w-12 items-center justify-center rounded-full border border-white/80 bg-slate-950 text-white shadow-[0_24px_45px_-24px_rgba(15,23,42,0.75)] transition-all duration-300 hover:bg-slate-800 sm:bottom-6 sm:right-6",
        showButton ? "translate-y-0 opacity-100" : "pointer-events-none translate-y-4 opacity-0",
      )}
    >
      <ArrowUp className="h-5 w-5" />
    </button>
  );
}
