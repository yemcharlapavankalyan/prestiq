"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api/client";
import { ScholarshipSummary, PagedResponse } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";

export default function AdminScholarshipsPage() {
  const [scholarships, setScholarships] = useState<ScholarshipSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [actionId, setActionId] = useState<number | null>(null);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);

  // Form states for adding new scholarship
  const [newTitle, setNewTitle] = useState("");
  const [newProvider, setNewProvider] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [newWebsite, setNewWebsite] = useState("https://");
  const [newSourceType, setNewSourceType] = useState("GOVERNMENT");
  const [newAmount, setNewAmount] = useState("100000");
  const [newDeadline, setNewDeadline] = useState("2026-12-31");
  const [submitting, setSubmitting] = useState(false);

  const fetchScholarships = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (activeFilter !== "ALL") {
        params.append("verificationStatus", activeFilter);
      }
      params.append("size", "50");

      const res = await api.get<PagedResponse<ScholarshipSummary>>(
        `/api/v1/admin/scholarships?${params.toString()}`
      );
      setScholarships(res.content || []);
    } catch {
      setScholarships([]);
    } finally {
      setLoading(false);
    }
  }, [activeFilter]);

  useEffect(() => {
    fetchScholarships();
  }, [fetchScholarships]);

  const handleVerify = async (id: number) => {
    setActionId(id);
    setMessage(null);
    try {
      await api.put(`/api/v1/admin/scholarships/${id}/verify`);
      // Also publish to make it immediately visible to students
      await api.put(`/api/v1/admin/scholarships/${id}/publish`);
      setMessage({ text: `Scholarship #${id} verified and published!`, type: "success" });
      fetchScholarships();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to verify scholarship.", type: "error" });
      }
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (id: number) => {
    setActionId(id);
    setMessage(null);
    try {
      await api.put(`/api/v1/admin/scholarships/${id}/reject`, {
        rejectionReason: "Incomplete documentation or eligibility criteria mismatch.",
      });
      setMessage({ text: `Scholarship #${id} marked as Rejected.`, type: "success" });
      fetchScholarships();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to reject scholarship.", type: "error" });
      }
    } finally {
      setActionId(null);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setMessage(null);
    try {
      const slug = newTitle
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/(^-|-$)+/g, "")
        .slice(0, 100);

      const payload = {
        title: newTitle.trim(),
        slug: slug || `scholarship-${Date.now()}`,
        providerName: newProvider.trim(),
        description: newDescription.trim(),
        officialWebsiteUrl: newWebsite.trim(),
        applicationUrl: newWebsite.trim(),
        sourceType: newSourceType,
        awardAmountMin: parseFloat(newAmount) || 50000,
        awardAmountMax: parseFloat(newAmount) || 50000,
        currencyCode: "INR",
        applicationDeadline: newDeadline,
      };

      await api.post("/api/v1/admin/scholarships", payload);
      setMessage({ text: "Scholarship created successfully as draft!", type: "success" });
      setShowAddModal(false);
      // Reset form
      setNewTitle("");
      setNewProvider("");
      setNewDescription("");
      fetchScholarships();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to create scholarship. Check fields.", type: "error" });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Scholarship Registry"
        badgeIcon="shield"
        title="Scholarship Management"
        description="Verify incoming scraped grants, publish validated schemes, or create bespoke institutional endowments."
        actions={
          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white text-xs font-semibold flex items-center gap-2 shadow-xs transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">add</span>
            <span>Add Scholarship</span>
          </button>
        }
      />

      {message && (
        <div
          className={`p-3.5 rounded-xl border text-xs font-medium flex items-center justify-between ${
            message.type === "success"
              ? "bg-[#EBF2F0] border-[#477C70]/30 text-[#477C70]"
              : "bg-[#FAF2EF] border-[#BA1A1A]/30 text-[#BA1A1A]"
          }`}
        >
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="text-[#716D68] hover:text-[#292624]">✕</button>
        </div>
      )}

      {/* Verification Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-[#E8E3DC] pb-3">
        {[
          { id: "ALL", label: "All Opportunities" },
          { id: "PENDING_REVIEW", label: "Pending Review" },
          { id: "VERIFIED", label: "Verified & Published" },
          { id: "REJECTED", label: "Rejected" },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveFilter(tab.id)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
              activeFilter === tab.id
                ? "bg-[#477C70] text-white"
                : "text-[#716D68] hover:text-[#292624] hover:bg-[#FAF2EF]"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Table Section */}
      <div className="bg-white border border-[#E8E3DC] rounded-xl shadow-xs overflow-hidden flex flex-col">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-3">
            <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
            <p className="text-xs font-semibold text-[#716D68]">Loading scholarship inventory...</p>
          </div>
        ) : scholarships.length === 0 ? (
          <div className="p-12 text-center flex flex-col items-center gap-2">
            <span className="material-symbols-outlined text-[36px] text-[#716D68]/40">inbox</span>
            <h3 className="font-semibold text-sm text-[#292624]">No scholarships found</h3>
            <p className="text-xs text-[#716D68]">No records match the active filter criteria.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-[#FAF2EF] border-b border-[#E8E3DC] text-[#716D68] uppercase tracking-wider font-semibold">
                  <th className="py-3.5 px-4">ID</th>
                  <th className="py-3.5 px-4">Program Title</th>
                  <th className="py-3.5 px-4">Provider Agency</th>
                  <th className="py-3.5 px-4">Source Type</th>
                  <th className="py-3.5 px-4">Grant Amount</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4 text-right">Moderation Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E8E3DC]">
                {scholarships.map((s) => (
                  <tr key={s.id} className="hover:bg-[#FAF2EF]/50 transition-colors">
                    <td className="py-3.5 px-4 font-mono text-[#716D68]">#{s.id}</td>
                    <td className="py-3.5 px-4 font-semibold text-[#292624] max-w-sm">
                      <div className="truncate">{s.title}</div>
                    </td>
                    <td className="py-3.5 px-4 text-[#716D68]">{s.providerName}</td>
                    <td className="py-3.5 px-4 text-[#716D68]">{s.sourceType}</td>
                    <td className="py-3.5 px-4 font-bold text-[#292624]">
                      {s.currencyCode} {s.awardAmount?.toLocaleString() ?? "N/A"}
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusBadge
                        label={s.verificationStatus}
                        variant={
                          s.verificationStatus === "VERIFIED"
                            ? "verified"
                            : s.verificationStatus === "PENDING_REVIEW"
                            ? "pending"
                            : "urgent"
                        }
                      />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {s.verificationStatus !== "VERIFIED" && (
                          <button
                            onClick={() => handleVerify(s.id)}
                            disabled={actionId === s.id}
                            className="px-3 py-1 rounded bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-50 text-white font-semibold transition-colors shadow-xs"
                          >
                            {actionId === s.id ? "..." : "Approve & Publish"}
                          </button>
                        )}
                        {s.verificationStatus !== "REJECTED" && (
                          <button
                            onClick={() => handleReject(s.id)}
                            disabled={actionId === s.id}
                            className="px-3 py-1 rounded border border-[#E8E3DC] hover:text-[#BA1A1A] hover:bg-[#FAF2EF] disabled:opacity-50 transition-colors"
                          >
                            Reject
                          </button>
                        )}
                        <Link
                          href={`/explore/${s.id}`}
                          className="px-2.5 py-1 text-[#716D68] hover:text-[#292624]"
                          title="Preview Detail Page"
                        >
                          <span className="material-symbols-outlined text-[16px]">visibility</span>
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Scholarship Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-[#E8E3DC] max-w-xl w-full p-6 shadow-xl flex flex-col gap-5 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#E8E3DC] pb-3">
              <h3 className="font-newsreader text-xl font-bold text-[#292624]">
                Create Scholarship Opportunity
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-[#716D68] hover:text-[#292624] text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="flex flex-col gap-4 text-xs">
              <div>
                <label className="block font-semibold text-[#292624] mb-1">Scholarship Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g., Tata Trusts STEM Merit Fellowship"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full h-10 px-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-[#292624] mb-1">Provider Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g., Tata Trusts"
                    value={newProvider}
                    onChange={(e) => setNewProvider(e.target.value)}
                    className="w-full h-10 px-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-[#292624] mb-1">Source Type</label>
                  <select
                    value={newSourceType}
                    onChange={(e) => setNewSourceType(e.target.value)}
                    className="w-full h-10 px-2 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                  >
                    <option value="GOVERNMENT">Government (DBT)</option>
                    <option value="TRUST">Trust Endowment</option>
                    <option value="CORPORATE">Corporate CSR</option>
                    <option value="UNIVERSITY">University Grant</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-[#292624] mb-1">Official Website URL</label>
                <input
                  type="url"
                  required
                  placeholder="https://..."
                  value={newWebsite}
                  onChange={(e) => setNewWebsite(e.target.value)}
                  className="w-full h-10 px-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-[#292624] mb-1">Award Value (₹)</label>
                  <input
                    type="number"
                    required
                    value={newAmount}
                    onChange={(e) => setNewAmount(e.target.value)}
                    className="w-full h-10 px-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-[#292624] mb-1">Deadline Date</label>
                  <input
                    type="date"
                    required
                    value={newDeadline}
                    onChange={(e) => setNewDeadline(e.target.value)}
                    className="w-full h-10 px-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-[#292624] mb-1">Program Description</label>
                <textarea
                  required
                  rows={3}
                  placeholder="Detail the award scope, required documents, and eligibility..."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  className="w-full p-3 rounded-lg border border-[#E8E3DC] focus:outline-none focus:border-[#477C70]"
                />
              </div>

              <div className="pt-3 border-t border-[#E8E3DC] flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-lg border border-[#E8E3DC] text-[#716D68] hover:bg-[#FAF2EF]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2 rounded-lg bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-50 text-white font-semibold"
                >
                  {submitting ? "Saving..." : "Create Scholarship Draft"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

