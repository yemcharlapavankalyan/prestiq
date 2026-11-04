import React from "react";

export type BadgeVariant =
  | "verified"
  | "pending"
  | "urgent"
  | "neutral"
  | "gold"
  | "error";

interface StatusBadgeProps {
  label: string;
  variant?: BadgeVariant;
  icon?: string;
  className?: string;
}

export function StatusBadge({
  label,
  variant = "neutral",
  icon,
  className = "",
}: StatusBadgeProps) {
  const variantStyles: Record<BadgeVariant, string> = {
    verified: "bg-[#EBF2F0] text-[#477C70] border-[#477C70]/30",
    pending: "bg-[#FAF2EF] text-[#76592E] border-[#D4AF7C]/40",
    urgent: "bg-[#FDF4E7] text-[#B85D19] border-[#B85D19]/30",
    neutral: "bg-[#F4ECE9] text-[#716D68] border-[#E8E3DC]",
    gold: "bg-[#FEF8EE] text-[#8C6527] border-[#D4AF7C]",
    error: "bg-[#FFDAD6] text-[#93000A] border-[#BA1A1A]/30",
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${variantStyles[variant]} ${className}`}
    >
      {icon && <span className="material-symbols-outlined text-[14px]">{icon}</span>}
      <span>{label}</span>
    </span>
  );
}
