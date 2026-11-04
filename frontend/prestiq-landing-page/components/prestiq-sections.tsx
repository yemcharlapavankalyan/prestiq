import { ArrowUpRight, Check, Clock3, FileText, GraduationCap, Search, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"

const opportunities = [
  { name: "Merit Pathway Award", provider: "Illustrative opportunity", amount: "Up to ₹75,000", deadline: "12 Aug 2025", tag: "Academic profile" },
  { name: "Future Leaders Grant", provider: "Illustrative opportunity", amount: "Up to ₹50,000", deadline: "28 Aug 2025", tag: "Leadership" },
  { name: "Access & Aspire Fund", provider: "Illustrative opportunity", amount: "Up to ₹1,00,000", deadline: "05 Sep 2025", tag: "Financial need" },
]

export function HowItWorks() {
  const steps = [
    ["01", "Build your profile", "Tell us about your academics, interests, and circumstances once."],
    ["02", "Discover your matches", "See opportunities organized around the details that matter to you."],
    ["03", "Track your next steps", "Keep requirements, deadlines, and progress clear in one place."],
  ]
  return <section id="how-it-works" className="border-y border-prestiq-border bg-prestiq-surface py-24 sm:py-32"><div className="mx-auto max-w-6xl px-6"><div className="mb-16 max-w-xl"><p className="eyebrow">A clearer way forward</p><h2 className="mt-5 font-serif text-5xl leading-[.98] text-prestiq-ink sm:text-7xl">From searching to submitting, with less guesswork.</h2></div><div className="grid gap-10 md:grid-cols-3">{steps.map(([number, title, description]) => <article key={number} className="border-t border-prestiq-border pt-5"><span className="font-mono text-xs text-prestiq-accent">{number}</span><h3 className="mt-10 text-xl font-medium text-prestiq-ink">{title}</h3><p className="mt-3 max-w-xs text-sm leading-6 text-prestiq-muted">{description}</p></article>)}</div></div></section>
}

export function FeaturedOpportunities() {
  return <section id="explore" className="py-24 sm:py-32"><div className="mx-auto max-w-6xl px-6"><div className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end"><div><p className="eyebrow">Explore opportunities</p><h2 className="mt-5 font-serif text-5xl leading-none text-prestiq-ink sm:text-6xl">Possibility, made visible.</h2></div><p className="max-w-xs text-sm leading-6 text-prestiq-muted">Illustrative examples for the product preview. Verified scholarship data can be connected when you are ready.</p></div><div className="mt-14 grid gap-4 md:grid-cols-3">{opportunities.map((item) => <article key={item.name} className="group rounded-2xl border border-prestiq-border bg-prestiq-surface p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md"><div className="flex items-start justify-between"><div className="flex size-10 items-center justify-center rounded-full bg-[#e7f0ec] text-prestiq-accent"><GraduationCap /></div><ArrowUpRight className="text-prestiq-muted transition group-hover:text-prestiq-ink" /></div><p className="mt-7 text-xs uppercase tracking-[.14em] text-prestiq-muted">{item.provider}</p><h3 className="mt-2 text-xl font-medium text-prestiq-ink">{item.name}</h3><div className="mt-8 flex items-end justify-between border-t border-prestiq-border pt-4"><div><p className="text-xs text-prestiq-muted">Award amount</p><p className="mt-1 font-medium text-prestiq-ink">{item.amount}</p></div><Badge variant="secondary" className="bg-[#eef4f0] text-prestiq-accent">{item.tag}</Badge></div><p className="mt-5 flex items-center gap-2 text-xs text-prestiq-muted"><Clock3 className="size-3.5" /> Deadline {item.deadline}</p></article>)}</div></div></section>
}

export function Benefits() {
  const benefits = [[Search, "Relevant by design", "Spend less time sorting through noise and more time understanding a good fit."], [FileText, "Clarity when it counts", "See key requirements in plain language before you commit to an application."], [Check, "Every deadline in view", "Keep your journey organized with a calm, focused view of what comes next."]]
  return <section id="about" className="bg-prestiq-ink py-24 text-white sm:py-32"><div className="mx-auto grid max-w-6xl gap-16 px-6 lg:grid-cols-[.8fr_1.2fr]"><div><p className="eyebrow text-[#a7c9bd]">Why PrestIQ</p><h2 className="mt-5 font-serif text-5xl leading-none sm:text-7xl">A little more clarity can change a lot.</h2></div><div className="grid gap-10 sm:grid-cols-3">{benefits.map(([Icon, title, text]) => <div key={title as string} className="border-t border-white/20 pt-5"><Icon className="size-5 text-[#a7c9bd]" /><h3 className="mt-8 text-lg font-medium">{title as string}</h3><p className="mt-3 text-sm leading-6 text-white/60">{text as string}</p></div>)}</div></div></section>
}

export function FinalCTA() { return <section className="px-6 py-24 sm:py-32"><div className="mx-auto flex max-w-4xl flex-col items-center rounded-3xl bg-[#e7f0ec] px-6 py-20 text-center sm:px-12"><Sparkles className="size-6 text-prestiq-accent" /><h2 className="mt-6 max-w-2xl font-serif text-5xl leading-none text-prestiq-ink sm:text-7xl">Your next opportunity starts here.</h2><p className="mt-6 max-w-md text-sm leading-6 text-prestiq-muted">Start building a scholarship journey that feels focused, informed, and yours.</p><Button asChild className="mt-9 rounded-full bg-prestiq-ink px-7 text-white hover:bg-prestiq-ink/90"><a href="#explore">Get Started <ArrowUpRight data-icon="inline-end" /></a></Button></div></section> }
