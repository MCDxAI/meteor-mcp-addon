# Changelog

All notable changes to the Meteor MCP addon are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Compare links are at the bottom of this file.

## [Unreleased]

### Changed

- fabric.mod.json now requires meteor-client `>=26.2-0`, which matches Meteor's
  distributed `26.2-N` pre-release versioning.
- README version references updated to Minecraft 26.2.

## [0.4.0] - 2026-08-14

### Migration

- Updated to Minecraft 26.2, Meteor Client 26.2-SNAPSHOT, fabric-loader 0.19.3
- Gradle 9.6.1, Fabric Loom 1.17-SNAPSHOT
- Addon version 0.4.0

### Ported

- Adapted the MCP server screens (AI settings, add/edit server, server list,
  tools) to the Meteor 26.2 APIs

### Docs

- CLAUDE.md and GEMINI.md refreshed; badges updated for 26.2

## [0.3.0] - 2026-04-29

### Migration

- Updated to Minecraft 26.1.2, fabric-loader 0.19.2
- Java/release target 21 → 25, Gradle 9.4.1, Loom 1.16
- Dropped yarn mappings; meteor and fabric-loader moved to plain `implementation`
- `modInclude` refactored into a Configuration extending `implementation` + `include`
- Added foojay toolchain resolver and Fabric Snapshots maven repo

### Dependencies

- MCP SDK 0.14.1 → 1.1.1 (dropped standalone mcp/mcp-json modules)
- StarScript 0.2.3 → 0.2.5

### CI

- Unified `release.yml` workflow — JDK 25, timestamped dev tags, PR build
  validation, auto-generated release notes on stable releases

## [0.2.0] - 2025-12-15

### Changed

- Updated to Minecraft 1.21.11 and Gradle 9.2.0
- Repository metadata moved to the MCDxAI organization

## [0.1.0] - 2025-10-20

- Initial release.

[Unreleased]: https://github.com/MCDxAI/meteor-mcp-addon/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/MCDxAI/meteor-mcp-addon/releases/tag/v0.4.0
[0.3.0]: https://github.com/MCDxAI/meteor-mcp-addon/releases/tag/v0.3.0
[0.2.0]: https://github.com/MCDxAI/meteor-mcp-addon/releases/tag/v0.2.0
[0.1.0]: https://github.com/MCDxAI/meteor-mcp-addon/releases/tag/v0.1.0
