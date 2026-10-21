"""Deduplication and change detection across canonical URLs and content snapshots."""

import hashlib
from typing import Optional, Tuple
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit
from app.models.db_models import CandidateScholarship
from app.models.enums import CandidateDeduplicationStatus
from app.models.schemas import NormalizedScholarshipData


class Deduplicator:
    """Manages canonical URL normalization, cryptographic hashing, and change detection."""

    TRACKING_PARAMS = {
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "msclkid", "mc_eid", "ref", "source"
    }

    def canonicalize_url(self, raw_url: str) -> str:
        """
        Normalizes a URL to a stable canonical format:
        - Lowercases scheme and host
        - Strips query tracking parameters
        - Strips fragment
        - Removes redundant trailing slash
        """
        parts = urlsplit(raw_url.strip())
        scheme = parts.scheme.lower()
        netloc = parts.netloc.lower()
        path = parts.path

        if path.endswith("/") and len(path) > 1:
            path = path[:-1]

        # Filter out tracking query params
        query_pairs = parse_qsl(parts.query)
        clean_pairs = sorted([(k, v) for k, v in query_pairs if k.lower() not in self.TRACKING_PARAMS])
        clean_query = urlencode(clean_pairs)

        return urlunsplit((scheme, netloc, path, clean_query, ""))

    def compute_content_hash(self, content: str) -> str:
        """Generates SHA-256 hash of text content."""
        return hashlib.sha256(content.encode("utf-8")).hexdigest()

    def check_duplicate(
        self,
        canonical_url: str,
        content_hash: str,
        normalized: NormalizedScholarshipData,
        existing_candidate: Optional[CandidateScholarship],
    ) -> Tuple[CandidateDeduplicationStatus, Optional[str]]:
        """
        Detects exact duplicates vs content modifications vs new opportunities.
        Returns (status, conflict_summary).
        """
        if not existing_candidate:
            return CandidateDeduplicationStatus.NEW, None

        # Check if content is identical
        if existing_candidate.content_hash == content_hash:
            return CandidateDeduplicationStatus.EXACT_DUPLICATE, None

        # Content has changed - detect what shifted
        previous_norm = existing_candidate.normalized_data
        conflicts = []

        prev_deadline = previous_norm.get("application_deadline")
        new_deadline = normalized.application_deadline
        if prev_deadline and new_deadline and prev_deadline != new_deadline:
            conflicts.append(f"Deadline changed from '{prev_deadline}' to '{new_deadline}'")

        prev_min = previous_norm.get("award_amount_min")
        new_min = str(normalized.award_amount_min) if normalized.award_amount_min else None
        if prev_min != new_min:
            conflicts.append(f"Award amount min changed from '{prev_min}' to '{new_min}'")

        conflict_summary = "; ".join(conflicts) if conflicts else "Content body updated without critical field changes"
        return CandidateDeduplicationStatus.CONTENT_CHANGED, conflict_summary
