"""SSRF Protection and Network Destination Validator for Ingestion Service."""

import ipaddress
import socket
from typing import List, Optional
from urllib.parse import urlsplit
from app.core.exceptions import SSRFSecurityError


def is_ip_prohibited(ip: ipaddress.IPv4Address | ipaddress.IPv6Address) -> bool:
    """
    Checks if an IP address belongs to loopback, private, link-local,
    multicast, or reserved address ranges.
    """
    return (
        ip.is_loopback
        or ip.is_private
        or ip.is_link_local
        or ip.is_multicast
        or ip.is_reserved
        or ip.is_unspecified
    )


def is_domain_allowed(hostname: str, allowed_domains: List[str]) -> bool:
    """
    Checks whether a hostname matches or is an authorized subdomain of an allowed domain.
    E.g., 'scholarships.mit.edu' matches 'mit.edu'.
    """
    hostname = hostname.lower().strip()
    for allowed in allowed_domains:
        allowed = allowed.lower().strip()
        if hostname == allowed or hostname.endswith("." + allowed):
            return True
    return False


def validate_target_url(
    url: str,
    allowed_domains: Optional[List[str]] = None,
    resolve_dns: bool = True,
) -> str:
    """
    Validates a URL against strict SSRF constraints:
    1. Scheme must be HTTP or HTTPS.
    2. Embedded credentials (e.g., http://user:pass@host) are rejected.
    3. Hostname must be present.
    4. Hostname must belong to allowed_domains if specified.
    5. Hostname must NOT resolve to private, loopback, or cloud-metadata IPs.
    """
    if not url:
        raise SSRFSecurityError("Target URL cannot be empty.")

    try:
        parts = urlsplit(url)
    except Exception as e:
        raise SSRFSecurityError(f"Malformed URL: {str(e)}")

    # 1. Scheme check
    if parts.scheme.lower() not in ("http", "https"):
        raise SSRFSecurityError(f"Prohibited scheme '{parts.scheme}'. Only HTTP and HTTPS are permitted.")

    # 2. Embedded credentials check
    if parts.username or parts.password:
        raise SSRFSecurityError("URLs containing embedded username or password credentials are strictly prohibited.")

    hostname = parts.hostname
    if not hostname:
        raise SSRFSecurityError("Target URL does not contain a valid hostname.")

    # 3. Allowed domains check
    if allowed_domains and not is_domain_allowed(hostname, allowed_domains):
        raise SSRFSecurityError(
            f"Host '{hostname}' is not in the source's approved domain whitelist: {allowed_domains}."
        )

    # 4. DNS resolution & IP check
    if resolve_dns:
        # Check if hostname itself is directly an IP literal
        try:
            direct_ip = ipaddress.ip_address(hostname)
            if is_ip_prohibited(direct_ip):
                raise SSRFSecurityError(f"Destination IP '{direct_ip}' belongs to a prohibited/private network.")
            return url
        except ValueError:
            pass  # It's a DNS name

        # Resolve hostname via socket
        try:
            addr_info = socket.getaddrinfo(hostname, None, socket.AF_UNSPEC, socket.SOCK_STREAM)
            for item in addr_info:
                sockaddr = item[4]
                ip_str = sockaddr[0]
                resolved_ip = ipaddress.ip_address(ip_str)
                if is_ip_prohibited(resolved_ip):
                    raise SSRFSecurityError(
                        f"Hostname '{hostname}' resolved to prohibited IP '{resolved_ip}'. Access blocked."
                    )
        except socket.gaierror as e:
            raise SSRFSecurityError(f"DNS resolution failed for host '{hostname}': {str(e)}")

    return url
