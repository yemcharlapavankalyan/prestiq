"""Clean HTML & document extractor with evidence preservation and noise reduction."""

import re
from typing import Dict, List, Optional
from bs4 import BeautifulSoup
from app.models.schemas import ExtractedScholarshipData


class CleanHtmlExtractor:
    """Extracts structured facts from HTML while stripping boilerplate navigation and scripts."""

    DEADLINE_PATTERNS = [
        re.compile(r"(?:application\s+deadline|deadline|apply\s+by|closes?|due\s+date)[:\s]+([A-Za-z]+ \d{1,2},? \d{4}|\d{4}-\d{2}-\d{2}|\d{1,2}/\d{1,2}/\d{4})", re.IGNORECASE),
        re.compile(r"([A-Za-z]+ \d{1,2},? \d{4})\s*(?:deadline)", re.IGNORECASE),
    ]

    AMOUNT_PATTERNS = [
        re.compile(r"(?:award|amount|grant|scholarship\s+value)[:\s]+(\$[\d,]+(?:\.\d{2})?|\d+[\d,]*\s*(?:USD|EUR|GBP))", re.IGNORECASE),
        re.compile(r"(\$[\d,]+(?:\s*-\s*\$[\d,]+)?)", re.IGNORECASE),
        re.compile(r"(full\s+tuition(?:\s+and\s+fees)?)", re.IGNORECASE),
    ]

    GPA_PATTERNS = [
        re.compile(r"(?:minimum|min\.?)\s*(?:cumulative\s*)?gpa\s*(?:of\s*)?(\d\.\d{1,2})", re.IGNORECASE),
        re.compile(r"gpa\s*(?:requirement|of)?\s*(\d\.\d{1,2})", re.IGNORECASE),
    ]

    def extract(self, html_content: str, source_url: str, default_provider_name: Optional[str] = None) -> ExtractedScholarshipData:
        """Parses HTML document into ExtractedScholarshipData preserving evidence snippets."""
        soup = BeautifulSoup(html_content, "html.parser")

        # 1. Strip noise elements
        for element in soup(["script", "style", "noscript", "nav", "footer", "header", "aside", "svg"]):
            element.decompose()

        evidence: Dict[str, str] = {}

        # 2. Title extraction
        title = None
        # Meta tags first
        meta_title = soup.find("meta", property="og:title") or soup.find("meta", attrs={"name": "title"})
        if meta_title and meta_title.get("content"):
            title = str(meta_title["content"]).strip()
            evidence["title"] = f"Meta tag: {title}"

        if not title:
            h1 = soup.find("h1")
            if h1 and h1.get_text(strip=True):
                title = h1.get_text(strip=True)
                evidence["title"] = f"H1 header: {title}"

        if not title and soup.title and soup.title.string:
            title = soup.title.string.strip()
            evidence["title"] = f"HTML Title: {title}"

        # 3. Provider Name
        provider_name = default_provider_name
        meta_provider = soup.find("meta", property="og:site_name")
        if meta_provider and meta_provider.get("content"):
            provider_name = str(meta_provider["content"]).strip()
            evidence["provider_name"] = f"og:site_name: {provider_name}"

        # 4. Description
        description = None
        meta_desc = soup.find("meta", property="og:description") or soup.find("meta", attrs={"name": "description"})
        if meta_desc and meta_desc.get("content"):
            description = str(meta_desc["content"]).strip()
            evidence["description"] = f"Meta description: {description}"

        clean_text = soup.get_text(separator="\n", strip=True)

        if not description:
            # Look for introductory paragraphs
            paragraphs = [p.get_text(strip=True) for p in soup.find_all("p") if len(p.get_text(strip=True)) > 50]
            if paragraphs:
                description = " ".join(paragraphs[:3])
                evidence["description"] = f"Body paragraphs: {description[:120]}..."

        # 5. Application Deadline
        raw_deadline = None
        for pattern in self.DEADLINE_PATTERNS:
            match = pattern.search(clean_text)
            if match:
                raw_deadline = match.group(1).strip()
                evidence["raw_deadline"] = match.group(0).strip()
                break

        # 6. Award Amount
        raw_amount = None
        for pattern in self.AMOUNT_PATTERNS:
            match = pattern.search(clean_text)
            if match:
                raw_amount = match.group(1).strip()
                evidence["raw_amount"] = match.group(0).strip()
                break

        # 7. Minimum GPA
        raw_min_gpa = None
        for pattern in self.GPA_PATTERNS:
            match = pattern.search(clean_text)
            if match:
                raw_min_gpa = match.group(1).strip()
                evidence["raw_min_gpa"] = match.group(0).strip()
                break

        # 8. Education levels heuristic detection
        raw_education_levels: List[str] = []
        lower_text = clean_text.lower()
        if "undergraduate" in lower_text or "bachelor" in lower_text or "college student" in lower_text:
            raw_education_levels.append("UNDERGRADUATE")
        if "postgraduate" in lower_text or "graduate student" in lower_text or "master" in lower_text:
            raw_education_levels.append("POSTGRADUATE")
        if "doctoral" in lower_text or "phd" in lower_text or "ph.d." in lower_text:
            raw_education_levels.append("DOCTORAL")
        if "high school" in lower_text:
            raw_education_levels.append("HIGH_SCHOOL")

        if raw_education_levels:
            evidence["raw_education_levels"] = f"Keyword matching: {', '.join(raw_education_levels)}"

        # 9. Application URL
        application_url = None
        for a_tag in soup.find_all("a", href=True):
            link_text = a_tag.get_text(strip=True).lower()
            if any(term in link_text for term in ("apply now", "application portal", "submit application")):
                application_url = a_tag["href"]
                evidence["application_url"] = f"Anchor text '{link_text}': {application_url}"
                break

        return ExtractedScholarshipData(
            title=title,
            provider_name=provider_name,
            official_website_url=source_url,
            application_url=application_url,
            description=description,
            raw_deadline=raw_deadline,
            raw_amount=raw_amount,
            raw_education_levels=raw_education_levels if raw_education_levels else None,
            raw_min_gpa=raw_min_gpa,
            evidence_snippets=evidence,
        )
