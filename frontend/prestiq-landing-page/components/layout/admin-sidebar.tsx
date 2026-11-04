"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const adminNavItems = [
  { label: "Admin Dashboard", href: "/admin/dashboard", icon: "space_dashboard" },
  { label: "Scholarship Management", href: "/admin/scholarships", icon: "school" },
  { label: "Student Management", href: "/admin/students", icon: "group" },
  { label: "Reports & Analytics", href: "/admin/reports", icon: "monitoring" },
  { label: "Settings", href: "/admin/settings", icon: "tune" },
];

export function AdminSidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-full w-72 bg-[#FAF2EF] border-r border-[#E8E3DC] shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-40 flex flex-col justify-between py-6 px-5 hidden lg:flex">
      <div className="flex flex-col gap-6">
        {/* Brand header */}
        <div className="flex items-center justify-between px-2">
          <Link href="/admin/dashboard" className="flex items-center gap-2.5">
            <span className="font-newsreader text-2xl font-bold tracking-tight text-[#292624]">
              PrestIQ
            </span>
            <span className="px-2 py-0.5 rounded-full bg-[#FFDDB1] text-[#795C30] text-[10px] font-bold tracking-wider uppercase">
              Admin Console
            </span>
          </Link>
        </div>

        {/* Node status card */}
        <div className="px-3 py-2 rounded-lg bg-[#F4ECE9] border border-[#E8E3DC] flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#477C70] animate-pulse"></span>
            <span className="text-[11px] font-semibold uppercase tracking-wider text-[#716D68]">
              Central Registry Node
            </span>
          </div>
          <span className="text-[11px] font-bold text-[#477C70]">v2.4</span>
        </div>

        {/* Nav Links */}
        <nav className="flex flex-col gap-1.5">
          {adminNavItems.map((item) => {
            const isActive = pathname === item.href || pathname?.startsWith(item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? "bg-[#477C70] text-white shadow-sm font-semibold"
                    : "text-[#716D68] hover:text-[#292624] hover:bg-[#F4ECE9]"
                }`}
              >
                <span
                  className={`material-symbols-outlined text-[20px] ${
                    isActive ? "text-white" : "text-[#716D68]"
                  }`}
                >
                  {item.icon}
                </span>
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Admin profile widget */}
      <div className="flex flex-col gap-3 pt-4 border-t border-[#E8E3DC]">
        <div className="flex items-center gap-3 p-2.5 rounded-xl bg-white border border-[#E8E3DC] shadow-xs">
          <div className="w-9 h-9 rounded-full bg-[#477C70] text-white flex items-center justify-center shrink-0 font-bold text-xs">
            RM
          </div>
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-xs font-semibold text-[#292624] truncate">Dr. Radhika Mehra</span>
            <span className="text-[11px] text-[#716D68] truncate">Chief Admin Officer</span>
          </div>
          <Link
            href="/"
            title="Sign out"
            className="p-1.5 rounded-lg text-[#716D68] hover:text-[#BA1A1A] hover:bg-[#FAF2EF] transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">logout</span>
          </Link>
        </div>
      </div>
    </aside>
  );
}
