import React from "react";
import Link from "next/link";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#F8F6F2] text-[#292624] flex flex-col justify-between p-4 sm:p-6 lg:p-8 font-sans">
      {/* Top Brand Bar */}
      <div className="w-full max-w-6xl mx-auto flex items-center justify-between py-2">
        <Link href="/" className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-lg bg-[#477C70] text-white flex items-center justify-center shadow-xs">
            <span className="material-symbols-outlined text-[20px]">school</span>
          </div>
          <span className="font-newsreader text-2xl font-semibold tracking-tight text-[#292624]">
            PrestIQ
          </span>
        </Link>
        <Link
          href="/"
          className="text-xs font-medium text-[#716D68] hover:text-[#292624] transition-colors flex items-center gap-1"
        >
          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
          <span>Back to Home</span>
        </Link>
      </div>

      {/* Main Centered Content */}
      <main className="w-full max-w-6xl mx-auto flex-1 flex items-center justify-center py-8">
        {children}
      </main>

      {/* Footer */}
      <footer className="w-full max-w-6xl mx-auto py-4 border-t border-[#E8E3DC] flex flex-col sm:flex-row items-center justify-between text-xs text-[#716D68] gap-2">
        <span>© 2026 PrestIQ. Verified Merit & Need Scholarships for India.</span>
        <div className="flex gap-4">
          <Link href="/#about" className="hover:text-[#292624]">About</Link>
          <Link href="/403" className="hover:text-[#292624]">Security</Link>
          <Link href="/#" className="hover:text-[#292624]">Terms & Privacy</Link>
        </div>
      </footer>
    </div>
  );
}
