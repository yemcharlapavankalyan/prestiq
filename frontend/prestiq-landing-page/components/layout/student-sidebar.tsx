"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

interface StudentSidebarProps {
  collapsed: boolean;
  onToggleCollapse: () => void;
}

const navItems = [
  { label: "Dashboard", href: "/dashboard", icon: "dashboard" },
  { label: "Explore Scholarships", href: "/explore", icon: "manage_search", badge: "38" },
  { label: "Saved Scholarships", href: "/saved", icon: "bookmark", badge: "12" },
  { label: "My Applications", href: "/applications", icon: "assignment", badge: "4" },
  { label: "AI Scholarship Coach", href: "/coach", icon: "auto_awesome", badge: "Pro", badgeColor: "gold" },
  { label: "Profile & Transcripts", href: "/profile", icon: "badge" },
  { label: "Notifications", href: "/notifications", icon: "notifications" },
];

export function StudentSidebar({ collapsed, onToggleCollapse }: StudentSidebarProps) {
  const pathname = usePathname();

  return (
    <aside
      className={`fixed left-0 top-0 h-full bg-white border-r border-[#E8E3DC] shadow-[0_1px_8px_rgba(0,0,0,0.03)] z-40 transition-all duration-300 ease-in-out hidden lg:flex flex-col justify-between ${
        collapsed ? "w-[76px]" : "w-[260px]"
      }`}
    >
      <div className="flex flex-col">
        {/* Header / Brand */}
        <div className="h-16 px-4 flex items-center justify-between border-b border-[#E8E3DC]/60">
          <Link href="/dashboard" className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-lg bg-[#477C70] text-white flex items-center justify-center shrink-0 shadow-sm">
              <span className="material-symbols-outlined text-[20px]">school</span>
            </div>
            {!collapsed && (
              <div className="flex flex-col overflow-hidden">
                <span className="font-newsreader text-[19px] font-semibold text-[#292624] tracking-tight leading-tight">
                  PrestIQ
                </span>
                <span className="text-[10px] uppercase tracking-wider text-[#716D68] font-medium">
                  Scholar Portal
                </span>
              </div>
            )}
          </Link>
          <button
            onClick={onToggleCollapse}
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            className="p-1 rounded-md text-[#716D68] hover:text-[#292624] hover:bg-[#F8F6F2] transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">
              {collapsed ? "chevron_right" : "chevron_left"}
            </span>
          </button>
        </div>

        {/* Navigation links */}
        <nav className="flex flex-col gap-1 p-2 pt-3">
          {navItems.map((item) => {
            const isActive = pathname === item.href || (item.href !== "/dashboard" && pathname?.startsWith(item.href));

            return (
              <Link
                key={item.href}
                href={item.href}
                title={collapsed ? item.label : undefined}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group relative ${
                  isActive
                    ? "bg-[#477C70] text-white shadow-sm"
                    : "text-[#716D68] hover:text-[#292624] hover:bg-[#F8F6F2]"
                }`}
              >
                <span
                  className={`material-symbols-outlined text-[20px] shrink-0 ${
                    isActive ? "text-white" : "text-[#716D68] group-hover:text-[#292624]"
                  }`}
                >
                  {item.icon}
                </span>

                {!collapsed && (
                  <div className="flex items-center justify-between w-full min-w-0">
                    <span className="truncate">{item.label}</span>
                    {item.badge && (
                      <span
                        className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                          item.badgeColor === "gold"
                            ? isActive
                              ? "bg-white/20 text-white"
                              : "bg-[#FEF8EE] text-[#8C6527] border border-[#D4AF7C]/40"
                            : isActive
                            ? "bg-white/20 text-white"
                            : "bg-[#F4ECE9] text-[#716D68]"
                        }`}
                      >
                        {item.badge}
                      </span>
                    )}
                  </div>
                )}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* User profile widget at bottom */}
      <div className="p-3 border-t border-[#E8E3DC]">
        <div className="p-2 rounded-xl bg-[#FAF2EF] flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-[#477C70] text-white flex items-center justify-center font-semibold text-xs shrink-0">
              AS
            </div>
            {!collapsed && (
              <div className="flex flex-col min-w-0">
                <span className="text-xs font-semibold text-[#292624] truncate">Aarav Sharma</span>
                <span className="text-[11px] text-[#716D68] truncate">IIT Bombay ’26</span>
              </div>
            )}
          </div>
          {!collapsed && (
            <Link
              href="/"
              title="Sign out to home"
              className="p-1 rounded-md text-[#716D68] hover:text-[#292624] hover:bg-white transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span>
            </Link>
          )}
        </div>
      </div>
    </aside>
  );
}
