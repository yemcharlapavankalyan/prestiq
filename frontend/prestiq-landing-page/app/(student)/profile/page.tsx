"use client";

import React, { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api/client";
import { StudentProfile } from "@/lib/api/types";
import { PageHeader } from "@/components/shared/page-header";

export default function StudentProfilePage() {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" } | null>(null);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [educationLevel, setEducationLevel] = useState("UNDERGRADUATE");
  const [institution, setInstitution] = useState("");
  const [major, setMajor] = useState("");
  const [gpa, setGpa] = useState("8.50");
  const [gpaScale, setGpaScale] = useState("10.00");
  const [state, setState] = useState("Maharashtra");
  const [income, setIncome] = useState("500000");
  const [citizenship, setCitizenship] = useState("IN");
  const [currentYear, setCurrentYear] = useState("Year 3");

  useEffect(() => {
    async function loadProfile() {
      try {
        const data = await api.get<StudentProfile>("/api/v1/students/profile");
        if (data) {
          setProfile(data);
          setFirstName(data.firstName || "");
          setLastName(data.lastName || "");
          setEducationLevel(data.educationLevel === "DOCTORATE" ? "DOCTORAL" : data.educationLevel || "UNDERGRADUATE");
          setInstitution(data.institutionName || "");
          setMajor(data.majorFieldOfStudy || "");
          setGpa(data.cumulativeGpa != null ? data.cumulativeGpa.toString() : "8.50");
          setGpaScale(data.gpaScale != null ? data.gpaScale.toString() : "10.00");
          setState(data.stateProvince || "Maharashtra");
          setIncome(data.annualFamilyIncome != null ? data.annualFamilyIncome.toString() : "500000");
          setCitizenship(data.citizenshipCountry || "IN");
          setCurrentYear(data.currentYearOrSemester || "Year 3");
        }
      } catch {
        // No existing profile
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const payload = {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        citizenshipCountry: citizenship.trim().toUpperCase() || "IN",
        stateProvince: state.trim() || "Maharashtra",
        educationLevel,
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

      const updated = await api.put<StudentProfile>("/api/v1/students/profile", payload);
      setProfile(updated);
      setMessage({ text: "Profile updated successfully!", type: "success" });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setMessage({ text: err.detail || err.message, type: "error" });
      } else {
        setMessage({ text: "Failed to update profile.", type: "error" });
      }
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <div className="w-8 h-8 border-3 border-[#477C70] border-t-transparent rounded-full animate-spin"></div>
        <p className="text-xs font-semibold text-[#716D68]">Loading your profile data...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-8 max-w-4xl mx-auto">
      <PageHeader
        badgeText="Scholar Profile"
        badgeIcon="badge"
        title="Profile & Academic Credentials"
        description="Maintain your demographic and academic metrics for accurate DBT eligibility evaluations and matching."
      />

      {message && (
        <div
          className={`p-4 rounded-xl border text-xs font-medium flex items-center justify-between ${
            message.type === "success"
              ? "bg-[#EBF2F0] border-[#477C70]/30 text-[#477C70]"
              : "bg-[#FAF2EF] border-[#BA1A1A]/30 text-[#BA1A1A]"
          }`}
        >
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="text-[#716D68] hover:text-[#292624]">✕</button>
        </div>
      )}

      <form onSubmit={handleSave} className="bg-white border border-[#E8E3DC] rounded-2xl p-6 sm:p-8 shadow-xs flex flex-col gap-6">
        <div className="flex items-center justify-between border-b border-[#E8E3DC] pb-4">
          <h2 className="font-newsreader text-xl font-bold text-[#292624]">
            Personal & Demographic Details
          </h2>
          <span className="text-xs text-[#716D68]">ISO 3166-1 / IN DBT Compliant</span>
        </div>

        {/* First & Last Name */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">First Name</label>
            <input
              type="text"
              required
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">Last Name</label>
            <input
              type="text"
              required
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
        </div>

        {/* Education Level Selector */}
        <div>
          <label className="block text-xs font-semibold text-[#292624] mb-2">Education Level</label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {[
              { id: "HIGH_SCHOOL", label: "Class 10/12" },
              { id: "UNDERGRADUATE", label: "Undergraduate" },
              { id: "POSTGRADUATE", label: "Postgraduate" },
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
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">Institution / University</label>
            <input
              type="text"
              required
              value={institution}
              onChange={(e) => setInstitution(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">Major / Degree Branch</label>
            <input
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
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">
              Cumulative GPA (Scale: {gpaScale})
            </label>
            <input
              type="number"
              step="0.01"
              required
              value={gpa}
              onChange={(e) => setGpa(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">Home State / UT</label>
            <input
              type="text"
              required
              value={state}
              onChange={(e) => setState(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-[#292624] mb-1.5">Annual Family Income (₹)</label>
            <input
              type="number"
              required
              value={income}
              onChange={(e) => setIncome(e.target.value)}
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
        </div>

        {/* Submit Button */}
        <div className="pt-4 border-t border-[#E8E3DC] flex items-center justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2.5 rounded-lg bg-[#477C70] hover:bg-[#3B675D] disabled:opacity-50 text-white font-semibold text-xs transition-colors shadow-xs flex items-center gap-2"
          >
            {saving ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Saving Changes...</span>
              </>
            ) : (
              <span>Save & Update Profile</span>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}

