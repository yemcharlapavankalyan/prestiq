const providers = [
  "Reliance Foundation",
  "Tata Trusts",
  "HDFC Bank Parivartan",
  "Foundation For Excellence",
  "Sitaram Jindal Foundation",
  "Buddy4Study",
]

function ProviderRow({ reverse = false }: { reverse?: boolean }) {
  return (
    <div className="provider-marquee__viewport" aria-hidden="true">
      <div className={`provider-marquee__track${reverse ? " provider-marquee__track--reverse" : ""}`}>
        {[...providers, ...providers].map((provider, index) => (
          <span className="provider-marquee__name" key={`${provider}-${index}`}>
            {provider}
          </span>
        ))}
      </div>
    </div>
  )
}

export function ProviderMarquee() {
  return (
    <section
      aria-labelledby="provider-marquee-heading"
      className="border-y border-prestiq-border bg-prestiq-surface py-12 sm:py-16"
    >
      <div className="mx-auto max-w-6xl px-6">
        <h2 id="provider-marquee-heading" className="eyebrow text-center">
          Explore opportunities from scholarship providers
        </h2>
      </div>
      <p className="sr-only">Temporary text placeholders for scholarship providers; no partnership or endorsement is implied.</p>
      <div className="provider-marquee mt-9 flex flex-col gap-5" aria-label="Scholarship provider names">
        <ProviderRow />
        <ProviderRow reverse />
      </div>
    </section>
  )
}
