"use client";

import React from "react";
import Link from "next/link";

interface StudentHeaderProps {
  collapsed: boolean;
  onOpenMobileDrawer: () => void;
}

export function StudentHeader({ collapsed, onOpenMobileDrawer }: StudentHeaderProps) {
  return (
    <header
      className={`fixed top-0 right-0 h-16 bg-[#F8F6F2]/90 backdrop-blur-md border-b border-[#E8E3DC] z-30 flex items-center justify-between px-4 sm:px-6 transition-all duration-300 left-0 ${
        collapsed ? "lg:left-[76px]" : "lg:left-[260px]"
      }`}
    >
      {/* Left: Mobile menu toggle + Global search */}
      <div className="flex items-center gap-3 w-full max-w-md">
        <button
          onClick={onOpenMobileDrawer}
          aria-label="Open navigation menu"
          className="lg:hidden p-2 rounded-lg text-[#292624] hover:bg-black/5 transition-colors"
        >
          <span className="material-symbols-outlined text-[24px]">menu</span>
        </button>

        <div className="relative w-full">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#716D68] text-[18px]">
            search
          </span>
          <input
            type="search"
            placeholder="Search scholarships, providers, keywords..."
            className="w-full h-10 pl-9 pr-14 rounded-lg bg-white border border-[#E8E3DC] text-sm text-[#292624] placeholder:text-[#716D68] focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all shadow-[0_1px_3px_rgba(0,0,0,0.02)]"
          />
          <div className="absolute right-2.5 top-1/2 -translate-y-1/2 hidden sm:flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-[#F4ECE9] text-[#716D68] text-[11px] font-mono border border-[#E8E3DC]">
            <span>⌘</span>
            <span>K</span>
          </div>
        </div>
      </div>

      {/* Right: Academic Pill, Notifications, User Avatar */}
      <div className="flex items-center gap-3 shrink-0">
        <div className="hidden md:flex items-center gap-2 px-3 py-1 rounded-full bg-white border border-[#E8E3DC] text-xs font-medium text-[#716D68] shadow-xs">
          <span className="w-2 h-2 rounded-full bg-[#477C70]"></span>
          <span>AY 2024–25 · IIT Bombay</span>
        </div>

        <Link
          href="/notifications"
          aria-label="Notifications"
          className="relative p-2 rounded-lg text-[#716D68] hover:text-[#292624] hover:bg-black/5 transition-colors"
        >
          <span className="material-symbols-outlined text-[22px]">notifications</span>
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-[#D4AF7C] ring-2 ring-white"></span>
        </Link>

        <Link href="/profile" className="flex items-center gap-2 pl-1 group">
          <div className="w-8 h-8 rounded-full bg-[#477C70] text-white flex items-center justify-center font-semibold text-xs ring-2 ring-[#477C70]/20 group-hover:ring-[#477C70]/40 transition-all">
            AS
          </div>
        </Link>
      </div>
    </header>
  );
}
