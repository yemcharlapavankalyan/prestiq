import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";

export default function AdminSettingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        badge="Central Registry Node v2.4 · Cluster Configuration"
        title="System Settings"
        description="Manage microservice gateway bindings, AI vector embedding parameters, DigiLocker credentials, and security compliance policies."
        actions={
          <button className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded bg-[#477C70] text-white hover:bg-[#3d6a60] transition shadow-sm">
            <span className="material-symbols-outlined text-[16px]">save</span>
            Save System Configuration
          </button>
        }
      />

      {/* Tabs */}
      <div className="flex border-b border-[#E8E3DC] gap-6 text-xs font-medium">
        <button className="pb-3 border-b-2 border-[#477C70] text-[#477C70] font-semibold">
          Microservices & Gateway
        </button>
        <button className="pb-3 border-b-2 border-transparent text-[#716D68] hover:text-[#292624] transition">
          AI & Matching Engine
        </button>
        <button className="pb-3 border-b-2 border-transparent text-[#716D68] hover:text-[#292624] transition">
          DigiLocker & Verification Bridges
        </button>
        <button className="pb-3 border-b-2 border-transparent text-[#716D68] hover:text-[#292624] transition">
          Security & Access Control
        </button>
      </div>

      {/* Setting Sections */}
      <div className="space-y-6">
        {/* Microservices Health & Status */}
        <div className="bg-white p-5 rounded-xl border border-[#E8E3DC] shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-newsreader text-base font-semibold text-[#292624]">
                Backend Services Topology
              </h3>
              <p className="text-xs text-[#716D68]">
                Real-time connection status to PrestIQ microservices via Spring Cloud Gateway
              </p>
            </div>
            <StatusBadge variant="verified" label="8 / 8 Services Healthy" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
            {[
              { name: "API Gateway", port: "8080", route: "/api/v1/*", status: "ONLINE" },
              { name: "Auth Service", port: "8081", route: "/api/v1/auth/*", status: "ONLINE" },
              { name: "Student Service", port: "8082", route: "/api/v1/students/*", status: "ONLINE" },
              { name: "Scholarship Service", port: "8083", route: "/api/v1/scholarships/*", status: "ONLINE" },
              { name: "Matching Engine", port: "8084", route: "/api/v1/matches/*", status: "ONLINE" },
              { name: "Application Service", port: "8085", route: "/api/v1/applications/*", status: "ONLINE" },
              { name: "Notification Service", port: "8086", route: "/api/v1/notifications/*", status: "ONLINE" },
              { name: "Audit Service", port: "8087", route: "/api/v1/audit/*", status: "ONLINE" },
            ].map((svc, idx) => (
              <div
                key={idx}
                className="p-3 bg-[#F8F6F2]/70 rounded-lg border border-[#E8E3DC] flex items-center justify-between text-xs"
              >
                <div>
                  <div className="font-semibold text-[#292624]">{svc.name}</div>
                  <div className="font-mono text-[11px] text-[#716D68] mt-0.5">
                    Port: {svc.port} · {svc.route}
                  </div>
                </div>
                <span className="font-mono text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                  {svc.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Global System Preferences */}
        <div className="bg-white p-5 rounded-xl border border-[#E8E3DC] shadow-xs space-y-4">
          <h3 className="font-newsreader text-base font-semibold text-[#292624]">
            Global Registry Parameters
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block font-medium text-[#292624] mb-1.5">
                Active Academic Year
              </label>
              <input
                type="text"
                defaultValue="AY 2024–2025"
                className="w-full px-3 py-2 bg-[#F8F6F2] border border-[#E8E3DC] rounded-lg text-[#292624] focus:outline-none focus:border-[#477C70]"
              />
            </div>
            <div>
              <label className="block font-medium text-[#292624] mb-1.5">
                Primary API Gateway Endpoint
              </label>
              <input
                type="text"
                defaultValue="http://localhost:8080"
                className="w-full px-3 py-2 bg-[#F8F6F2] border border-[#E8E3DC] rounded-lg text-[#292624] font-mono focus:outline-none focus:border-[#477C70]"
              />
            </div>
            <div>
              <label className="block font-medium text-[#292624] mb-1.5">
                Vector Similarity Match Threshold
              </label>
              <input
                type="number"
                step="0.01"
                defaultValue="0.75"
                className="w-full px-3 py-2 bg-[#F8F6F2] border border-[#E8E3DC] rounded-lg text-[#292624] font-mono focus:outline-none focus:border-[#477C70]"
              />
            </div>
            <div>
              <label className="block font-medium text-[#292624] mb-1.5">
                Audit Trail Cryptographic Hashing
              </label>
              <select className="w-full px-3 py-2 bg-[#F8F6F2] border border-[#E8E3DC] rounded-lg text-[#292624] focus:outline-none focus:border-[#477C70]">
                <option>SHA-256 (Government Compliance Standard)</option>
                <option>SHA-512</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
