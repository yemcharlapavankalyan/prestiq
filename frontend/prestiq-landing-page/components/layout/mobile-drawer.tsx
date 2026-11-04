"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

interface MobileDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: "dashboard" },
  { label: "Explore Scholarships", href: "/explore", icon: "manage_search", badge: "38" },
  { label: "Saved Scholarships", href: "/saved", icon: "bookmark", badge: "12" },
  { label: "My Applications", href: "/applications", icon: "assignment", badge: "4" },
  { label: "AI Scholarship Coach", href: "/coach", icon: "auto_awesome", badge: "Pro" },
  { label: "Profile & Transcripts", href: "/profile", icon: "badge" },
  { label: "Notifications", href: "/notifications", icon: "notifications" },
];

export function MobileNavDrawer({ isOpen, onClose }: MobileDrawerProps) {
  const pathname = usePathname();

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (isOpen) {
      window.addEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "hidden";
    }
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-[#292624]/40 backdrop-blur-xs transition-opacity duration-300"
      />

      {/* Drawer surface */}
      <div className="fixed inset-y-0 left-0 w-[84%] max-w-[320px] bg-white shadow-2xl flex flex-col justify-between z-10">
        <div className="flex flex-col">
          <div className="h-16 px-5 flex items-center justify-between border-b border-[#E8E3DC]">
            <Link href="/dashboard" onClick={onClose} className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-[#477C70] text-white flex items-center justify-center">
                <span className="material-symbols-outlined text-[18px]">school</span>
              </div>
              <span className="font-newsreader text-lg font-semibold text-[#292624]">
                PrestIQ
              </span>
            </Link>
            <button
              onClick={onClose}
              aria-label="Close navigation"
              className="p-1.5 rounded-lg text-[#716D68] hover:bg-[#F8F6F2] transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div className="p-3">
            <span className="px-3 text-[10px] uppercase tracking-wider text-[#716D68] font-bold">
              Academic Portal
            </span>
            <nav className="mt-2 flex flex-col gap-1">
              {navItems.map((item) => {
                const isActive = pathname === item.href;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={onClose}
                    className={`flex items-center justify-between px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-[#477C70] text-white"
                        : "text-[#716D68] hover:text-[#292624] hover:bg-[#FAF2EF]"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          isActive ? "bg-white/20 text-white" : "bg-[#F4ECE9] text-[#716D68]"
                        }`}
                      >
                        {item.badge}
                      </span>
                    )}
                  </Link>
                );
              })}
            </nav>
          </div>
        </div>

        <div className="p-4 border-t border-[#E8E3DC] bg-[#FAF2EF]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-[#477C70] text-white flex items-center justify-center font-bold text-xs shrink-0">
              AS
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-sm font-semibold text-[#292624] truncate">Aarav Sharma</span>
              <span className="text-xs text-[#716D68] truncate">IIT Bombay ’26</span>
            </div>
            <Link
              href="/"
              onClick={onClose}
              className="p-1.5 text-[#716D68] hover:text-[#292624]"
              title="Sign out"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
