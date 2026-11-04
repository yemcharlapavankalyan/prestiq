"use client";

import React from "react";
import Link from "next/link";

interface AdminHeaderProps {
  onOpenMobileNav?: () => void;
}

export function AdminHeader({ onOpenMobileNav }: AdminHeaderProps) {
  return (
    <header className="fixed top-0 right-0 h-16 bg-[#F8F6F2]/85 backdrop-blur-xl border-b border-[#E8E3DC] z-30 px-6 sm:px-8 flex items-center justify-between left-0 lg:left-72 transition-all">
      {/* Left: Breadcrumbs & status */}
      <div className="flex items-center gap-4">
        {onOpenMobileNav && (
          <button
            onClick={onOpenMobileNav}
            className="lg:hidden p-1.5 rounded-lg text-[#292624] hover:bg-black/5"
            aria-label="Open navigation"
          >
            <span className="material-symbols-outlined text-[22px]">menu</span>
          </button>
        )}
        <div className="hidden sm:flex items-center gap-2 text-xs text-[#716D68]">
          <span className="font-medium">Central Registry</span>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="text-[#292624] font-semibold">Operations</span>
        </div>
        <span className="hidden sm:inline-block h-4 w-[1px] bg-[#E8E3DC]"></span>
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-[#477C70]"></span>
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#716D68]">
            Central Authority Node · Active
          </span>
        </div>
      </div>

      {/* Right: Search + Superadmin Badge + Notification + Avatar */}
      <div className="flex items-center gap-4">
        <div className="relative hidden md:flex items-center">
          <span className="material-symbols-outlined absolute left-3 text-[18px] text-[#716D68]">
            search
          </span>
          <input
            type="search"
            placeholder="Search scholarships, applicants..."
            className="w-72 h-9 pl-9 pr-12 rounded-lg bg-white border border-[#E8E3DC] text-xs text-[#292624] placeholder:text-[#716D68] focus:outline-none focus:border-[#477C70] shadow-xs"
          />
          <span className="absolute right-2 px-1.5 py-0.5 rounded bg-[#F4ECE9] text-[#716D68] text-[10px] font-mono border border-[#E8E3DC]">
            ⌘K
          </span>
        </div>

        <div className="px-2.5 py-1 rounded-full bg-[#FFDDB1] text-[#795C30] text-xs font-semibold flex items-center gap-1.5 border border-[#D4AF7C]/30 shadow-xs">
          <span className="material-symbols-outlined text-[14px]">verified_user</span>
          <span className="hidden sm:inline">Tier 1 — Superadmin</span>
        </div>

        <button
          aria-label="Admin Alerts"
          className="relative p-2 rounded-lg text-[#716D68] hover:bg-black/5 transition-colors"
        >
          <span className="material-symbols-outlined text-[20px]">notifications</span>
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[#BA1A1A]"></span>
        </button>

        <Link href="/admin/settings" className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-[#477C70] text-white flex items-center justify-center font-bold text-xs ring-2 ring-[#477C70]/20">
            RM
          </div>
        </Link>
      </div>
    </header>
  );
}
