"use client"

import { useEffect, useRef, useState } from "react"
import { ArrowUpRight, Check, Clock3, GraduationCap, UserRound } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Progress } from "@/components/ui/progress"

export function DashboardPreview() {
  const previewRef = useRef<HTMLDivElement>(null)
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const element = previewRef.current
    if (!element) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true)
          observer.disconnect()
        }
      },
      { threshold: 0.2 },
    )

    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  return (
    <section className="relative px-6 py-16 sm:py-24" aria-labelledby="workspace-preview-title">
      <div ref={previewRef} className={`mx-auto max-w-5xl workspace-preview ${isVisible ? "workspace-preview--visible" : ""}`}>
        <div className="overflow-hidden rounded-2xl border border-prestiq-border bg-prestiq-surface shadow-[0_24px_80px_rgba(41,38,36,.12)]">
          <div className="flex items-center justify-between border-b border-prestiq-border px-5 py-4 sm:px-7">
            <div className="flex items-center gap-3">
              <div className="flex size-8 items-center justify-center rounded-full bg-[#e7f0ec] text-prestiq-accent" aria-hidden="true"><GraduationCap className="size-4" /></div>
              <span id="workspace-preview-title" className="text-sm font-medium text-prestiq-ink">Your scholarship workspace</span>
            </div>
            <div className="flex items-center gap-2 text-xs text-prestiq-muted"><span className="size-2 rounded-full bg-prestiq-accent" aria-hidden="true" /> Profile active</div>
          </div>

          <div className="grid gap-5 p-5 sm:grid-cols-[1.6fr_1fr] sm:p-8">
            <article className="scholarship-preview-card rounded-xl border border-prestiq-border bg-prestiq-bg p-5 transition-transform duration-300 hover:-translate-y-1 hover:shadow-[0_16px_36px_rgba(41,38,36,.10)] sm:p-7">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs uppercase tracking-[.16em] text-prestiq-muted">Illustrative preview</p>
                  <h2 className="mt-3 text-2xl font-medium leading-tight text-prestiq-ink sm:text-3xl">Merit Pathway Award</h2>
                  <p className="mt-2 text-sm text-prestiq-muted">Example match based on your profile</p>
                </div>
                <Badge variant="secondary" className="shrink-0 bg-[#e7f0ec] text-prestiq-accent">Good match</Badge>
              </div>

              <div className="mt-7 grid grid-cols-2 gap-4 border-y border-prestiq-border py-5 sm:gap-6">
                <div><p className="text-xs text-prestiq-muted">Award amount</p><p className="mt-1 text-lg font-medium text-prestiq-ink">Up to ₹75,000</p></div>
                <div><p className="text-xs text-prestiq-muted">Deadline</p><p className="mt-1 flex items-center gap-1.5 text-lg font-medium text-prestiq-ink"><Clock3 className="size-4 text-prestiq-accent" aria-hidden="true" /> 12 Aug 2025</p></div>
              </div>

              <div className="mt-5"><p className="text-xs text-prestiq-muted">Eligibility summary</p><p className="mt-2 max-w-xl text-sm leading-6 text-prestiq-ink">For students with a strong academic profile and demonstrated financial need. Review the official criteria before applying.</p></div>
              <Button variant="outline" className="mt-6 gap-2 border-prestiq-border bg-transparent text-prestiq-ink hover:bg-prestiq-surface">View scholarship details <ArrowUpRight data-icon="inline-end" /></Button>
            </article>

            <aside className="rounded-xl border border-prestiq-border bg-prestiq-bg p-5 sm:p-6" aria-labelledby="profile-completion-title">
              <div className="flex items-start justify-between gap-3"><div><p className="text-xs uppercase tracking-[.16em] text-prestiq-muted">Profile readiness</p><h2 id="profile-completion-title" className="mt-2 text-xl font-medium text-prestiq-ink">Complete your profile</h2></div><UserRound className="mt-1 size-5 text-prestiq-accent" aria-hidden="true" /></div>
              <div className="mt-7 flex items-end justify-between"><span className="text-sm text-prestiq-muted">Profile completion</span><span className="text-2xl font-medium text-prestiq-ink">72%</span></div>
              <Progress value={isVisible ? 72 : 0} className="profile-progress mt-3 h-2" aria-label="Profile completion: 72 percent" />
              <ul className="mt-6 flex flex-col gap-3 text-sm text-prestiq-ink">
                {["Academic history", "Goals and interests", "Supporting documents"].map((item, index) => <li key={item} className="flex items-center gap-3"><span className={`flex size-5 shrink-0 items-center justify-center rounded-full ${index < 2 ? "bg-[#e7f0ec] text-prestiq-accent" : "border border-prestiq-border text-prestiq-muted"}`}>{index < 2 ? <Check className="size-3" aria-hidden="true" /> : <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />}</span>{item}<span className="sr-only">{index < 2 ? " complete" : " incomplete"}</span></li>)}
              </ul>
            </aside>
          </div>
        </div>
      </div>
    </section>
  )
}
