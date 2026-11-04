import React from "react";

interface PageHeaderProps {
  badgeText?: string;
  badge?: string;
  badgeIcon?: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export function PageHeader({
  badgeText,
  badge,
  badgeIcon,
  title,
  description,
  actions,
}: PageHeaderProps) {
  const activeBadge = badgeText || badge;

  return (
    <header className="flex flex-col lg:flex-row lg:items-end justify-between gap-4 pb-6 border-b border-[#E8E3DC]">
      <div className="flex flex-col gap-2 max-w-3xl">
        {activeBadge && (
          <div className="inline-flex items-center gap-2 self-start px-3 py-1 rounded-full bg-white border border-[#E8E3DC] shadow-xs text-xs font-medium text-[#716D68]">
            {badgeIcon && <span className="material-symbols-outlined text-[16px] text-[#477C70]">{badgeIcon}</span>}
            <span>{activeBadge}</span>
          </div>
        )}
        <h1 className="font-newsreader text-3xl sm:text-4xl font-semibold text-[#292624] tracking-tight">
          {title}
        </h1>
        {description && (
          <p className="text-sm sm:text-base text-[#716D68] leading-relaxed">
            {description}
          </p>
        )}
      </div>

      {actions && <div className="flex items-center gap-3 shrink-0 pt-2 lg:pt-0">{actions}</div>}
    </header>
  );
}
