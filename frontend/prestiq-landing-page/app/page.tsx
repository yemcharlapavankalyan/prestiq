import { Header } from "@/components/header"
import { HeroSection } from "@/components/hero-section"
import { DashboardPreview } from "@/components/dashboard-preview"
import { HowItWorks, FeaturedOpportunities, Benefits, FinalCTA } from "@/components/prestiq-sections"
import { ProviderMarquee } from "@/components/provider-marquee"

export default function LandingPage() {
  return <main className="min-h-screen overflow-hidden bg-prestiq-bg"><Header /><HeroSection /><ProviderMarquee /><DashboardPreview /><HowItWorks /><FeaturedOpportunities /><Benefits /><FinalCTA /><footer className="border-t border-prestiq-border px-6 py-8"><div className="mx-auto flex max-w-6xl flex-col justify-between gap-4 text-sm text-prestiq-muted sm:flex-row"><span className="font-medium text-prestiq-ink">PrestIQ</span><span>© 2026 PrestIQ. Built for clearer possibilities.</span><div className="flex gap-5"><a href="#about" className="hover:text-prestiq-ink">About</a><a href="#" className="hover:text-prestiq-ink">Privacy</a><a href="#" className="hover:text-prestiq-ink">Terms</a></div></div></footer></main>
}
