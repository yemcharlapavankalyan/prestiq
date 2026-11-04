"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth/auth-context";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMessage("");

    try {
      const auth = await login(email, password);
      if (auth.user?.roles?.includes("ROLE_ADMIN") || auth.user?.roles?.includes("ROLE_SCHOLARSHIP_OFFICER")) {
        router.push("/admin/dashboard");
      } else {
        router.push("/dashboard");
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Authentication failed. Please verify your credentials.";
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-2xl border border-[#E8E3DC] p-6 sm:p-8 shadow-[0_4px_24px_rgba(41,38,36,0.05)]">
      {/* Header */}
      <div className="flex flex-col gap-2 mb-6 text-center">
        <div className="inline-flex items-center gap-1.5 self-center px-3 py-1 rounded-full bg-[#FAF2EF] border border-[#E8E3DC] text-xs font-semibold text-[#477C70]">
          <span className="material-symbols-outlined text-[14px]">school</span>
          <span>Academic Portal · AY 2024–25</span>
        </div>
        <h1 className="font-newsreader text-3xl font-bold text-[#292624] tracking-tight">
          Welcome back
        </h1>
        <p className="text-xs sm:text-sm text-[#716D68]">
          Sign in to your PrestIQ scholar profile to manage grants, applications, and verified eligibility.
        </p>
      </div>

      {errorMessage && (
        <div className="p-3 mb-4 rounded-lg bg-[#FFDAD6] border border-[#BA1A1A]/30 text-xs font-medium text-[#93000A]">
          {errorMessage}
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div>
          <label htmlFor="login-email" className="block text-xs font-semibold text-[#292624] mb-1.5">
            Email address
          </label>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#716D68] text-[18px]">
              mail
            </span>
            <input
              id="login-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="student@university.edu"
              className="w-full h-11 pl-9 pr-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
            />
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label htmlFor="login-password" className="text-xs font-semibold text-[#292624]">
              Password
            </label>
            <Link
              href="/forgot-password"
              className="text-xs text-[#477C70] hover:text-[#3B675D] font-medium"
            >
              Forgot password?
            </Link>
          </div>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#716D68] text-[18px]">
              lock
            </span>
            <input
              id="login-password"
              type={showPassword ? "text" : "password"}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full h-11 pl-9 pr-10 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] placeholder:text-[#716D68]/60 focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]/30 transition-all"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#716D68] hover:text-[#292624]"
            >
              <span className="material-symbols-outlined text-[18px]">
                {showPassword ? "visibility_off" : "visibility"}
              </span>
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full h-11 mt-2 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white font-semibold text-sm transition-colors shadow-xs flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70"
        >
          {loading ? (
            <span>Authenticating...</span>
          ) : (
            <>
              <span>Sign In to Scholar Portal</span>
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </>
          )}
        </button>
      </form>

      {/* Switch to Register */}
      <div className="mt-6 pt-4 border-t border-[#E8E3DC] text-center text-xs text-[#716D68]">
        Don’t have an account yet?{" "}
        <Link href="/register" className="text-[#477C70] hover:underline font-semibold">
          Create scholar account
        </Link>
      </div>
    </div>
  );
}
