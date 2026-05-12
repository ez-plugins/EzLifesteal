---
title: PlaceholderAPI
nav_order: 2
parent: Integrations
description: "PlaceholderAPI integration — exposes heart placeholders and resolves PAPI tokens in messages"
---

# PlaceholderAPI
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (PAPI) is a standard placeholder layer used by hundreds of Bukkit plugins. When installed alongside EzLifesteal, it enables EzLifesteal to both **publish** its own placeholders and **consume** PAPI tokens in its own templates.

---

## What it enables

### Published placeholders

Other plugins (scoreboards, tab lists, chat formatters, etc.) can read live EzLifesteal data via PAPI placeholders:

| Placeholder | Value |
| --- | --- |
| `%ezlifesteal_hearts%` | Player's current heart count |

### Consumed placeholders

EzLifesteal processes PAPI tokens inside:

- Language message templates (player-facing messages)
- Kill streak command reward strings (the `command` field in streak rewards)

This lets you embed any PAPI placeholder inside a kill streak command or broadcast message, and the token will be resolved against the triggering player before execution.

---

## When absent

| Context | Fallback behaviour |
| --- | --- |
| Third-party plugin using `%ezlifesteal_hearts%` | Placeholder token passes through unresolved |
| Kill streak command reward containing `%some_papi_token%` | Token remains as literal text in the executed command |
| Internal message template containing a PAPI token | Token remains as literal text in the displayed message |

No errors are logged. EzLifesteal continues to work normally; only PAPI resolution is skipped.

---

## Setup

1. Download and install **PlaceholderAPI** on your server. No extra expansion is needed — EzLifesteal registers its own expansion at startup.
2. Use `%ezlifesteal_hearts%` in any PAPI-compatible plugin.
3. Optionally embed PAPI tokens in kill streak command rewards:

```yaml
streaks:
  - threshold: 3
    rewards:
      - type: command
        run-as: CONSOLE
        command: "broadcast &a%player_name% is on a 3-kill streak with %ezlifesteal_hearts% hearts!"
```

---

## Related pages

- [Kill Streaks](../features/kill-streaks) — reward type reference and command format
- [Kill Streak config (`lifesteal-killstreaks.yml`)](../config/killstreaks) — full reward-type list
