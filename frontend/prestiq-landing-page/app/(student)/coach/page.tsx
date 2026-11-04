"use client";

import React from "react";
import Link from "next/link";
import { PageHeader } from "@/components/shared/page-header";

export default function AiCoachPage() {
  return (
    <div className="flex flex-col gap-8 max-w-4xl mx-auto">
      <PageHeader
        badgeText="AI Advisory Service"
        badgeIcon="auto_awesome"
        title="AI Scholarship Coach"
        description="Personalized scholarship statement review, essay framing, and deterministic interview prep."
      />

      <div className="bg-white border border-[#E8E3DC] rounded-2xl p-8 text-center flex flex-col items-center gap-4 shadow-xs">
        <div className="w-14 h-14 rounded-2xl bg-[#FEF8EE] border border-[#D4AF7C]/40 text-[#8C6527] flex items-center justify-center">
          <span className="material-symbols-outlined text-[32px]">psychology</span>
        </div>
        <h2 className="font-newsreader text-2xl font-bold text-[#292624]">
          AI Coach Architecture Mode (Deferred for Core MVP)
        </h2>
        <p className="text-sm text-[#716D68] max-w-lg leading-relaxed">
          The AI Scholarship Coach microservice is deferred in accordance with the MVP Implementation Directive to prioritize deterministic eligibility matching and application lifecycle tracking.
        </p>
        <div className="flex items-center gap-3 mt-2">
          <Link
            href="/dashboard"
            className="px-5 py-2.5 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Return to Scholar Dashboard
          </Link>
          <Link
            href="/explore"
            className="px-5 py-2.5 rounded-lg border border-[#E8E3DC] text-xs font-semibold text-[#292624] hover:bg-[#FAF2EF]"
          >
            Discover Scholarships
          </Link>
        </div>
      </div>
    </div>
  );
}

