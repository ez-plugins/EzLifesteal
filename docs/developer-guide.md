---
title: Developer Guide
nav_order: 6
description: "Architecture overview, contribution workflow, and EzSeasons API integration"
---

# EzLifesteal Developer Guide
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

This guide helps contributors quickly understand how EzLifesteal is organized, how to add new functionality safely, and how to validate changes before opening a PR.

## Prerequisites

- Java 25+
- Maven 3.9+
- Paper 1.21+ test server (for manual plugin validation)

## Project Architecture

EzLifesteal is organized to keep command/listener entry points thin and business logic reusable.

- `command/` and `command/subcommand/` — parse input, permissions, and delegate work.
- `listener/` — event boundaries only; should delegate to services.
- `service/` — reusable business logic and orchestration.
- `storage/` / `repository/` — persistence adapters and data access.
- `config/` — typed wrappers around YAML sections and options.
- `integration/` / `hook/` — optional third-party integrations.
- `runtime/` — startup and wiring lifecycle.
- `gui/`, `hologram/`, `heart/`, `model/`, `util/` — domain-specific modules.

Use constructor injection throughout. Root service wiring should happen in plugin bootstrap/runtime classes.

## Contribution Workflow

1. **Create a branch** for your change.
2. **Map the feature** to the proper layer (command/listener -> service -> storage/config if needed).
3. **Implement minimal classes** with a single responsibility.
4. **Add or update tests** for behavior changes.
5. **Run local verification commands**.
6. **Document user-facing config/command changes** under `docs/`.

## Feature Implementation Patterns

### Adding a new command

- Create a dedicated command/subcommand class.
- Keep parsing and validation in command classes only.
- Delegate all meaningful logic to a service.

Recommended flow:

1. `Subcommand` class parses args and validates sender/context.
2. Service method performs domain logic.
3. Storage/repository persists any state asynchronously if needed.

### Adding a new listener behavior

- Keep listeners focused on event extraction and cancellation decisions.
- Move branching/domain logic into services for better testability.
- Avoid expensive operations on the main thread.

### Adding new configuration keys

- Add defaults in `src/main/resources/*.yml`.
- Add/update a dedicated config wrapper in `config/`.
- Ensure reload paths apply the new setting correctly.
- Update the relevant docs in `docs/config/`.

### Adding integrations

- Keep integration code in `integration/` or `hook/`.
- Wrap external plugin APIs behind interfaces where feasible.
- Ensure missing dependencies fail gracefully without blocking startup.

## Async and Performance Rules

- Never perform blocking DB/file operations on the server main thread.
- Use the project storage executor for persistence work.
- Cache hot-path data where appropriate.
- Keep event handlers lightweight.

## Testing Guidance

Prioritize fast, deterministic tests around services and utility logic.

- **Unit tests**: business logic in `service/`, `storage/` adapters, and `util/`.
- **Integration tests**: runtime wiring, listener registration, and command registration with MockBukkit.
- **Mocking**: isolate external integrations (Vault, PlaceholderAPI, bStats/EzSeasons).

Run before PR:

```bash
mvn test
mvn verify
```

Coverage reports are generated in:

- `target/site/jacoco/index.html`

## Manual Validation Checklist

Use a local Paper server and verify at least:

- Plugin starts cleanly with no stack traces.
- `/lifesteal reload` applies changed settings.
- Updated commands return expected messages/permissions behavior.
- Optional integrations still degrade gracefully when absent.

## Documentation Checklist for Contributors

When you change behavior, update docs in the same PR:

- Command behavior -> `docs/commands.md`
- Permission nodes -> `docs/permissions.md`
- Config keys -> `docs/configuration.md` + matching `docs/config/*.md`
- Contributor workflow/process -> `docs/developer-guide.md`

## Pull Request Expectations

A good PR should include:

- Clear description of what changed and why.
- Tests added/updated for changed logic.
- Notes on config migrations or new keys.
- Confirmation that `mvn test` and `mvn verify` were executed.

Keep classes small and cohesive. If a class grows too large, split it by responsibility.
