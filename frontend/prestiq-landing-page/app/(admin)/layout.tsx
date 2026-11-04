"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";
import { AdminSidebar } from "@/components/layout/admin-sidebar";
import { AdminHeader } from "@/components/layout/admin-header";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!isAuthenticated) {
        router.replace("/login");
      } else if (!isAdmin) {
        router.replace("/403");
      }
    }
  }, [isLoading, isAuthenticated, isAdmin, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F8F6F2] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs font-semibold text-[#716D68]">Verifying Admin Access...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !isAdmin) {
    return null;
  }

  return (
    <div className="min-h-screen bg-[#F8F6F2] text-[#292624] flex flex-col font-sans">
      <AdminSidebar />
      <AdminHeader />
      <div className="flex-1 pt-16 lg:pl-72 transition-all duration-300">
        <main className="max-w-[1520px] mx-auto px-6 lg:px-8 py-8 w-full">
          {children}
        </main>
      </div>
    </div>
  );
}
