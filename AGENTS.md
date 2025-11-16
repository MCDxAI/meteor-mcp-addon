# Agents & Contributors

## Primary Maintainer
- **GhostTypes** — Author of the Meteor MCP Addon, oversees feature planning, architectural direction, and release packaging.

## Supporting Automation
- **Codex** — Provides repository hygiene improvements and documentation upkeep during maintenance sessions.

## Coordination Notes
- Check `ai_reference/` first (especially `ai_reference/INDEX.md`) to see which reference repos are already cloned locally; the folder is git-ignored but usually the quickest source for answers.
- Sync major code or API changes with GhostTypes before merging to ensure compatibility with Meteor Client and MCP SDK updates.
- Capture notable decisions (protocol tweaks, dependency upgrades, StarScript additions) in pull request descriptions for historical context.
- When introducing new automation or build tooling, document usage in `README.md` and notify the primary maintainer for rollout approval.
