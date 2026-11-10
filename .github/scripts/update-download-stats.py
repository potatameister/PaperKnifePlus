#!/usr/bin/env python3
"""Rebuild the combined download-count JSON used by the README badge.

Sources
-------
* IzzyOnDroid: all-time sum of stats/basic/yearly/<year>.json
* GitHub Releases: sum of .apk asset download counts

The JSON is committed by .github/workflows/download-stats.yml. Nothing here
creates tags or releases, so F-Droid / IzzyOnDroid builds are never triggered.
"""

from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

PACKAGE = "com.paperknifeplus.app"
REPO = "potatameister/PaperKnifePlus"
IZZY_YEAR_URL = (
    "https://dlstats.izzyondroid.org/iod-stats-collector/"
    "stats/basic/yearly/{year}.json"
)
IZZY_FIRST_TRACKED_YEAR = 2024
OUTPUT = Path(__file__).resolve().parents[1] / "badges" / "downloads.json"


def fetch_json(url: str, token: str | None = None, attempts: int = 3):
    headers = {"User-Agent": "PaperKnifePlus-download-stats"}
    if token:
        headers.update(
            {
                "Authorization": f"Bearer {token}",
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
            }
        )
    for attempt in range(1, attempts + 1):
        try:
            request = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            if attempt == attempts:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == attempts:
                raise
        time.sleep(2 * attempt)
    raise RuntimeError(f"unreachable: {url}")


def izzy_downloads() -> int:
    """All-time downloads reported by the IzzyOnDroid stats collector."""
    total = 0
    current_year = datetime.now(timezone.utc).year
    for year in range(IZZY_FIRST_TRACKED_YEAR, current_year + 1):
        data = fetch_json(IZZY_YEAR_URL.format(year=year))
        count = int(data.get(PACKAGE, 0))
        total += count
        print(f"IzzyOnDroid {year}: {count:,}")
    return total


def github_downloads(token: str | None) -> int:
    """All-time .apk asset downloads across GitHub releases."""
    total = 0
    page = 1
    while True:
        url = f"https://api.github.com/repos/{REPO}/releases?per_page=100&page={page}"
        releases = fetch_json(url, token=token)
        if not releases:
            if page == 1:
                raise RuntimeError("GitHub API returned no releases")
            break
        for release in releases:
            for asset in release.get("assets", []):
                if asset.get("name", "").lower().endswith(".apk"):
                    total += int(asset.get("download_count", 0))
        if len(releases) < 100:
            break
        page += 1
    return total


def main() -> int:
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        print(
            "warning: GITHUB_TOKEN is not set, using unauthenticated GitHub API",
            file=sys.stderr,
        )

    izzy = izzy_downloads()
    github = github_downloads(token)
    total = izzy + github

    previous = {}
    if OUTPUT.exists():
        previous = json.loads(OUTPUT.read_text(encoding="utf-8"))

    unchanged = (
        previous.get("total") == total
        and previous.get("izzyondroid") == izzy
        and previous.get("github") == github
    )
    if unchanged:
        print(f"Totals unchanged ({total:,}); not rewriting {OUTPUT}.")
        return 0

    payload = {
        "total": total,
        "display": f"{total:,}",
        "izzyondroid": izzy,
        "github": github,
        "updated": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {OUTPUT}: {total:,} = {izzy:,} IzzyOnDroid + {github:,} GitHub")
    return 0


if __name__ == "__main__":
    sys.exit(main())
