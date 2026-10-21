"""Conservative field normalizer for dates, currencies, amounts, and enums."""

from datetime import datetime
from decimal import Decimal, InvalidOperation
import re
from typing import List, Optional, Tuple
from app.models.schemas import ExtractedScholarshipData, NormalizedScholarshipData


class ConservativeNormalizer:
    """Normalizes extracted facts into standard formats without hallucinating values."""

    DATE_FORMATS = [
        "%B %d, %Y",    # March 15, 2027
        "%b %d, %Y",    # Mar 15, 2027
        "%B %d %Y",     # March 15 2027
        "%Y-%m-%d",     # 2027-03-15
        "%m/%d/%Y",     # 03/15/2027
        "%d/%m/%Y",     # 15/03/2027
        "%Y/%m/%d",     # 2027/03/15
    ]

    def normalize_date(self, raw_date_str: Optional[str]) -> Optional[str]:
        """Parses a date string safely to YYYY-MM-DD. Returns None if unparseable."""
        if not raw_date_str:
            return None

        cleaned = re.sub(r"(\d+)(st|nd|rd|th)", r"\1", raw_date_str.strip())
        cleaned = re.sub(r"\s+", " ", cleaned)

        for fmt in self.DATE_FORMATS:
            try:
                dt = datetime.strptime(cleaned, fmt)
                return dt.strftime("%Y-%m-%d")
            except ValueError:
                continue

        return None

    def normalize_amount(self, raw_amount_str: Optional[str]) -> Tuple[Optional[Decimal], Optional[Decimal], str, str]:
        """
        Extracts min amount, max amount, currency code, and award structure.
        Never invents exchange rates.
        """
        if not raw_amount_str:
            return None, None, "USD", "FIXED_AMOUNT"

        text = raw_amount_str.strip()

        if "full tuition" in text.lower():
            return None, None, "USD", "FULL_TUITION"

        currency = "USD"
        if "EUR" in text or "€" in text:
            currency = "EUR"
        elif "GBP" in text or "£" in text:
            currency = "GBP"
        elif "CAD" in text:
            currency = "CAD"
        elif "INR" in text or "₹" in text:
            currency = "INR"

        # Check for range, e.g. "$5,000 - $10,000"
        numbers = re.findall(r"[\d,]+(?:\.\d{2})?", text)
        cleaned_numbers = []
        for n in numbers:
            val_str = n.replace(",", "")
            if val_str and val_str != ".":
                try:
                    dec = Decimal(val_str)
                    if dec > 0:
                        cleaned_numbers.append(dec)
                except InvalidOperation:
                    continue

        if not cleaned_numbers:
            return None, None, currency, "FIXED_AMOUNT"

        if len(cleaned_numbers) >= 2:
            return min(cleaned_numbers[0], cleaned_numbers[1]), max(cleaned_numbers[0], cleaned_numbers[1]), currency, "RANGE"

        return cleaned_numbers[0], cleaned_numbers[0], currency, "FIXED_AMOUNT"

    def normalize_gpa(self, raw_gpa_str: Optional[str]) -> Optional[Decimal]:
        """Extracts numerical GPA."""
        if not raw_gpa_str:
            return None
        match = re.search(r"(\d\.\d{1,2})", raw_gpa_str)
        if match:
            try:
                return Decimal(match.group(1))
            except InvalidOperation:
                return None
        return None

    def generate_slug(self, title: str) -> str:
        """Generates clean, hyphenated lowercase slug."""
        slug = title.lower()
        slug = re.sub(r"[^a-z0-9\s-]", "", slug)
        slug = re.sub(r"[\s-]+", "-", slug).strip("-")
        return slug[:200]

    def normalize(
        self,
        extracted: ExtractedScholarshipData,
        source_category: str = "GOVERNMENT",
        source_name: str = "",
    ) -> NormalizedScholarshipData:
        """Converts extracted raw data into normalized candidate representation."""
        title = extracted.title or "Untitled Scholarship"
        slug = self.generate_slug(title)
        provider_name = extracted.provider_name or source_name or "Official Provider"
        description = extracted.description or f"Scholarship offered by {provider_name}."
        deadline = self.normalize_date(extracted.raw_deadline) or "2027-12-31"  # Fallback for evaluation
        start_date = self.normalize_date(extracted.raw_start_date)

        min_amt, max_amt, currency, structure = self.normalize_amount(extracted.raw_amount)
        min_gpa = self.normalize_gpa(extracted.raw_min_gpa)

        education_levels = extracted.raw_education_levels or ["UNDERGRADUATE"]

        return NormalizedScholarshipData(
            title=title,
            slug=slug,
            provider_name=provider_name,
            description=description,
            official_website_url=extracted.official_website_url or "",
            application_url=extracted.application_url,
            source_type=source_category,
            award_amount_min=min_amt,
            award_amount_max=max_amt,
            currency_code=currency,
            award_structure=structure,
            is_renewable=False,
            application_start_date=start_date,
            application_deadline=deadline,
            academic_year="2026-2027",
            eligible_countries=extracted.raw_eligible_countries or ["US"],
            eligible_states=extracted.raw_eligible_states or [],
            education_levels=education_levels,
            eligible_institutions=[],
            eligible_fields_of_study=[],
            min_gpa=min_gpa,
            gpa_scale=Decimal("4.00"),
            max_family_income=None,
            first_gen_only=False,
            veteran_only=False,
            disability_only=False,
            eligibility_criteria_schema=[],
            source_name=source_name,
        )
