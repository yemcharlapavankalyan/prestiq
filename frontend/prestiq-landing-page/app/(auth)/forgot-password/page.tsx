"use client";

import React, { useState } from "react";
import Link from "next/link";

export default function ForgotPasswordPage() {
  const [step, setStep] = useState<"email" | "otp" | "reset" | "done">("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [newPassword, setNewPassword] = useState("");

  const handleSendCode = (e: React.FormEvent) => {
    e.preventDefault();
    setStep("otp");
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setStep("reset");
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    setStep("done");
  };

  const handleOtpChange = (index: number, val: string) => {
    if (val.length > 1) val = val[0];
    const newArr = [...otp];
    newArr[index] = val;
    setOtp(newArr);
  };

  return (
    <div className="w-full max-w-md bg-white rounded-2xl border border-[#E8E3DC] p-6 sm:p-8 shadow-[0_4px_24px_rgba(41,38,36,0.05)]">
      <div className="flex flex-col gap-2 mb-6 text-center">
        <div className="inline-flex items-center gap-1.5 self-center px-3 py-1 rounded-full bg-[#FAF2EF] border border-[#E8E3DC] text-xs font-semibold text-[#477C70]">
          <span className="material-symbols-outlined text-[14px]">verified_user</span>
          <span>Security Recovery Node</span>
        </div>
        <h1 className="font-newsreader text-3xl font-bold text-[#292624] tracking-tight">
          Identity Verification
        </h1>
        <p className="text-xs sm:text-sm text-[#716D68]">
          {step === "email" && "Enter your registered email address to receive a secure recovery OTP."}
          {step === "otp" && `Enter the 6-digit code dispatched to ${email || "your registered email"}.`}
          {step === "reset" && "Create a new strong password for your PrestIQ scholar profile."}
          {step === "done" && "Your password has been successfully updated."}
        </p>
      </div>

      {step === "email" && (
        <form onSubmit={handleSendCode} className="flex flex-col gap-4">
          <div>
            <label htmlFor="fp-email" className="block text-xs font-semibold text-[#292624] mb-1.5">
              Registered email address
            </label>
            <input
              id="fp-email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="student@university.edu"
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <button
            type="submit"
            className="w-full h-11 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white font-semibold text-sm transition-colors shadow-xs"
          >
            Send Verification Code
          </button>
        </form>
      )}

      {step === "otp" && (
        <form onSubmit={handleVerifyOtp} className="flex flex-col gap-4">
          <div className="flex justify-between gap-2 my-2">
            {otp.map((digit, i) => (
              <input
                key={i}
                type="text"
                maxLength={1}
                value={digit}
                onChange={(e) => handleOtpChange(i, e.target.value)}
                className="w-12 h-12 text-center text-lg font-bold border border-[#E8E3DC] rounded-lg focus:outline-none focus:border-[#477C70] focus:ring-1 focus:ring-[#477C70]"
              />
            ))}
          </div>
          <button
            type="submit"
            className="w-full h-11 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white font-semibold text-sm transition-colors shadow-xs"
          >
            Verify 6-Digit OTP
          </button>
        </form>
      )}

      {step === "reset" && (
        <form onSubmit={handleResetPassword} className="flex flex-col gap-4">
          <div>
            <label htmlFor="fp-newpassword" className="block text-xs font-semibold text-[#292624] mb-1.5">
              New password
            </label>
            <input
              id="fp-newpassword"
              type="password"
              required
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full h-11 px-3 rounded-lg border border-[#E8E3DC] bg-white text-sm text-[#292624] focus:outline-none focus:border-[#477C70]"
            />
          </div>
          <button
            type="submit"
            className="w-full h-11 rounded-lg bg-[#477C70] hover:bg-[#3B675D] text-white font-semibold text-sm transition-colors shadow-xs"
          >
            Save New Password
          </button>
        </form>
      )}

      {step === "done" && (
        <div className="flex flex-col gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-[#EBF2F0] text-[#477C70] flex items-center justify-center mx-auto">
            <span className="material-symbols-outlined text-[24px]">check</span>
          </div>
          <Link
            href="/login"
            className="w-full h-11 rounded-lg bg-[#477C70] text-white font-semibold text-sm flex items-center justify-center"
          >
            Sign In with New Password
          </Link>
        </div>
      )}

      <div className="mt-6 pt-4 border-t border-[#E8E3DC] text-center text-xs text-[#716D68]">
        Remember your password?{" "}
        <Link href="/login" className="text-[#477C70] hover:underline font-semibold">
          Return to login
        </Link>
      </div>
    </div>
  );
}
