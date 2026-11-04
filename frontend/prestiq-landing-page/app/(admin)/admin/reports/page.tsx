"use client";

import React from "react";
import Link from "next/link";
import { PageHeader } from "@/components/shared/page-header";
import { MetricCard } from "@/components/shared/metric-card";

export default function AdminReportsPage() {
  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Statutory Analytics"
        badgeIcon="monitoring"
        title="Reports & Analytics"
        description="Comprehensive disbursal analytics, state allocation audits, and demographic equity metrics."
        actions={
          <Link
            href="/admin/scholarships"
            className="px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Manage Scholarships
          </Link>
        }
      />

      <section className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <MetricCard
          label="Total Disbursals"
          value="₹38.5 Cr"
          subValue="FY 2024-25"
          indicatorText="Direct Benefit Transfer"
          icon="account_balance"
          variant="success"
        />
        <MetricCard
          label="Verification Rate"
          value="99.4%"
          subValue="Zero Fraud"
          indicatorText="Deterministic Eligibility Rule"
          icon="fact_check"
          variant="default"
        />
        <MetricCard
          label="Average Processing"
          value="4.2 Days"
          subValue="SLA Met"
          indicatorText="Industry Benchmark: 45 Days"
          icon="speed"
          variant="default"
        />
      </section>

      <div className="bg-white border border-[#E8E3DC] rounded-xl p-8 text-center flex flex-col items-center gap-3 shadow-xs">
        <span className="material-symbols-outlined text-[36px] text-[#716D68]/40">bar_chart</span>
        <h3 className="font-newsreader text-xl font-bold text-[#292624]">
          Real-time Audit Ledger Active
        </h3>
        <p className="text-xs text-[#716D68] max-w-md">
          Statutory reports are generated from verified PostgreSQL records and auditable across central schemes.
        </p>
      </div>
    </div>
  );
}

