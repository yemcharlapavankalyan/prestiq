"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";
import { api, ApiError } from "@/lib/api/client";
import { StudentProfile } from "@/lib/api/types";

export default function OnboardingPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [educationLevel, setEducationLevel] = useState("UNDERGRADUATE");
  const [institution, setInstitution] = useState("IIT Bombay");
  const [major, setMajor] = useState("Computer Science & Engineering");
  const [gpa, setGpa] = useState("9.20");
  const [gpaScale, setGpaScale] = useState("10.00");
  const [state, setState] = useState("Maharashtra");
  const [income, setIncome] = useState("450000");
  const [citizenship, setCitizenship] = useState("IN");
  const [currentYear, setCurrentYear] = useState("Year 3");
  const [loading, setLoading] = useState(false);
  const [fetchingExisting, setFetchingExisting] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Sync user details if available
  useEffect(() => {
    if (user) {
      if (user.firstName) setFirstName(user.firstName);
      if (user.lastName) setLastName(user.lastName);
    }
  }, [user]);

  // Load existing profile when revisiting onboarding
  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }

    const loadProfile = async () => {
      try {
        const profile = await api.get<StudentProfile>("/api/v1/students/profile");
        if (profile) {
          if (profile.firstName) setFirstName(profile.firstName);
          if (profile.lastName) setLastName(profile.lastName);
          if (profile.educationLevel) {
            setEducationLevel(profile.educationLevel === "DOCTORATE" ? "DOCTORAL" : profile.educationLevel);
          }
          if (profile.institutionName) setInstitution(profile.institutionName);
          if (profile.majorFieldOfStudy) setMajor(profile.majorFieldOfStudy);
          if (profile.cumulativeGpa != null) setGpa(profile.cumulativeGpa.toString());
          if (profile.gpaScale != null) setGpaScale(profile.gpaScale.toString());
          if (profile.stateProvince) setState(profile.stateProvince);
          if (profile.annualFamilyIncome != null) setIncome(profile.annualFamilyIncome.toString());
          if (profile.citizenshipCountry) setCitizenship(profile.citizenshipCountry);
          if (profile.currentYearOrSemester) setCurrentYear(profile.currentYearOrSemester);
        }
      } catch {
        // Profile does not exist yet; new user onboarding
      } finally {
        setFetchingExisting(false);
      }
    };

    loadProfile();
  }, [authLoading, isAuthenticated, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const payload = {
        firstName: firstName.trim() || user?.firstName || "Scholar",
        lastName: lastName.trim() || user?.lastName || "Student",
        citizenshipCountry: citizenship.trim().toUpperCase() || "IN",
        stateProvince: state.trim() || "Maharashtra",
        educationLevel: educationLevel === "DOCTORATE" ? "DOCTORAL" : educationLevel,
        institutionName: institution.trim(),
        majorFieldOfStudy: major.trim(),
        currentYearOrSemester: currentYear.trim() || "Year 3",
        cumulativeGpa: parseFloat(gpa) || 8.0,
        gpaScale: parseFloat(gpaScale) || 10.0,
        annualFamilyIncome: parseFloat(income) || 500000.0,
        currencyCode: "INR",
        firstGenStudent: false,
        militaryVeteran: false,
        disabilityStatus: false,
      };

      await api.post<StudentProfile>("/api/v1/students/profile", payload);
      router.push("/dashboard");
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setError(err.detail || err.message);
      } else {
        setError("Failed to save student profile. Please check all fields.");
      }
    } finally {
      setLoading(false);
    }
  };

  if (fetchingExisting) {
    return (
      <div className="w-full max-w-2xl bg-white rounded-2xl border border-[#E8E3DC] p-12 text-center shadow-xs">
        <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
        <p className="text-xs font-semibold text-[#716D68]">Loading your onboarding profile...</p>
      </div>
    );
  }

  return (
    <div className="w-full max-w-2xl bg-white rounded-2xl border border-[#E8E3DC] p-6 sm:p-10 shadow-[0_4px_24px_rgba(41,38,36,0.05)]">
      {/* Header */}
      <div className="flex flex-col gap-2 mb-8 text-center sm:text-left">
        <div className="inline-flex items-center gap-1.5 self-center sm:self-start px-3 py-1 rounded-full bg-[#FAF2EF] border border-[#E8E3DC] text-xs font-semibold text-[#477C70]">
          <span className="material-symbols-outlined text-[14px]">school</span>
          <span>Scholar Onboarding · 3 Minutes</span>
        </div>
        <h1 className="font-newsreader text-3xl sm:text-4xl font-bold text-[#292624] tracking-tight">
          Let’s personalize your scholarship journey
        </h1>
        <p className="text-sm text-[#716D68]">
          PrestIQ matches you deterministically with Indian central schemes, state DBT grants, and private corporate endowments.
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-lg bg-[#FAF2EF] border border-[#BA1A1A]/30 text-xs font-medium text-[#BA1A1A] flex items-center gap-2">
          <span className="material-symbols-outlined text-[16px]">error</span>
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        {/* Name Fields */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="onb-fname" className="block text-xs font-semibold text-[#292624] mb-1.5">
              First Name
            </label>
            <input
              id="onb-fname"
              type="text"
              required
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label htmlFor="onb-lname" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Last Name
            </label>
            <input
              id="onb-lname"
              type="text"
              required
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
        </div>

        {/* Education Level */}
        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-[#716D68] mb-2">
            Current Education Level
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {[
              { id: "HIGH_SCHOOL", label: "Class 10 / 12" },
              { id: "UNDERGRADUATE", label: "Undergraduate (B.Tech / B.Sc)" },
              { id: "POSTGRADUATE", label: "Postgraduate (M.Tech / M.Sc)" },
              { id: "DOCTORAL", label: "PhD / Research" },
            ].map((lvl) => (
              <button
                type="button"
                key={lvl.id}
                onClick={() => setEducationLevel(lvl.id)}
                className={`p-3 text-left rounded-lg border text-xs font-semibold transition-all ${
                  educationLevel === lvl.id
                    ? "bg-[#477C70] text-white border-[#477C70]"
                    : "bg-[#FAF2EF] text-[#292624] border-[#E8E3DC] hover:border-[#477C70]/40"
                }`}
              >
                {lvl.label}
              </button>
            ))}
          </div>
        </div>

        {/* Institution & Major */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="onb-institution" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Institution / College Name
            </label>
            <input
              id="onb-institution"
              type="text"
              required
              value={institution}
              onChange={(e) => setInstitution(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label htmlFor="onb-major" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Major / Degree Branch
            </label>
            <input
              id="onb-major"
              type="text"
              required
              value={major}
              onChange={(e) => setMajor(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
        </div>

        {/* CGPA & State & Income */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label htmlFor="onb-gpa" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Cumulative GPA (Scale: {gpaScale})
            </label>
            <input
              id="onb-gpa"
              type="number"
              step="0.01"
              required
              value={gpa}
              onChange={(e) => setGpa(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label htmlFor="onb-state" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Home State / UT
            </label>
            <input
              id="onb-state"
              type="text"
              required
              value={state}
              onChange={(e) => setState(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label htmlFor="onb-income" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Annual Family Income (₹)
            </label>
            <input
              id="onb-income"
              type="number"
              required
              value={income}
              onChange={(e) => setIncome(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={loading}
          className="w-full h-12 mt-4 rounded-lg bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-60 text-white font-semibold text-sm transition-colors shadow-xs flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Saving Profile...</span>
            </>
          ) : (
            <>
              <span>Calculate My Matches & Enter Dashboard</span>
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </>
          )}
        </button>
      </form>
    </div>
  );
}
