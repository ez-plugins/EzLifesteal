# AGENTS.md

## Purpose
This agent is responsible for generating professional, production-ready Java code for Minecraft plugins (Spigot/Paper).  
All code must be clean, modular, maintainable, and scalable.

---

## Core Principles

- Follow SOLID principles
- Ensure separation of concerns
- Prefer composition over inheritance
- Keep code readable and self-explanatory
- Avoid large, monolithic classes
- Write small, focused methods
- Design for extensibility and maintainability

---

## Project Structure

Organize code into clear layers:

```text
com.skyblockexp.ezlifesteal
├── command/                 # Command handlers
│   └── subcommand/          # Subcommands for main command handlers
├── listener/                # Event listeners
├── service/                 # Business logic
├── repository/              # Data access layer
├── config/                  # Configuration wrappers
├── model/                   # Data models
├── util/                    # Utility classes
├── compat/                  # Java/API compatibility adapters and lazy Bukkit wrappers
├── hook/                    # External integrations
├── runtime/                 # Runtime orchestration and lifecycle classes
├── hologram/                # Hologram settings & adapters
├── heart/                   # Heart models and registries
├── gui/                     # Inventory/menu UI
├── storage/                 # Persistence implementations (YAML/MySQL)
├── integration/             # Third-party integrations (Vault, PAPI, bStats)
└── EzLifestealPlugin.java   # Main plugin class
```

Complete project layout (top-level highlights):

- `src/main/java/com/skyblockexp/ezlifesteal/`
	- `EzLifestealPlugin.java` (plugin entrypoint)
	- `Bootstrap.java`, `Registry.java`, `PluginRuntimeServices.java`
	- `command/` (commands + `subcommand/` implementations)
	- `listener/`, `service/`, `storage/`, `runtime/`, `gui/`, `util/`, etc.
- `src/main/resources/`
	- `plugin.yml`, `config.yml`, `languages/`, various `*.yml` templates
- `src/test/java/` — unit and integration tests (uses MockBukkit, Mockito)
- `pom.xml`, `README.md`, `docs/` — build, documentation and guidelines

Best-case test coverage instructions

To achieve reliable, high coverage while keeping tests maintainable, follow this approach:

- **Aim:** Focus on unit-testing business logic in `service/`, `storage/` adapters, and `util/` helpers. Use lightweight integration tests for runtime wiring.
- **Tools:** Use `MockBukkit` for Bukkit integration tests, `Mockito`/`mockito-inline` for mocking, and `JUnit 5` for test framework. Use JaCoCo for coverage reports (already configured in `pom.xml`).
- **Strategy:**
	- Unit tests: isolate services from Bukkit by extracting logic into POJOs; mock Bukkit and external APIs only at the boundary.
	- Integration tests: use `MockBukkit` to boot a minimal runtime and assert lifecycle behavior (`Bootstrap`, command registration, listeners).
	- Storage tests: test both YAML-based and MySQL-backed implementations with temporary fixtures; prefer in-memory or ephemeral test data.
	- Command tests: keep command classes thin; assert parsing and delegation to service classes rather than full runtime behavior.
- **Coverage targets:** keep the project-level threshold aligned with current `pom.xml` (55% line coverage). For new code, aim for 80%+ on core services and models.
- **Test hygiene:**
	- Keep tests deterministic and fast; avoid long sleeps and real network I/O.
	- Mock external systems (Vault, PlaceholderAPI, bStats) in tests or provide test hooks to disable integrations.
	- Use parameterized tests and small fixtures to cover edge cases (bounds, nulls, invalid config).
- **Running and validating:**
	- Run tests and coverage with:

```bash
mvn test
mvn verify
```

	- Inspect JaCoCo report at `target/site/jacoco/index.html` and adjust tests to cover critical logic paths.

---

## Code Style

### Formatting
- Use 4 spaces indentation  
- Max line length: ~120 characters  
- Always use braces {}

### Naming
- Classes → PascalCase  
- Methods/variables → camelCase  
- Constants → UPPER_CASE  
- Packages → lowercase  

---

## Class Design

- One responsibility per class  
- Avoid unnecessary static usage  
- Use constructor-based dependency injection  
- Keep classes small and focused  
- Separate logic, data, and infrastructure clearly  

---

## Commands

- Each command must be a separate class  
- Do not place logic in command classes  
- Commands must only handle input and delegate to services  

---

## Listeners

- Keep listeners minimal  
- Do not include business logic  
- Delegate all logic to services  

---

## Services

- Contain all business logic  
- Must be reusable and testable  
- Should not depend heavily on Bukkit API  
- Handle validation, processing, and coordination  

---

## Configuration

- Use dedicated configuration wrapper classes  
- Avoid direct config access across the codebase  
- Centralize all configuration logic  

---

## Dependency Management

- Instantiate dependencies in the main plugin class  
- Pass dependencies via constructors  
- Avoid global state and static access  

---

## Error Handling

- Never ignore exceptions  
- Always log meaningful and descriptive errors  
- Fail gracefully where possible  

---

## Performance

- Avoid blocking the main thread  
- Use asynchronous operations when appropriate  
- Cache frequently used data  
- Minimize expensive operations in events  

---

## Hooks & Integrations

- Place all integrations in the hook package  
- Keep integrations isolated from core logic  
- Ensure integrations can be enabled/disabled safely  

---

## Documentation

- Document public classes and methods  
- Explain intent and reasoning, not just behavior  

---

## Testing Mindset

- Write loosely coupled code  
- Design with testability in mind  
- Avoid tight coupling to external APIs  

---

## What to Avoid

- Putting logic in the main plugin class  
- Static singleton abuse  
- Large, complex classes  
- Hardcoded values  
- Direct config usage everywhere  
- Mixing responsibilities between layers  

---

## Output Requirements

When generating code:

1. Always split into multiple classes  
2. Follow the defined project structure  
3. Include all necessary imports  
4. Ensure the code is compile-ready  
5. Keep logic clean and minimal per class  

---

## Goal

Produce code that is:

- Clean  
- Modular  
- Scalable  
- Maintainable  
- Production-ready  

---

## Project-specific contributor instructions

These are concise, actionable rules contributors and the agent should follow when adding or modifying code in this project.

- **Responsibilities:** Keep command/listener classes thin — delegate business logic to services under `service/`.
- **Class layout:** Each new feature should be split across a small set of classes: a configuration wrapper (`config/`), a service (`service/`), a repository/storage adapter (`storage/`), and a thin command or listener class that wires them together.
- **Java compatibility:** Put Java-version or API-compatibility logic under `compat/` and keep domain logic in regular feature packages. Prefer a small adapter in `compat/` over spreading reflection/version checks through commands, listeners, or services.
- **Dependency injection:** Use constructor injection only. Instantiate root services in `EzLifestealPlugin` or `Bootstrap` and pass down via constructors. Avoid static singletons.
- **Configuration:** Wrap `config.yml` sections in small POJOs under `config/`. Provide sensible defaults and a `reload()` entrypoint on runtime services.
- **Async rules:** Never perform blocking I/O on the main thread. Use the storage executor returned by `getStorageExecutor()` for DB or file operations.
- **Holograms & UI:** Treat hologram settings as runtime-managed state. Call `saveHologramSettings()` when updating and read via `getHologramSection(true)`.
- **Integrations:** Place external integration code under `integration/` or `hook/`. Wrap third-party APIs behind interfaces so tests can mock them easily.
- **Logging & errors:** Log meaningful errors and fail gracefully. Prefer throwing checked exceptions from storage adapters and catch/log at service boundaries.
- **Tests:** New features must include unit tests covering core logic. Integration tests (MockBukkit) are required for lifecycle, command registration, and major runtime interactions.

### Pull Request checklist

- Run `mvn test` and `mvn verify` locally; ensure no regressions.
- Add or update unit tests for any behavior change.
- Update `src/main/resources` templates if configuration keys change.
- Keep methods and classes small — if a class grows beyond ~200 lines, split it.
- Ensure the PR description explains the reasoning and mentions related tests.

### How the agent should generate code

- Always include the full package declaration and necessary imports.
- Favor small, focused classes with single responsibility.
- Provide a minimal unit test for each new service class demonstrating expected behavior.
- For commands, generate a thin command class that validates input and delegates to a service. Example pattern:

```java
public class GiveheartSubcommand implements Subcommand {
	private final HeartService heartService;
	public GiveheartSubcommand(HeartService heartService) { this.heartService = heartService; }
	@Override
	public void execute(CommandSender sender, String[] args) {
		// parse/validate args -> call heartService.give(...)
	}
}
```

### Formatting & style enforcement

- 4-space indentation, keep lines <=120 chars.
- Use descriptive names; avoid one-letter variables.
- Do not add inline comments unless clarifying non-obvious behavior.