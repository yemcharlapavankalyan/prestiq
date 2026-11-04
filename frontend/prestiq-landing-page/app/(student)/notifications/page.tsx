"use client";

import React, { useState } from "react";
import { PageHeader } from "@/components/shared/page-header";

const mockNotifications = [
  {
    id: "n-1",
    title: "Urgent Deadline: Tata Trusts STEM Grant",
    message: "Application submissions close in 3 days. Ensure all required bonafide attachments are uploaded.",
    type: "DEADLINE",
    time: "2 hours ago",
    unread: true,
  },
  {
    id: "n-2",
    title: "Academic Eligibility Re-evaluated",
    message: "Your profile was matched with 4 newly verified corporate fellowship opportunities for B.Tech CSE.",
    type: "MATCH",
    time: "Yesterday, 3:45 PM",
    unread: true,
  },
  {
    id: "n-3",
    title: "Central Sector Scheme DBT Verification Complete",
    message: "Your institution nodal officer certified your enrollment. Reference #NSP-2024-9921 updated.",
    type: "SYSTEM",
    time: "24 Oct 2026",
    unread: false,
  },
];

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(mockNotifications);

  const markAllRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, unread: false })));
  };

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        badgeText="Notifications Center"
        badgeIcon="notifications"
        title="Notifications"
        description="Stay informed on upcoming deadlines, state portal verification changes, and newly matched grant opportunities."
        actions={
          <button
            onClick={markAllRead}
            className="px-4 py-2 rounded-lg bg-white border border-[#E8E3DC] text-xs font-semibold text-[#292624] hover:bg-[#FAF2EF] transition-colors shadow-xs"
          >
            Mark all as read
          </button>
        }
      />

      <div className="flex flex-col gap-3">
        {notifications.map((n) => (
          <div
            key={n.id}
            className={`p-5 rounded-xl border transition-all flex items-start justify-between gap-4 ${
              n.unread
                ? "bg-white border-[#E8E3DC] shadow-xs"
                : "bg-[#FAF2EF]/70 border-[#E8E3DC]/60 opacity-80"
            }`}
          >
            <div className="flex items-start gap-3.5">
              <div
                className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${
                  n.type === "DEADLINE"
                    ? "bg-[#FDF4E7] text-[#B85D19]"
                    : n.type === "MATCH"
                    ? "bg-[#EBF2F0] text-[#477C70]"
                    : "bg-[#F4ECE9] text-[#716D68]"
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">
                  {n.type === "DEADLINE" ? "alarm" : n.type === "MATCH" ? "auto_awesome" : "verified"}
                </span>
              </div>
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold text-sm text-[#292624]">{n.title}</h3>
                  {n.unread && <span className="w-2 h-2 rounded-full bg-[#D4AF7C]"></span>}
                </div>
                <p className="text-xs sm:text-sm text-[#716D68] leading-relaxed">{n.message}</p>
                <span className="text-[11px] text-[#716D68]/80 mt-1">{n.time}</span>
              </div>
            </div>

            <button
              onClick={() =>
                setNotifications((prev) =>
                  prev.map((item) => (item.id === n.id ? { ...item, unread: !item.unread } : item))
                )
              }
              className="text-xs text-[#716D68] hover:text-[#292624] shrink-0"
            >
              {n.unread ? "Mark read" : "Mark unread"}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
