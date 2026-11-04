"use client";

import React, { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api/client";
import { SavedScholarshipResponse, PagedResponse } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";

export default function SavedScholarshipsPage() {
  const [saved, setSaved] = useState<SavedScholarshipResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const fetchSaved = useCallback(async () => {
    try {
      const res = await api.get<PagedResponse<SavedScholarshipResponse>>("/api/v1/saved-scholarships");
      setSaved(res.content || []);
    } catch {
      setSaved([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSaved();
  }, [fetchSaved]);

  const handleRemove = async (scholarshipId: number) => {
    setRemovingId(scholarshipId);
    setFeedback(null);
    try {
      await api.delete(`/api/v1/saved-scholarships/${scholarshipId}`);
      setSaved((prev) => prev.filter((item) => item.scholarshipId !== scholarshipId));
      setFeedback("Scholarship removed from your saved list.");
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setFeedback(err.detail || "Failed to remove scholarship");
      } else {
        setFeedback("Failed to remove bookmark.");
      }
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Saved Dossier"
        badgeIcon="bookmark"
        title="Saved Scholarships"
        description="Bookmarked opportunities and grants shortlisted for upcoming application cycles."
        actions={
          <span className="text-xs font-semibold px-3 py-1.5 rounded-full bg-white border border-[#E8E3DC] text-[#716D68]">
            {saved.length} Bookmarked
          </span>
        }
      />

      {feedback && (
        <div className="p-3.5 rounded-xl bg-[#FAF2EF] border border-[#E8E3DC] text-xs font-semibold text-[#477C70] flex items-center justify-between">
          <span>{feedback}</span>
          <button onClick={() => setFeedback(null)} className="text-[#716D68] hover:text-[#292624]">✕</button>
        </div>
      )}

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xs font-semibold text-[#716D68]">Loading your bookmarked grants...</p>
        </div>
      ) : saved.length === 0 ? (
        <div className="bg-white border border-[#E8E3DC] rounded-xl p-12 text-center flex flex-col items-center gap-3">
          <span className="material-symbols-outlined text-[40px] text-[#716D68]/40">bookmark_border</span>
          <h3 className="font-newsreader text-xl font-bold text-[#292624]">Your saved dossier is empty</h3>
          <p className="text-xs text-[#716D68] max-w-sm">
            Save scholarships while exploring the catalog to compare criteria and prepare applications.
          </p>
          <Link
            href="/explore"
            className="mt-2 px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Explore Scholarships
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {saved.map((item) => (
            <div
              key={item.id}
              className="bg-white border border-[#E8E3DC] rounded-xl p-5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="flex flex-col gap-1 max-w-xl">
                <span className="text-xs text-[#716D68] font-medium">{item.providerName}</span>
                <h3 className="font-newsreader text-lg sm:text-xl font-bold text-[#292624] tracking-tight">
                  <Link href={`/explore/${item.scholarshipId}`} className="hover:text-[#477C70] transition-colors">
                    {item.scholarshipTitle}
                  </Link>
                </h3>
                {item.notes && <p className="text-xs text-[#716D68] italic mt-1">"{item.notes}"</p>}
              </div>

              <div className="flex flex-col sm:items-end gap-2 shrink-0">
                <span className="font-newsreader text-xl font-bold text-[#292624]">
                  {item.currencyCode} {item.awardAmount?.toLocaleString() ?? "Grant"}
                </span>
                <span className="text-xs text-[#B85D19] font-medium">
                  {item.deadline ? `Deadline: ${new Date(item.deadline).toLocaleDateString()}` : "Active Opportunity"}
                </span>
                <div className="flex items-center gap-2 mt-1">
                  <button
                    onClick={() => handleRemove(item.scholarshipId)}
                    disabled={removingId === item.scholarshipId}
                    className="px-3 py-1.5 rounded-lg border border-[#E8E3DC] text-xs font-semibold text-[#716D68] hover:text-[#BA1A1A] hover:bg-[#FAF2EF] transition-colors disabled:opacity-50"
                  >
                    {removingId === item.scholarshipId ? "Removing..." : "Remove"}
                  </button>
                  <Link
                    href={`/explore/${item.scholarshipId}`}
                    className="px-4 py-1.5 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D] transition-colors"
                  >
                    View & Apply
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
