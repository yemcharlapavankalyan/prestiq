import React from "react";

interface MetricCardProps {
  label: string;
  value: string | number;
  subValue?: string;
  subtext?: string;
  indicatorText?: string;
  trend?: {
    value: string;
    isPositive?: boolean;
  };
  icon: string;
  variant?: "default" | "urgent" | "success";
}

export function MetricCard({
  label,
  value,
  subValue,
  subtext,
  indicatorText,
  trend,
  icon,
  variant = "default",
}: MetricCardProps) {
  const iconColor =
    variant === "urgent"
      ? "text-[#B85D19] bg-[#FDF4E7]"
      : variant === "success"
      ? "text-[#477C70] bg-[#EBF2F0]"
      : "text-[#716D68] bg-[#F4ECE9]";

  const secondaryText = subValue || subtext;

  return (
    <div className="bg-white border border-[#E8E3DC] rounded-xl p-5 shadow-[0_2px_8px_rgba(41,38,36,0.03)] hover:shadow-[0_4px_16px_rgba(41,38,36,0.06)] transition-all flex flex-col justify-between">
      <div className="flex items-start justify-between">
        <span className="text-xs uppercase tracking-wider text-[#716D68] font-bold">
          {label}
        </span>
        <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${iconColor}`}>
          <span className="material-symbols-outlined text-[20px]">{icon}</span>
        </div>
      </div>

      <div className="mt-4">
        <div className="flex items-baseline gap-2">
          <span className="font-newsreader text-3xl font-semibold text-[#292624] tracking-tight">
            {value}
          </span>
          {trend && (
            <span
              className={`text-xs font-semibold px-1.5 py-0.5 rounded ${
                trend.isPositive
                  ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                  : "bg-red-50 text-red-700 border border-red-200"
              }`}
            >
              {trend.value}
            </span>
          )}
        </div>
        {(secondaryText || indicatorText) && (
          <div className="flex items-center gap-1.5 mt-2 text-xs text-[#716D68]">
            {variant === "urgent" && <span className="w-2 h-2 rounded-full bg-[#B85D19] animate-pulse"></span>}
            {variant === "success" && <span className="w-2 h-2 rounded-full bg-[#477C70]"></span>}
            <span>{secondaryText || indicatorText}</span>
          </div>
        )}
      </div>
    </div>
  );
}
