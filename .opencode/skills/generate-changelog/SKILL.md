---
name: generate-changelog
description: Generates a structured changelog entry from context/PROGRESS.md and recent git commits. Use when the user asks to generate a changelog, release notes, or summarise what changed in the current increment or release.
---

# Generate Changelog

Produce a structured changelog entry for the current increment or release by combining `context/PROGRESS.md` with recent git history.

## Steps

1. Read `context/PROGRESS.md` — extract completed increments, files changed, and any noted decisions or deviations.
2. Run `git log --oneline --no-merges` to get recent commit messages since the last tag or a specified range.
3. Categorise changes into: **Added**, **Changed**, **Fixed**, **Removed** (follow [Keep a Changelog](https://keepachangelog.com) conventions).
4. Write the entry to `CHANGELOG.md` under a new `## [Unreleased]` section, or under a version heading if the user supplies one.

> Project note: this repo keeps every change under `[Unreleased]` in the root `CHANGELOG.md`. If an `[Unreleased]` section already exists, append to it rather than creating a duplicate.

## Output Format

```markdown
## [Unreleased] — YYYY-MM-DD

### Added
- [What was introduced]

### Changed
- [What was modified in existing behaviour]

### Fixed
- [Bugs resolved]

### Removed
- [Features or fields removed]
```

## Rules

- One bullet per logical change — not one per commit.
- Write from the **caller's perspective**: what changed in behaviour, not what files were edited.
- Omit refactoring-only increments unless they affect public API or performance.
- If `context/PROGRESS.md` is missing, fall back to git log only and note the limitation.
