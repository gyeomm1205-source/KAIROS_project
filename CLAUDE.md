# CLAUDE.md — Project Conventions for Claude

## Role

You are assisting the **AI part** developer on this project.
- Always use `ai` as the part identifier in branch names and merge request titles.

---

## Workflow Rule

**Every task must start with a Jira issue, then a branch.**
Never create a branch or start coding without a corresponding Jira issue key (e.g., `A506-XX`).

---

## Git Conventions

### Commit Message

```
[type/ISSUE-KEY] description in Korean or English

Example: [feat/A506-20] Add search result filtering
```

### Branch Name

```
ai/type/ISSUE-KEY

Example: ai/feat/A506-20
```

- Always lowercase English.
- Part is fixed to `ai`.

### Merge Request Title

```
[ai/type] [ISSUE-KEY] description

Example: [ai/feat] [A506-20] Add activity summarization model
```

### Git Flow

| Branch | Purpose |
|---|---|
| `main` | Production/deployment branch |
| `develop` | Integration branch for development |
| `ai/type/ISSUE-KEY` | Feature branches (always branch off `develop`) |

### Branch Types

| Type | Meaning |
|---|---|
| `feat` | New feature |
| `fix` | Bug or error fix |
| `chore` | Non-code changes (build config, file moves, package management) |
| `docs` | Documentation updates (README, WIKI, etc.) |
| `refactor` | Code restructuring without functional change |
| `test` | Test code additions or refactoring |
| `!hotfix` | Emergency production fix (highest priority) |

### Operating Rules

- All work starts with an issue creation, then branch creation.
- PRs require at least **1 approval** before merging.
- Force merge is only allowed in production emergencies; must be reported after.
- Delete branches that are no longer needed.
- All English text must be lowercase.
- Every commit, branch, or merge request must include a **Jira Issue Key** for GitLab–Jira integration.

### Issue Template

```
# ⚙️ ISSUE
- Describe the issue

# 📄 To-Do
- [ ] Subtask list
```

### Pull Request Template

```
# 📄 Work Description
- Description of changes

# ⚙️ ISSUE
- close A506-XX

# 📷 Screenshot
- Videos, images, logs, Swagger, Postman screenshots, etc.

# 💬 To Reviewers
- Notes or questions for reviewers

# 🔗 Reference
- Helpful links or code references
```

---

## Code Review — Pn Rules

When reviewing code, prefix comments with the appropriate priority level:

| Tag | Action Required | Meaning |
|---|---|---|
| **P1** | Request Changes | Critical — must be fixed. Potential for serious service errors. Author must fix or provide a convincing counter-argument. |
| **P2** | Request Changes | Strongly recommended. Author should accept or open a discussion if unable to comply. |
| **P3** | Comment | Prefer to reflect. If not, author must explain why or log a follow-up Jira ticket. |
| **P4** | Approve | Optional suggestion. Author may ignore without comment. |
| **P5** | Approve | Minor nitpick. Author may ignore entirely. |

**When reviewing code the user writes, apply these Pn labels in your feedback.**

---

## Jira Conventions

### Structure

- Use **Epic → Story** hierarchy only. No sub-tasks.
- Every Story must be linked to an Epic.
- Each developer creates their own Stories.

### Epic

- Represents a feature unit or a multi-day workstream.
- Share with the team before creating.

### Story Title

```
[ai] Description of work

Example: [ai] Implement activity summarization with LLM
```

### Story Description

```
Purpose: One-line summary

Tasks:
- [ ] Detailed to-do items

Done criteria: Matches spec, passes tests, approved in code review
```

### Story Points (Time-based)

| Points | Time |
|---|---|
| 1 | Within 1 hour |
| 2 | 2–3 hours |
| 3 | Half a day |
| 5 | Full day |
| 8 | 2 days |
| 13 | 3 days |

- Stories over **13 points must be split** into 3–5 point units.
- Stories of **21 points or more cannot be created**.

### Unplanned Work

- Create a Jira story immediately when unexpected work arises mid-sprint.
- Always add the **`unplanned`** label to distinguish from planned work.
- Assign story points after completion based on actual time spent.

### Sprint Closure

- Incomplete stories move to the next sprint with a one-line reason.
  - Examples: `[API change]`, `[priority shift]`, `[spec revision]`, `[time overrun]`
