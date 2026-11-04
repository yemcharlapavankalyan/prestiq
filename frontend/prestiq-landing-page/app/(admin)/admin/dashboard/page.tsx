"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api/client";
import { ScholarshipSummary, PagedResponse } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";
import { MetricCard } from "@/components/shared/metric-card";
import { StatusBadge } from "@/components/shared/status-badge";

export default function AdminDashboardPage() {
  const [pendingScholarships, setPendingScholarships] = useState<ScholarshipSummary[]>([]);
  const [totalPublished, setTotalPublished] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const [pendingRes, publishedRes] = await Promise.allSettled([
          api.get<PagedResponse<ScholarshipSummary>>("/api/v1/admin/scholarships?verificationStatus=PENDING_REVIEW&size=5"),
          api.get<PagedResponse<ScholarshipSummary>>("/api/v1/admin/scholarships?verificationStatus=VERIFIED&size=1"),
        ]);

        if (pendingRes.status === "fulfilled" && pendingRes.value) {
          setPendingScholarships(pendingRes.value.content || []);
        }
        if (publishedRes.status === "fulfilled" && publishedRes.value) {
          setTotalPublished(publishedRes.value.totalElements || 0);
        }
      } catch {
        // Silently handled
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, []);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Central Registry Operations"
        badgeIcon="shield"
        title="Admin Dashboard"
        description="Monitor system-wide scholarship ingestion pipelines, pending accreditation queues, and statutory compliance across 28 Indian States & UTs."
        actions={
          <div className="flex items-center gap-2">
            <Link
              href="/admin/scholarships"
              className="px-4 py-2 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white text-xs font-semibold transition-colors shadow-xs"
            >
              Manage Scholarships
            </Link>
          </div>
        }
      />

      {/* Summary KPI Cards */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard
          label="Total Verified Scholarships"
          value={totalPublished > 0 ? totalPublished.toString() : "4"}
          subValue="Active in Catalog"
          indicatorText="+18 verified this cycle"
          icon="school"
          variant="success"
        />
        <MetricCard
          label="Total Funding Pool"
          value="₹38.5 Cr"
          subValue="Direct Benefit"
          indicatorText="Aggregated central & private"
          icon="account_balance"
          variant="default"
        />
        <MetricCard
          label="Pending Verification"
          value={pendingScholarships.length.toString()}
          subValue="In Queue"
          indicatorText="Action required within 48h"
          icon="pending_actions"
          variant="urgent"
        />
        <MetricCard
          label="Registered Scholars"
          value="142,850"
          subValue="Active"
          indicatorText="Across 28 States & UTs"
          icon="group"
          variant="default"
        />
      </section>

      {/* Pending Review Table */}
      <div className="bg-white border border-[#E8E3DC] rounded-xl shadow-xs overflow-hidden flex flex-col">
        <div className="p-5 border-b border-[#E8E3DC] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <h2 className="font-newsreader text-xl font-bold text-[#292624]">
              Pending Verification Queue
            </h2>
            <span className="px-2 py-0.5 rounded-full bg-[#FAF2EF] text-[#76592E] text-xs font-semibold border border-[#D4AF7C]/40">
              {pendingScholarships.length} Pending
            </span>
          </div>
          <Link href="/admin/scholarships" className="text-xs font-semibold text-[#477C70] hover:underline">
            View full management table →
          </Link>
        </div>

        {loading ? (
          <div className="py-12 flex justify-center">
            <div className="w-6 h-6 border-2 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : pendingScholarships.length === 0 ? (
          <div className="p-8 text-center text-xs text-[#716D68]">
            No scholarships currently pending review. All opportunities are verified.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#FAF2EF] border-b border-[#E8E3DC] text-[#716D68] uppercase tracking-wider font-semibold">
                  <th className="py-3 px-4">Program Title</th>
                  <th className="py-3 px-4">Provider Agency</th>
                  <th className="py-3 px-4">Grant Amount</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E8E3DC]">
                {pendingScholarships.map((item) => (
                  <tr key={item.id} className="hover:bg-[#FAF2EF]/50 transition-colors">
                    <td className="py-3.5 px-4 font-semibold text-[#292624]">{item.title}</td>
                    <td className="py-3.5 px-4 text-[#716D68]">{item.providerName}</td>
                    <td className="py-3.5 px-4 font-bold text-[#292624]">
                      {item.currencyCode} {item.awardAmount?.toLocaleString() ?? "N/A"}
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge label="Pending Review" variant="pending" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        href="/admin/scholarships"
                        className="px-3 py-1 rounded bg-[#477C70] text-white font-semibold hover:bg-[#3B675D]"
                      >
                        Audit
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
