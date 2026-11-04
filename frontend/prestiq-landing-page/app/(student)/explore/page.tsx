"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { api } from "@/lib/api/client";
import { ScholarshipSummary, PagedResponse } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";

export default function ExploreScholarshipsPage() {
  const [scholarships, setScholarships] = useState<ScholarshipSummary[]>([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedLevel, setSelectedLevel] = useState("ALL");
  const [selectedSource, setSelectedSource] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [totalCount, setTotalCount] = useState(0);

  const fetchScholarships = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (searchTerm.trim()) {
        params.append("query", searchTerm.trim());
      }
      if (selectedLevel !== "ALL") {
        params.append("educationLevel", selectedLevel);
      }
      if (selectedSource !== "ALL") {
        params.append("sourceType", selectedSource);
      }
      params.append("size", "20");
      params.append("includeExpired", "true");

      const res = await api.get<PagedResponse<ScholarshipSummary>>(
        `/api/v1/scholarships?${params.toString()}`
      );
      if (res && res.content) {
        setScholarships(res.content);
        setTotalCount(res.totalElements || res.content.length);
      } else {
        setScholarships([]);
        setTotalCount(0);
      }
    } catch {
      setScholarships([]);
      setTotalCount(0);
    } finally {
      setLoading(false);
    }
  }, [searchTerm, selectedLevel, selectedSource]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchScholarships();
    }, 250);
    return () => clearTimeout(timer);
  }, [fetchScholarships]);

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Scholarship Discovery"
        badgeIcon="explore"
        title="Explore Scholarships"
        description="Search, filter, and verify all published government, institutional, and trust-funded fellowship opportunities in India."
        actions={
          <span className="text-xs font-semibold px-3 py-1.5 rounded-full bg-white border border-[#E8E3DC] text-[#716D68]">
            {totalCount} Opportunities Found
          </span>
        }
      />

      {/* Filter and Search Bar */}
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4 p-4 rounded-xl bg-white border border-[#E8E3DC] shadow-xs">
        <div className="relative flex-1">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#716D68] text-[18px]">
            search
          </span>
          <input
            type="search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by program title, provider, or keyword..."
            className="w-full h-10 pl-9 pr-4 rounded-lg border border-[#E8E3DC] bg-[#FAF2EF] text-sm text-[#292624] focus:outline-none focus:bg-white focus:border-[#477C70]"
          />
        </div>

        {/* Education Level Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
          <span className="text-[11px] font-bold text-[#716D68] uppercase tracking-wider mr-1">Level:</span>
          {[
            { id: "ALL", label: "All" },
            { id: "UNDERGRADUATE", label: "UG" },
            { id: "POSTGRADUATE", label: "PG" },
            { id: "HIGH_SCHOOL", label: "Class 10-12" },
            { id: "DOCTORAL", label: "PhD" },
          ].map((f) => (
            <button
              key={f.id}
              onClick={() => setSelectedLevel(f.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedLevel === f.id
                  ? "bg-[#477C70] text-white"
                  : "bg-[#F4ECE9] text-[#716D68] hover:text-[#292624]"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Source Type Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
          <span className="text-[11px] font-bold text-[#716D68] uppercase tracking-wider mr-1">Type:</span>
          {[
            { id: "ALL", label: "All" },
            { id: "GOVERNMENT", label: "Govt (DBT)" },
            { id: "TRUST", label: "Trusts" },
            { id: "CORPORATE", label: "Corporate" },
          ].map((s) => (
            <button
              key={s.id}
              onClick={() => setSelectedSource(s.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                selectedSource === s.id
                  ? "bg-[#477C70] text-white"
                  : "bg-[#F4ECE9] text-[#716D68] hover:text-[#292624]"
              }`}
            >
              {s.label}
            </button>
          ))}
        </div>
      </div>

      {/* Loading & Empty States */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xs font-semibold text-[#716D68]">Searching scholarship database...</p>
        </div>
      ) : scholarships.length === 0 ? (
        <div className="bg-white border border-[#E8E3DC] rounded-xl p-12 text-center flex flex-col items-center gap-3">
          <span className="material-symbols-outlined text-[40px] text-[#716D68]/40">search_off</span>
          <h3 className="font-newsreader text-xl font-bold text-[#292624]">No matching scholarships found</h3>
          <p className="text-xs text-[#716D68] max-w-md">
            Try adjusting your search keywords or clearing active filters to see all available opportunities.
          </p>
          <button
            onClick={() => {
              setSearchTerm("");
              setSelectedLevel("ALL");
              setSelectedSource("ALL");
            }}
            className="mt-2 px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Reset All Filters
          </button>
        </div>
      ) : (
        /* Scholarships Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {scholarships.map((s) => (
            <div
              key={s.id}
              className="bg-white border border-[#E8E3DC] rounded-xl p-6 shadow-xs hover:shadow-[0_4px_16px_rgba(41,38,36,0.06)] transition-all flex flex-col justify-between gap-4"
            >
              <div className="flex flex-col gap-2">
                <div className="flex items-start justify-between gap-3">
                  <span className="text-xs text-[#716D68] font-medium">{s.providerName}</span>
                  <StatusBadge
                    label={s.verificationStatus === "VERIFIED" ? "Verified" : s.verificationStatus}
                    variant={s.verificationStatus === "VERIFIED" ? "verified" : "neutral"}
                    icon="verified"
                  />
                </div>
                <h3 className="font-newsreader text-xl font-bold text-[#292624] tracking-tight hover:text-[#477C70] transition-colors leading-snug">
                  <Link href={`/explore/${s.id}`}>{s.title}</Link>
                </h3>
                <div className="flex items-baseline gap-2 mt-1">
                  <span className="font-newsreader text-2xl font-bold text-[#477C70]">
                    {s.currencyCode} {s.awardAmount ? s.awardAmount.toLocaleString() : "Full Funding"}
                  </span>
                  <span className="text-xs text-[#716D68]">
                    · {s.deadline ? `Closes ${new Date(s.deadline).toLocaleDateString()}` : "Rolling Deadline"}
                  </span>
                </div>
              </div>

              <div className="pt-3 border-t border-[#E8E3DC] flex items-center justify-between">
                <span className="text-xs font-semibold text-[#8C6527] bg-[#FEF8EE] px-2.5 py-0.5 rounded-full border border-[#D4AF7C]/40">
                  {s.sourceType || "GOVERNMENT"}
                </span>
                <Link
                  href={`/explore/${s.id}`}
                  className="px-4 py-1.5 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D] transition-colors"
                >
                  View Details & Apply
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
