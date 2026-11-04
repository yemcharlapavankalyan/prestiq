"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";
import { StudentSidebar } from "@/components/layout/student-sidebar";
import { StudentHeader } from "@/components/layout/student-header";
import { MobileNavDrawer } from "@/components/layout/mobile-drawer";

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F8F6F2] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs font-semibold text-[#716D68]">Loading Scholar Portal...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-[#F8F6F2] text-[#292624] flex flex-col font-sans">
      {/* Desktop Sidebar */}
      <StudentSidebar
        collapsed={collapsed}
        onToggleCollapse={() => setCollapsed((prev) => !prev)}
      />

      {/* Mobile Nav Drawer */}
      <MobileNavDrawer
        isOpen={mobileDrawerOpen}
        onClose={() => setMobileDrawerOpen(false)}
      />

      {/* Top Application Header */}
      <StudentHeader
        collapsed={collapsed}
        onOpenMobileDrawer={() => setMobileDrawerOpen(true)}
      />

      {/* Main Content Area */}
      <div
        className={`flex-1 transition-all duration-300 pt-16 ${
          collapsed ? "lg:pl-[76px]" : "lg:pl-[260px]"
        }`}
      >
        <main className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
          {children}
        </main>
      </div>
    </div>
  );
}
