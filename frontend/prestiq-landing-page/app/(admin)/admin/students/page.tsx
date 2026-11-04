"use client";

import React from "react";
import Link from "next/link";
import { PageHeader } from "@/components/shared/page-header";
import { MetricCard } from "@/components/shared/metric-card";

export default function AdminStudentsPage() {
  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Scholar Registry"
        badgeIcon="group"
        title="Student Management"
        description="Monitor verified student profiles, institutional accreditation statuses, and state DBT enrollments."
        actions={
          <Link
            href="/admin/scholarships"
            className="px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Review Scholarship Pipeline
          </Link>
        }
      />

      <section className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <MetricCard
          label="Registered Scholars"
          value="142,850"
          subValue="Active Profiles"
          indicatorText="Across 28 States & UTs"
          icon="group"
          variant="success"
        />
        <MetricCard
          label="Aadhaar DBT Verified"
          value="94.2%"
          subValue="Certified"
          indicatorText="PFMS Gateway Linked"
          icon="verified"
          variant="default"
        />
        <MetricCard
          label="Pending Verification"
          value="1,420"
          subValue="Institutions"
          indicatorText="Nodal Officer Review"
          icon="pending_actions"
          variant="urgent"
        />
      </section>

      <div className="bg-white border border-[#E8E3DC] rounded-xl p-8 text-center flex flex-col items-center gap-3 shadow-xs">
        <span className="material-symbols-outlined text-[36px] text-[#716D68]/40">badge</span>
        <h3 className="font-newsreader text-xl font-bold text-[#292624]">
          Student Verification Roster Synchronized
        </h3>
        <p className="text-xs text-[#716D68] max-w-md">
          Student profiles are updated live via the Student Service microservice on port 8082 and synchronized across the Central Registry.
        </p>
      </div>
    </div>
  );
}

