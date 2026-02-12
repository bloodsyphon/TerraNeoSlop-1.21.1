
---

## `CLAUDE.md` (for Claude Code)

```md
# Project Memory — Terra NeoForge Port (MC 1.21.1)

This file defines how Claude Code should operate in this repository.

## Goal
Modify the Terra Minecraft mod to support NeoForge.

## Hard Constraints (DO NOT VIOLATE)
- Target Minecraft version: 1.21.1
  - DO NOT use 1.21.10
  - DO NOT upgrade versions unless explicitly instructed
- Target Terra branch:
  - https://github.com/PolyhedralDev/Terra/tree/dev/1.21.1
- Terra requires Architectury Loom
  - DO NOT remove or replace Architectury Loom
- Scope: NeoForge support (loader/platform support), not a rewrite

If you discover a conflict with any constraint, STOP and log it as a BLOCKER.

When looking up documentation, ensure it is for the correct version. We are working with older minecraft, loom, and terra versions. 

---
## Helpful notes and references
Please reference this folder for a template for how to use Architectury with neoforge: C:\Terra\Terra\Examples\example_mod-1.21.1-neoforge-only-template
Please reference this folder for a template for a real world example of how to use Architectury with neoforge: C:\Terra\Terra\Examples\liteminer-1.21.1

After I test a build, the logs will be in C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs
Names: latest.log and debug.log
These logs should currently represent the last build attempt

## Mandatory Workflow (NON-NEGOTIABLE)

### Before doing anything
1) Read ai_notes.md in full.
2) Determine:
   - What has already been attempted
   - What failed and why
   - What is currently blocked
   - What the next intended step is (if stated)

Rules:
- Do NOT repeat previously-failed approaches unless you explicitly justify the retry in the log.
- Do NOT “clean up” unrelated code.

### While working
- Prefer small, reversible changes.
- Prefer Gradle/Loom/loader glue changes before code changes.
- Keep edits scoped to the minimum needed for progress.

### After every action (including failures)
- Append a new entry to ai_notes.md.
- Every attempt must be logged, even if it fails quickly.

---

## Logging Rules (CRITICAL)

All work must be recorded in ai_notes.md.

- Append-only (do not rewrite history).
- Use factual language: record what was done and what happened.
- Each entry must follow the exact format defined below.

IMPORTANT:
The “format defined below” is a generic template.
It is not a real log entry.
Replace the placeholders with your actual details each time.

---

## Log Entry Template (COPY/PASTE THIS EXACT STRUCTURE)

## <YYYY-MM-DD HH:MM TZ> — <Short descriptive title>

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: <OS, JDK version, Gradle version, etc. if known>

### Goal
<One sentence describing the outcome you are trying to achieve in this attempt>

### Hypothesis
<What you believe will work, and why>

### Actions Taken
- <Bullet list of concrete steps performed>

### Files Touched
- <path/to/file> — <brief note: created/edited/inspected>
- <path/to/file> — <brief note>

### Commands Run
```text
<paste exact commands here>
