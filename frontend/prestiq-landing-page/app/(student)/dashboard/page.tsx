"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth/auth-context";
import { api } from "@/lib/api/client";
import {
  StudentProfile,
  ScholarshipSummary,
  SavedScholarshipResponse,
  ApplicationResponse,
  PagedResponse,
} from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";
import { MetricCard } from "@/components/shared/metric-card";
import { StatusBadge } from "@/components/shared/status-badge";

export default function StudentDashboardPage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [scholarships, setScholarships] = useState<ScholarshipSummary[]>([]);
  const [saved, setSaved] = useState<SavedScholarshipResponse[]>([]);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadDashboardData() {
      try {
        const [profileRes, scholarshipsRes, savedRes, appsRes] = await Promise.allSettled([
          api.get<StudentProfile>("/api/v1/students/profile"),
          api.get<PagedResponse<ScholarshipSummary>>("/api/v1/scholarships?size=4"),
          api.get<PagedResponse<SavedScholarshipResponse>>("/api/v1/saved-scholarships"),
          api.get<PagedResponse<ApplicationResponse>>("/api/v1/applications"),
        ]);

        if (profileRes.status === "fulfilled" && profileRes.value) {
          setProfile(profileRes.value);
        }
        if (scholarshipsRes.status === "fulfilled" && scholarshipsRes.value) {
          setScholarships(scholarshipsRes.value.content || []);
        }
        if (savedRes.status === "fulfilled" && savedRes.value) {
          setSaved(savedRes.value.content || []);
        }
        if (appsRes.status === "fulfilled" && appsRes.value) {
          setApplications(appsRes.value.content || []);
        }
      } catch {
        // Handled silently
      } finally {
        setLoading(false);
      }
    }

    loadDashboardData();
  }, []);

  const studentName = profile
    ? `${profile.firstName} ${profile.lastName}`
    : user
    ? `${user.firstName} ${user.lastName}`
    : "Scholar";

  const totalFundingEstimate = scholarships.reduce((acc, s) => acc + (s.awardAmount || 0), 0);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
        <span className="text-xs font-semibold text-[#716D68]">Loading your scholar dashboard...</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-8">
      {/* Header */}
      <PageHeader
        badgeText={profile?.educationLevel ? `${profile.educationLevel} Scholar` : "Scholar Portal"}
        badgeIcon="verified"
        title={`Welcome back, ${studentName}`}
        description={`${profile?.institutionName || "Higher Education"} · ${
          profile?.majorFieldOfStudy || "STEM Discipline"
        } · GPA: ${profile?.cumulativeGpa ?? "8.5"}/${profile?.gpaScale ?? "10.0"}`}
        actions={
          <div className="flex items-center gap-3">
            <Link
              href="/profile"
              className="px-4 py-2 rounded-lg bg-white border border-[#E8E3DC] text-xs font-semibold text-[#292624] hover:bg-[#FAF2EF] transition-colors"
            >
              Edit Profile
            </Link>
            <Link
              href="/explore"
              className="px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D] transition-colors shadow-xs"
            >
              Explore Matches
            </Link>
          </div>
        }
      />

      {/* Metrics Row */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard
          label="Available Opportunities"
          value={scholarships.length.toString()}
          subValue="Active Grants"
          indicatorText="Verified Indian Endowments"
          icon="school"
          variant="success"
        />
        <MetricCard
          label="Saved Scholarships"
          value={saved.length.toString()}
          subValue="Bookmarked"
          indicatorText="Ready for application"
          icon="bookmark"
          variant="default"
        />
        <MetricCard
          label="Active Applications"
          value={applications.length.toString()}
          subValue="Submissions"
          indicatorText="In review / Progress"
          icon="assignment"
          variant="urgent"
        />
        <MetricCard
          label="Eligible Funding Pool"
          value={`₹${(totalFundingEstimate / 100000).toFixed(1)}L`}
          subValue="Disbursable"
          indicatorText="Direct benefit transfer (DBT)"
          icon="account_balance"
          variant="default"
        />
      </section>

      {/* Main Grid: Active Applications & Saved Opportunities */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Active Applications Column (2 cols) */}
        <div className="lg:col-span-2 flex flex-col gap-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[#477C70] text-[22px]">assignment</span>
              <h2 className="font-newsreader text-2xl font-bold text-[#292624]">
                Application Progress
              </h2>
            </div>
            <Link href="/applications" className="text-xs font-semibold text-[#477C70] hover:underline">
              View all ({applications.length})
            </Link>
          </div>

          {applications.length === 0 ? (
            <div className="bg-white border border-[#E8E3DC] rounded-xl p-8 text-center flex flex-col items-center gap-3">
              <span className="material-symbols-outlined text-[36px] text-[#716D68]/40">assignment_turned_in</span>
              <h3 className="font-semibold text-sm text-[#292624]">No active applications yet</h3>
              <p className="text-xs text-[#716D68] max-w-sm">
                Explore verified grants tailored to your profile and begin your first application dossier.
              </p>
              <Link
                href="/explore"
                className="mt-2 px-4 py-2 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D]"
              >
                Find Scholarships to Apply
              </Link>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {applications.slice(0, 3).map((app) => (
                <div
                  key={app.id}
                  className="bg-white border border-[#E8E3DC] rounded-xl p-5 shadow-xs flex flex-col gap-3"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-col">
                      <span className="text-xs text-[#716D68] font-mono">
                        Ref #{app.externalReferenceNumber || app.id.slice(0, 8)}
                      </span>
                      <h3 className="font-newsreader text-lg font-bold text-[#292624]">
                        {app.scholarshipTitle}
                      </h3>
                      <span className="text-xs text-[#716D68]">{app.providerName}</span>
                    </div>
                    <StatusBadge
                      label={app.currentStatus.replace(/_/g, " ")}
                      variant={
                        app.currentStatus === "SUBMITTED" || app.currentStatus === "AWARDED"
                          ? "verified"
                          : app.currentStatus === "IN_PROGRESS"
                          ? "gold"
                          : "neutral"
                      }
                    />
                  </div>

                  {/* Progress */}
                  <div className="flex flex-col gap-1 pt-1">
                    <div className="flex justify-between text-xs text-[#716D68]">
                      <span>Dossier Completion</span>
                      <span className="font-semibold text-[#292624]">{app.checklistCompletionPercentage}%</span>
                    </div>
                    <div className="w-full bg-[#E8E3DC] h-2 rounded-full overflow-hidden">
                      <div
                        className="bg-[#477C70] h-full rounded-full transition-all"
                        style={{ width: `${app.checklistCompletionPercentage}%` }}
                      ></div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Available Opportunities Section */}
          <div className="flex items-center justify-between mt-4">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[#477C70] text-[22px]">auto_awesome</span>
              <h2 className="font-newsreader text-2xl font-bold text-[#292624]">
                Top Available Scholarships
              </h2>
            </div>
            <Link href="/explore" className="text-xs font-semibold text-[#477C70] hover:underline">
              View Catalog
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {scholarships.slice(0, 4).map((s) => (
              <div
                key={s.id}
                className="bg-white border border-[#E8E3DC] rounded-xl p-5 shadow-xs flex flex-col justify-between gap-4"
              >
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#716D68] font-medium">{s.providerName}</span>
                    <StatusBadge label={s.verificationStatus} variant="verified" />
                  </div>
                  <h3 className="font-newsreader text-base font-bold text-[#292624] leading-snug">
                    {s.title}
                  </h3>
                  <div className="font-newsreader text-lg font-bold text-[#477C70] mt-1">
                    {s.currencyCode} {s.awardAmount ? s.awardAmount.toLocaleString() : "Full Funding"}
                  </div>
                </div>

                <div className="pt-3 border-t border-[#E8E3DC] flex items-center justify-between">
                  <span className="text-[11px] text-[#716D68]">
                    {s.deadline ? `Closes ${new Date(s.deadline).toLocaleDateString()}` : "Rolling Deadline"}
                  </span>
                  <Link
                    href={`/explore/${s.id}`}
                    className="px-3 py-1.5 rounded-lg bg-[#477C70] text-white text-xs font-semibold hover:bg-[#3B675D] transition-colors"
                  >
                    View Details
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Saved Scholarships Sidebar (1 col) */}
        <div className="flex flex-col gap-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[#477C70] text-[22px]">bookmark</span>
              <h2 className="font-newsreader text-2xl font-bold text-[#292624]">
                Saved Grants
              </h2>
            </div>
            <Link href="/saved" className="text-xs font-semibold text-[#477C70] hover:underline">
              Manage ({saved.length})
            </Link>
          </div>

          {saved.length === 0 ? (
            <div className="bg-white border border-[#E8E3DC] rounded-xl p-6 text-center flex flex-col items-center gap-2">
              <span className="material-symbols-outlined text-[30px] text-[#716D68]/40">bookmark_border</span>
              <p className="text-xs text-[#716D68]">
                Bookmark scholarships from the catalog to review or apply later.
              </p>
              <Link
                href="/explore"
                className="mt-2 text-xs font-semibold text-[#477C70] hover:underline"
              >
                Browse Scholarships →
              </Link>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {saved.slice(0, 4).map((item) => (
                <div
                  key={item.id}
                  className="bg-white border border-[#E8E3DC] rounded-xl p-4 shadow-xs flex flex-col gap-2"
                >
                  <span className="text-[11px] text-[#716D68]">{item.providerName}</span>
                  <h4 className="font-newsreader text-sm font-bold text-[#292624] leading-snug">
                    {item.scholarshipTitle}
                  </h4>
                  <div className="flex items-center justify-between pt-2 border-t border-[#E8E3DC]/60 text-xs">
                    <span className="font-semibold text-[#477C70]">
                      {item.currencyCode} {item.awardAmount?.toLocaleString() ?? ""}
                    </span>
                    <Link
                      href={`/explore/${item.scholarshipId}`}
                      className="text-xs font-semibold text-[#477C70] hover:underline"
                    >
                      Apply Now →
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

