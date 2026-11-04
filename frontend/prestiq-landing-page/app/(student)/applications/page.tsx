"use client";

import React, { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { api, ApiError } from "@/lib/api/client";
import { ApplicationResponse, ApplicationStatus, PagedResponse } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";

export default function MyApplicationsPage() {
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [activeTab, setActiveTab] = useState<string>("ALL");
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  const fetchApplications = useCallback(async () => {
    try {
      const endpoint =
        activeTab === "ALL"
          ? "/api/v1/applications"
          : `/api/v1/applications?status=${activeTab}`;
      const res = await api.get<PagedResponse<ApplicationResponse>>(endpoint);
      setApplications(res.content || []);
    } catch {
      setApplications([]);
    } finally {
      setLoading(false);
    }
  }, [activeTab]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const advanceStatus = async (app: ApplicationResponse) => {
    let nextStatus: ApplicationStatus | null = null;
    let nextPercentage = app.checklistCompletionPercentage;

    if (app.currentStatus === "PLANNING") {
      nextStatus = "IN_PROGRESS";
      nextPercentage = 50;
    } else if (app.currentStatus === "IN_PROGRESS") {
      nextStatus = "READY_TO_SUBMIT";
      nextPercentage = 90;
    } else if (app.currentStatus === "READY_TO_SUBMIT") {
      nextStatus = "SUBMITTED";
      nextPercentage = 100;
    }

    if (!nextStatus) return;

    setUpdatingId(app.id);
    setMessage(null);
    try {
      // First update completion percentage
      await api.patch(`/api/v1/applications/${app.id}`, {
        checklistCompletionPercentage: nextPercentage,
      });

      // Next update status
      const updated = await api.put<ApplicationResponse>(`/api/v1/applications/${app.id}/status`, {
        targetStatus: nextStatus,
        reason: `Advanced to ${nextStatus} by student`,
      });

      setApplications((prev) =>
        prev.map((item) => (item.id === app.id ? updated : item))
      );
      setMessage({
        text: `Application transitioned to ${nextStatus.replace(/_/g, " ")}!`,
        type: "success",
      });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to update application status.", type: "error" });
      }
    } finally {
      setUpdatingId(null);
    }
  };

  const handleWithdraw = async (id: string) => {
    if (!confirm("Are you sure you want to withdraw this application?")) return;
    setUpdatingId(id);
    setMessage(null);
    try {
      const res = await api.post<ApplicationResponse>(`/api/v1/applications/${id}/withdraw`, null);
      setApplications((prev) =>
        prev.map((item) => (item.id === id ? res : item))
      );
      setMessage({ text: "Application withdrawn successfully.", type: "success" });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to withdraw application.", type: "error" });
      }
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Application Tracker"
        badgeIcon="assignment"
        title="My Applications"
        description="Track active fellowship and scholarship submissions, document completion milestones, and verified disbursal statuses."
        actions={
          <Link
            href="/explore"
            className="px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D] shadow-xs"
          >
            Apply for More Scholarships
          </Link>
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

      {/* Stage tabs */}
      <div className="flex items-center gap-2 border-b border-[#E8E3DC] pb-3 overflow-x-auto">
        {["ALL", "PLANNING", "IN_PROGRESS", "READY_TO_SUBMIT", "SUBMITTED", "WITHDRAWN"].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              activeTab === tab
                ? "bg-[#477C70] text-white"
                : "text-[#716D68] hover:text-[#292624] hover:bg-[#FAF2EF]"
            }`}
          >
            {tab.replace(/_/g, " ")}
          </button>
        ))}
      </div>

      {/* Applications List */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xs font-semibold text-[#716D68]">Loading your active applications...</p>
        </div>
      ) : applications.length === 0 ? (
        <div className="bg-white border border-[#E8E3DC] rounded-xl p-12 text-center flex flex-col items-center gap-3">
          <span className="material-symbols-outlined text-[40px] text-[#716D68]/40">assignment_turned_in</span>
          <h3 className="font-newsreader text-xl font-bold text-[#292624]">No applications in this category</h3>
          <p className="text-xs text-[#716D68] max-w-sm">
            {activeTab === "ALL"
              ? "You haven't initiated any scholarship applications yet. Discover eligible opportunities and begin."
              : `No applications currently marked as ${activeTab.replace(/_/g, " ")}.`}
          </p>
          <Link
            href="/explore"
            className="mt-2 px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
          >
            Discover Scholarships
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {applications.map((app) => (
            <div
              key={app.id}
              className="bg-white border border-[#E8E3DC] rounded-xl p-6 shadow-xs flex flex-col gap-4"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div className="flex flex-col">
                  <span className="text-xs text-[#716D68] font-mono">
                    Ref #{app.externalReferenceNumber || app.id.slice(0, 8)}
                  </span>
                  <h3 className="font-newsreader text-xl font-bold text-[#292624] tracking-tight">
                    <Link href={`/explore/${app.scholarshipId}`} className="hover:text-[#477C70]">
                      {app.scholarshipTitle}
                    </Link>
                  </h3>
                  <span className="text-xs text-[#716D68] mt-0.5">{app.providerName}</span>
                </div>
                <StatusBadge
                  label={app.currentStatus.replace(/_/g, " ")}
                  variant={
                    app.currentStatus === "SUBMITTED" || app.currentStatus === "AWARDED"
                      ? "verified"
                      : app.currentStatus === "IN_PROGRESS" || app.currentStatus === "READY_TO_SUBMIT"
                      ? "gold"
                      : app.currentStatus === "WITHDRAWN"
                      ? "urgent"
                      : "neutral"
                  }
                />
              </div>

              {/* Progress bar */}
              <div className="flex flex-col gap-1.5 pt-2">
                <div className="flex items-center justify-between text-xs text-[#716D68]">
                  <span>Checklist & Verification Progress</span>
                  <span className="font-semibold text-[#292624]">{app.checklistCompletionPercentage}%</span>
                </div>
                <div className="w-full bg-[#E8E3DC] h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-[#477C70] h-full rounded-full transition-all"
                    style={{ width: `${app.checklistCompletionPercentage}%` }}
                  ></div>
                </div>
              </div>

              {/* Actions & Timestamps */}
              <div className="pt-3 border-t border-[#E8E3DC] flex flex-wrap items-center justify-between text-xs text-[#716D68] gap-3">
                <span>
                  {app.submissionDeadline ? (
                    <>
                      Submission Deadline:{" "}
                      <strong className="text-[#292624]">
                        {new Date(app.submissionDeadline).toLocaleDateString()}
                      </strong>
                    </>
                  ) : (
                    "Rolling Application Cycle"
                  )}
                </span>

                <div className="flex items-center gap-2">
                  {app.currentStatus !== "SUBMITTED" && app.currentStatus !== "WITHDRAWN" && (
                    <button
                      onClick={() => advanceStatus(app)}
                      disabled={updatingId === app.id}
                      className="px-3.5 py-1.5 rounded-lg bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-50 text-white font-semibold transition-colors flex items-center gap-1.5"
                    >
                      {updatingId === app.id ? (
                        <span>Updating...</span>
                      ) : app.currentStatus === "PLANNING" ? (
                        <span>Start Working →</span>
                      ) : app.currentStatus === "IN_PROGRESS" ? (
                        <span>Mark Ready →</span>
                      ) : (
                        <span>Confirm Submission →</span>
                      )}
                    </button>
                  )}

                  {app.currentStatus !== "WITHDRAWN" && app.currentStatus !== "SUBMITTED" && (
                    <button
                      onClick={() => handleWithdraw(app.id)}
                      disabled={updatingId === app.id}
                      className="px-3 py-1.5 rounded-lg border border-[#E8E3DC] hover:text-[#BA1A1A] hover:bg-[#FAF2EF] transition-colors"
                    >
                      Withdraw
                    </button>
                  )}

                  <Link
                    href={`/explore/${app.scholarshipId}`}
                    className="px-3 py-1.5 rounded-lg border border-[#E8E3DC] text-[#292624] hover:bg-[#FAF2EF] transition-colors font-medium"
                  >
                    View Details
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
