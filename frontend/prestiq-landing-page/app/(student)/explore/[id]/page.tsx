"use client";

import React, { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { api, ApiError } from "@/lib/api/client";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";

interface ScholarshipDetailData {
  id: number;
  slug?: string;
  title: string;
  providerName: string;
  description?: string;
  officialWebsiteUrl?: string;
  applicationUrl?: string;
  sourceType?: string;
  awardAmountMin?: number;
  awardAmountMax?: number;
  awardAmount?: number;
  currencyCode?: string;
  applicationDeadline?: string;
  deadline?: string;
  isExpired?: boolean;
  minGpa?: number;
  gpaScale?: number;
  maxFamilyIncome?: number;
  educationLevels?: string[];
  eligibleStates?: string[];
  eligibleCountries?: string[];
  termsAndConditions?: string;
  verificationStatus?: string;
}

export default function ScholarshipDetailPage() {
  const params = useParams();
  const router = useRouter();
  const scholarshipId = params?.id as string;

  const [scholarship, setScholarship] = useState<ScholarshipDetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [applying, setApplying] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    if (!scholarshipId) return;

    async function loadData() {
      try {
        const data = await api.get<ScholarshipDetailData>(`/api/v1/scholarships/${scholarshipId}`);
        setScholarship(data);
      } catch (err: unknown) {
        if (err instanceof ApiError) {
          setMessage({ text: err.detail || "Scholarship not found", type: "error" });
        } else {
          setMessage({ text: "Failed to load scholarship details", type: "error" });
        }
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [scholarshipId]);

  const handleSave = async () => {
    if (!scholarship) return;
    setSaving(true);
    setMessage(null);
    try {
      await api.post(`/api/v1/saved-scholarships/${scholarship.id}`);
      setIsSaved(true);
      setMessage({ text: "Scholarship bookmarked to your Saved Dossier!", type: "success" });
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 409) {
        setIsSaved(true);
        setMessage({ text: "Already bookmarked in your Saved list.", type: "success" });
      } else {
        setMessage({ text: "Failed to save scholarship. Please try again.", type: "error" });
      }
    } finally {
      setSaving(false);
    }
  };

  const handleStartApplication = async () => {
    if (!scholarship) return;
    setApplying(true);
    setMessage(null);
    try {
      await api.post("/api/v1/applications", {
        scholarshipId: scholarship.id,
        scholarshipTitle: scholarship.title,
        scholarshipProvider: scholarship.providerName,
        submissionDeadline: scholarship.applicationDeadline || scholarship.deadline || null,
        notes: "Started from PrestIQ scholarship discovery",
      });
      router.push("/applications");
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 409) {
        router.push("/applications");
      } else if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Could not initiate application. Please try again.", type: "error" });
      }
    } finally {
      setApplying(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
        <p className="text-xs font-semibold text-[#716D68]">Loading scholarship criteria & details...</p>
      </div>
    );
  }

  if (!scholarship) {
    return (
      <div className="bg-white border border-[#E8E3DC] rounded-xl p-12 text-center flex flex-col items-center gap-3">
        <span className="material-symbols-outlined text-[40px] text-[#BA1A1A]">error</span>
        <h2 className="font-newsreader text-2xl font-bold text-[#292624]">Scholarship Not Found</h2>
        <p className="text-xs text-[#716D68]">This opportunity may have been unpublished or removed.</p>
        <Link
          href="/explore"
          className="mt-2 px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
        >
          Back to Explore
        </Link>
      </div>
    );
  }

  const amountDisplay = scholarship.awardAmountMax
    ? `${scholarship.currencyCode || "INR"} ${scholarship.awardAmountMax.toLocaleString()}`
    : scholarship.awardAmount
    ? `${scholarship.currencyCode || "INR"} ${scholarship.awardAmount.toLocaleString()}`
    : "Full Financial Grant";

  const deadlineDisplay = scholarship.applicationDeadline || scholarship.deadline;

  return (
    <div className="flex flex-col gap-8 max-w-5xl mx-auto">
      {/* Breadcrumb & Navigation */}
      <div className="flex items-center gap-2 text-xs text-[#716D68]">
        <Link href="/explore" className="hover:text-[#477C70] flex items-center gap-1">
          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
          <span>Back to Scholarships</span>
        </Link>
        <span>/</span>
        <span className="text-[#292624] font-medium truncate max-w-xs">{scholarship.title}</span>
      </div>

      {message && (
        <div
          className={`p-4 rounded-xl border text-xs font-medium flex items-center gap-2 ${
            message.type === "success"
              ? "bg-[#EBF2F0] border-[#477C70]/30 text-[#477C70]"
              : "bg-[#FAF2EF] border-[#BA1A1A]/30 text-[#BA1A1A]"
          }`}
        >
          <span className="material-symbols-outlined text-[18px]">
            {message.type === "success" ? "check_circle" : "error"}
          </span>
          <span>{message.text}</span>
        </div>
      )}

      {/* Main Header Card */}
      <div className="bg-white border border-[#E8E3DC] rounded-2xl p-6 sm:p-8 shadow-xs flex flex-col gap-6">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-[#716D68] uppercase tracking-wider">
                {scholarship.providerName}
              </span>
              <span className="text-[#E8E3DC]">·</span>
              <StatusBadge
                label={scholarship.verificationStatus || "VERIFIED"}
                variant="verified"
                icon="verified"
              />
            </div>
            <h1 className="font-newsreader text-2xl sm:text-3xl font-bold text-[#292624] tracking-tight">
              {scholarship.title}
            </h1>
            <span className="text-xs font-semibold text-[#8C6527] bg-[#FEF8EE] px-3 py-1 rounded-full border border-[#D4AF7C]/40 self-start mt-1">
              {scholarship.sourceType || "GOVERNMENT"} Direct Grant
            </span>
          </div>

          <div className="flex flex-col sm:items-end gap-1 shrink-0 bg-[#FAF2EF] p-4 rounded-xl border border-[#E8E3DC]">
            <span className="text-[11px] font-semibold text-[#716D68] uppercase tracking-wider">Award Value</span>
            <span className="font-newsreader text-2xl sm:text-3xl font-bold text-[#477C70]">
              {amountDisplay}
            </span>
            <span className="text-xs text-[#716D68]">
              {deadlineDisplay ? `Deadline: ${new Date(deadlineDisplay).toLocaleDateString()}` : "Rolling Deadline"}
            </span>
          </div>
        </div>

        {/* Action Bar */}
        <div className="pt-4 border-t border-[#E8E3DC] flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              onClick={handleSave}
              disabled={saving || isSaved}
              className={`px-4 py-2.5 rounded-lg border text-xs font-semibold flex items-center gap-2 transition-all ${
                isSaved
                  ? "bg-[#FAF2EF] border-[#477C70] text-[#477C70]"
                  : "bg-white border-[#E8E3DC] text-[#292624] hover:bg-[#FAF2EF]"
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">
                {isSaved ? "bookmark_added" : "bookmark"}
              </span>
              <span>{isSaved ? "Saved to Dossier" : "Save Scholarship"}</span>
            </button>

            {scholarship.officialWebsiteUrl && (
              <a
                href={scholarship.officialWebsiteUrl}
                target="_blank"
                rel="noreferrer"
                className="px-4 py-2.5 rounded-lg border border-[#E8E3DC] bg-white text-xs font-semibold text-[#716D68] hover:text-[#292624] hover:bg-[#FAF2EF] flex items-center gap-1.5 transition-colors"
              >
                <span>Official Scheme Portal</span>
                <span className="material-symbols-outlined text-[16px]">open_in_new</span>
              </a>
            )}
          </div>

          <button
            onClick={handleStartApplication}
            disabled={applying}
            className="px-6 py-2.5 rounded-lg bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-60 text-white text-xs font-bold transition-colors shadow-xs flex items-center gap-2"
          >
            {applying ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Starting Application...</span>
              </>
            ) : (
              <>
                <span>Start Application</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Program Details & Eligibility Criteria Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left: Program Description */}
        <div className="md:col-span-2 bg-white border border-[#E8E3DC] rounded-xl p-6 sm:p-7 shadow-xs flex flex-col gap-4">
          <h2 className="font-newsreader text-xl font-bold text-[#292624]">
            Program Overview & Scope
          </h2>
          <div className="text-sm text-[#716D68] leading-relaxed whitespace-pre-line">
            {scholarship.description ||
              "This scholarship program provides direct merit-cum-means financial aid to eligible scholars across accredited institutions in India. Disbursals are facilitated via Direct Benefit Transfer (DBT) directly into the scholar's verified bank account upon verification."}
          </div>

          {scholarship.termsAndConditions && (
            <div className="mt-4 pt-4 border-t border-[#E8E3DC] flex flex-col gap-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-[#292624]">
                Terms & Disbursal Conditions
              </h3>
              <p className="text-xs text-[#716D68] leading-relaxed whitespace-pre-line">
                {scholarship.termsAndConditions}
              </p>
            </div>
          )}
        </div>

        {/* Right: Statutory Eligibility Rules */}
        <div className="bg-white border border-[#E8E3DC] rounded-xl p-6 shadow-xs flex flex-col gap-4">
          <h2 className="font-newsreader text-lg font-bold text-[#292624]">
            Eligibility Benchmark
          </h2>

          <div className="flex flex-col gap-3 text-xs">
            <div className="p-3 rounded-lg bg-[#FAF2EF] border border-[#E8E3DC] flex flex-col gap-1">
              <span className="text-[#716D68] font-medium">Minimum GPA Required</span>
              <span className="font-bold text-[#292624] text-sm">
                {scholarship.minGpa ? `${scholarship.minGpa} / ${scholarship.gpaScale || 10.0}` : "No minimum cutoff"}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#FAF2EF] border border-[#E8E3DC] flex flex-col gap-1">
              <span className="text-[#716D68] font-medium">Max Annual Family Income</span>
              <span className="font-bold text-[#292624] text-sm">
                {scholarship.maxFamilyIncome
                  ? `₹${scholarship.maxFamilyIncome.toLocaleString()}`
                  : "No income ceiling"}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#FAF2EF] border border-[#E8E3DC] flex flex-col gap-1">
              <span className="text-[#716D68] font-medium">Target Education Level</span>
              <span className="font-bold text-[#292624] text-sm">
                {scholarship.educationLevels && scholarship.educationLevels.length > 0
                  ? scholarship.educationLevels.join(", ")
                  : "All Levels Eligible"}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-[#FAF2EF] border border-[#E8E3DC] flex flex-col gap-1">
              <span className="text-[#716D68] font-medium">Eligible States</span>
              <span className="font-bold text-[#292624] text-sm">
                {scholarship.eligibleStates && scholarship.eligibleStates.length > 0
                  ? scholarship.eligibleStates.join(", ")
                  : "All 28 Indian States & UTs"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

