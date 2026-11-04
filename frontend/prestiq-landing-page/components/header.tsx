"use client"

import { useEffect, useState } from "react"
import { Menu, X } from "lucide-react"
import { Button } from "@/components/ui/button"

const links = [
  { label: "Explore Scholarships", href: "#explore" },
  { label: "How It Works", href: "#how-it-works" },
  { label: "About", href: "#about" },
]

export function Header() {
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50)
    handleScroll()
    window.addEventListener("scroll", handleScroll, { passive: true })
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  useEffect(() => {
    if (!menuOpen) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setMenuOpen(false)
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [menuOpen])

  const closeMenu = () => setMenuOpen(false)

  return (
    <header className="prestiq-header fixed inset-x-0 top-0 z-50 px-4 pt-4 sm:px-6 sm:pt-5" data-scrolled={scrolled}>
      <nav
        className="prestiq-nav mx-auto max-w-6xl rounded-[1.35rem] border border-transparent bg-transparent px-4 py-2.5 shadow-none backdrop-blur-0 sm:px-5"
        aria-label="Main navigation"
      >
        <div className="flex items-center justify-between gap-4">
          <a href="#" className="font-serif text-[1.45rem] tracking-tight text-prestiq-ink" onClick={closeMenu}>
            PrestIQ
          </a>

          <div className="hidden items-center gap-7 text-sm text-prestiq-muted md:flex">
            {links.map((link) => (
              <a key={link.href} href={link.href} className="prestiq-nav-link" onClick={closeMenu}>
                {link.label}
              </a>
            ))}
          </div>

          <div className="hidden items-center gap-1.5 sm:flex">
            <Button variant="ghost" asChild className="rounded-full px-4 text-prestiq-ink hover:bg-black/5">
              <a href="/login" onClick={closeMenu}>Log In</a>
            </Button>
            <Button asChild className="rounded-full bg-prestiq-ink px-5 text-white hover:bg-prestiq-ink/90">
              <a href="/register" onClick={closeMenu}>Get Started</a>
            </Button>
          </div>

          <Button
            variant="ghost"
            size="icon"
            className="size-10 rounded-full text-prestiq-ink hover:bg-black/5 sm:hidden"
            aria-label={menuOpen ? "Close navigation menu" : "Open navigation menu"}
            aria-expanded={menuOpen}
            aria-controls="mobile-navigation"
            onClick={() => setMenuOpen((open) => !open)}
          >
            {menuOpen ? <X data-icon="inline-start" /> : <Menu data-icon="inline-start" />}
          </Button>
        </div>

        <div
          id="mobile-navigation"
          className={`prestiq-mobile-menu sm:hidden ${menuOpen ? "prestiq-mobile-menu--open" : ""}`}
          aria-hidden={!menuOpen}
        >
          <div className="flex flex-col gap-1 border-t border-prestiq-border/70 pt-3">
            {links.map((link) => (
              <a key={link.href} href={link.href} className="prestiq-mobile-link" tabIndex={menuOpen ? 0 : -1} onClick={closeMenu}>
                {link.label}
              </a>
            ))}
            <div className="mt-2 flex gap-2 border-t border-prestiq-border/70 pt-3">
              <Button variant="ghost" asChild className="flex-1 rounded-full text-prestiq-ink hover:bg-black/5">
                <a href="/login" tabIndex={menuOpen ? 0 : -1} onClick={closeMenu}>Log In</a>
              </Button>
              <Button asChild className="flex-1 rounded-full bg-prestiq-ink text-white hover:bg-prestiq-ink/90">
                <a href="/register" tabIndex={menuOpen ? 0 : -1} onClick={closeMenu}>Get Started</a>
              </Button>
            </div>
          </div>
        </div>
      </nav>
    </header>
  )
}
