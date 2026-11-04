import React from "react";
import Link from "next/link";

export default function AccessDeniedPage() {
  return (
    <div className="min-h-screen bg-[#F8F6F2] flex items-center justify-center p-4 sm:p-6 font-sans">
      <div className="w-full max-w-lg bg-white rounded-2xl border border-[#E8E3DC] p-8 sm:p-10 shadow-[0_4px_24px_rgba(41,38,36,0.06)] text-center flex flex-col items-center">
        {/* Shield icon in deep sage container */}
        <div className="w-16 h-16 rounded-2xl bg-[#FAF2EF] border border-[#E8E3DC] text-[#477C70] flex items-center justify-center mb-6 shadow-xs">
          <span className="material-symbols-outlined text-[32px]">lock</span>
        </div>

        {/* Status code & title */}
        <span className="text-xs uppercase tracking-widest text-[#716D68] font-bold">
          HTTP Error 403
        </span>
        <h1 className="font-newsreader text-3xl sm:text-4xl font-bold text-[#292624] tracking-tight mt-1 mb-3">
          Access Denied
        </h1>

        {/* Description */}
        <p className="text-sm text-[#716D68] leading-relaxed mb-8 max-w-sm">
          You do not have the required statutory authorization or role credentials to access this administrative registry node.
        </p>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row items-center gap-3 w-full">
          <Link
            href="/dashboard"
            className="w-full sm:flex-1 py-2.5 px-5 rounded-lg bg-[#477C70] text-white text-sm font-semibold hover:bg-[#3B675D] transition-colors shadow-xs"
          >
            Return to Dashboard
          </Link>
          <Link
            href="/"
            className="w-full sm:flex-1 py-2.5 px-5 rounded-lg bg-white border border-[#E8E3DC] text-[#292624] text-sm font-semibold hover:bg-[#FAF2EF] transition-colors"
          >
            Visit Landing Page
          </Link>
        </div>

        {/* Security watermark */}
        <div className="mt-8 pt-6 border-t border-[#E8E3DC] w-full flex items-center justify-center gap-2 text-xs text-[#716D68]">
          <span className="w-1.5 h-1.5 rounded-full bg-[#477C70]"></span>
          <span>PrestIQ Security Gateway · Zero-Trust RBAC Enforcement</span>
        </div>
      </div>
    </div>
  );
}
