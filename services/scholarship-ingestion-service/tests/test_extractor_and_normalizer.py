"""Tests for HTML extraction, noise stripping, and conservative normalization."""

from decimal import Decimal
import pytest
from app.models.schemas import ExtractedScholarshipData
from app.pipeline.extractor import CleanHtmlExtractor
from app.pipeline.normalizer import ConservativeNormalizer


SAMPLE_HTML = """
<!DOCTYPE html>
<html>
<head>
    <title>National STEM Leaders Scholarship 2027</title>
    <meta property="og:title" content="National STEM Leaders Scholarship" />
    <meta property="og:site_name" content="National Science Foundation" />
    <meta name="description" content="Prestigious leadership grant awarded to undergraduate STEM majors enrolled in accredited US colleges." />
    <script>alert("malicious script");</script>
    <style>body { color: red; }</style>
</head>
<body>
    <nav><a href="/home">Home</a><a href="/login">Login</a></nav>
    <main>
        <h1>National STEM Leaders Scholarship</h1>
        <p>This award provides $10,000 for high-achieving undergraduate students.</p>
        <p>Application Deadline: March 15, 2027</p>
        <p>Eligibility: Minimum cumulative GPA of 3.50 required.</p>
        <a href="https://stem.gov/apply" class="btn">Apply Now</a>
    </main>
    <footer><p>&copy; 2026 NSF. All rights reserved.</p></footer>
</body>
</html>
"""

MALFORMED_HTML = """
<div><h1>Broken HTML with no head or meta tags
<p>Deadline: 2027-08-01
<p>Award: $5,000 - $15,000
"""

SPARSE_HTML = """
<html><body>
    <h1>Minimal Announcement</h1>
    <p>General information about grants.</p>
</body></html>
"""


def test_html_extraction_strips_scripts_and_extracts_evidence():
    extractor = CleanHtmlExtractor()
    extracted = extractor.extract(SAMPLE_HTML, source_url="https://stem.gov/scholarships")

    assert extracted.title == "National STEM Leaders Scholarship"
    assert extracted.provider_name == "National Science Foundation"
    assert "March 15, 2027" in extracted.raw_deadline
    assert "$10,000" in extracted.raw_amount
    assert extracted.raw_min_gpa == "3.50"
    assert "UNDERGRADUATE" in (extracted.raw_education_levels or [])
    assert extracted.application_url == "https://stem.gov/apply"

    # Evidence verification
    assert "title" in extracted.evidence_snippets
    assert "raw_deadline" in extracted.evidence_snippets
    assert "raw_amount" in extracted.evidence_snippets


def test_sparse_html_leaves_unknown_fields_as_none():
    extractor = CleanHtmlExtractor()
    extracted = extractor.extract(SPARSE_HTML, source_url="https://example.gov")

    assert extracted.title == "Minimal Announcement"
    assert extracted.raw_deadline is None
    assert extracted.raw_amount is None
    assert extracted.raw_min_gpa is None
    assert extracted.application_url is None


def test_conservative_normalizer_dates_and_currencies():
    normalizer = ConservativeNormalizer()

    # Dates
    assert normalizer.normalize_date("March 15, 2027") == "2027-03-15"
    assert normalizer.normalize_date("2027-08-01") == "2027-08-01"
    assert normalizer.normalize_date("05/20/2027") == "2027-05-20"
    assert normalizer.normalize_date("Invalid date string") is None

    # Amounts
    min_amt, max_amt, curr, struct = normalizer.normalize_amount("$10,000")
    assert min_amt == Decimal("10000")
    assert max_amt == Decimal("10000")
    assert curr == "USD"
    assert struct == "FIXED_AMOUNT"

    # Range
    r_min, r_max, r_curr, r_struct = normalizer.normalize_amount("$5,000 - $15,000")
    assert r_min == Decimal("5000")
    assert r_max == Decimal("15000")
    assert r_struct == "RANGE"

    # Full tuition
    _, _, _, ft_struct = normalizer.normalize_amount("Full tuition and mandatory fees covered")
    assert ft_struct == "FULL_TUITION"

    # Slug
    slug = normalizer.generate_slug("National STEM Leaders Scholarship 2027!")
    assert slug == "national-stem-leaders-scholarship-2027"
