"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage("");

    try {
      await register({ firstName, lastName, email, password });
      router.push("/onboarding");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Registration failed. Please check your details.";
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-2xl border border-[#E8E3DC] p-6 sm:p-8 shadow-[0_4px_24px_rgba(41,38,36,0.05)]">
      <div className="flex flex-col gap-2 mb-6 text-center">
        <div className="inline-flex items-center gap-1.5 self-center px-3 py-1 rounded-full bg-[#FAF2EF] border border-[#E8E3DC] text-xs font-semibold text-[#477C70]">
          <span className="material-symbols-outlined text-[14px]">school</span>
          <span>Join 140,000+ Indian Scholars</span>
        </div>
        <h1 className="font-newsreader text-3xl font-bold text-[#292624] tracking-tight">
          Create your account
        </h1>
        <p className="text-xs sm:text-sm text-[#716D68]">
          Discover vetted scholarships matching your degree, state, caste, and income eligibility.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="reg-firstname" className="block text-xs font-semibold text-[#292624] mb-1.5">
              First name
            </label>
            <input
              id="reg-firstname"
              type="text"
              required
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              placeholder="Aarav"
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
            />
          </div>
          <div>
            <label htmlFor="reg-lastname" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Last name
            </label>
            <input
              id="reg-lastname"
              type="text"
              required
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              placeholder="Sharma"
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
            />
          </div>
        </div>

        <div>
          <label htmlFor="reg-email" className="block text-xs font-semibold text-[#292624] mb-1.5">
            Email address
          </label>
          <input
            id="reg-email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="aarav.sharma@iitb.ac.in"
            className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
          />
        </div>

        <div>
          <label htmlFor="reg-password" className="block text-xs font-semibold text-[#292624] mb-1.5">
            Create password
          </label>
          <input
            id="reg-password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Min. 8 chars with upper, digit & symbol"
            className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
          />
        </div>

        <div className="flex items-start gap-2 pt-1">
          <input
            id="reg-terms"
            type="checkbox"
            required
            checked={agreed}
            onChange={(e) => setAgreed(e.target.checked)}
            className="mt-1 rounded border-[#E8E3DC] text-[#477C70] focus:ring-[#477C70]"
          />
          <label htmlFor="reg-terms" className="text-xs text-[#716D68] leading-snug">
            I agree to the PrestIQ Scholarship Portal Terms of Service and Privacy Policy.
          </label>
        </div>

        <button
          type="submit"
          disabled={loading || !agreed}
          className="w-full h-11 mt-2 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white font-semibold text-sm transition-colors shadow-xs flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60"
        >
          <span>Continue to Onboarding</span>
          <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
        </button>
      </form>

      <div className="mt-6 pt-4 border-t border-[#E8E3DC] text-center text-xs text-[#716D68]">
        Already have a scholar account?{" "}
        <Link href="/login" className="text-[#477C70] hover:underline font-semibold">
          Sign in
        </Link>
      </div>
    </div>
  );
}
