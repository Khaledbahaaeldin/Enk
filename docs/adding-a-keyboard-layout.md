# Adding a Keyboard Layout

Enk features an in-app keyboard designed to replicate the acoustic and tactile rhythm of a mechanical typewriter. Layouts are stored as declarative JSON files in application assets.

## Layout File Location

Keyboard layouts live under:
```
app/src/main/assets/keyboard/<locale>.json
```
For example, the default English QWERTY layout is located at `app/src/main/assets/keyboard/en.json`.

## JSON Schema

A layout file consists of metadata and page definitions:

```json
{
  "id": "en",
  "label": "English (US)",
  "direction": "ltr",
  "pages": {
    "letters": {
      "rows": [
        {
          "keys": [
            { "type": "char", "label": "q", "popup": ["1"] },
            { "type": "char", "label": "w", "popup": ["2"] }
          ]
        },
        {
          "keys": [
            { "type": "shift", "weight": 1.5 },
            { "type": "char", "label": "z" },
            { "type": "backspace", "weight": 1.5 }
          ]
        },
        {
          "keys": [
            { "type": "page", "target": "numbers", "label": "?123", "weight": 1.25 },
            { "type": "globe", "weight": 1.0 },
            { "type": "space", "weight": 4.5 },
            { "type": "enter", "weight": 1.5 }
          ]
        }
      ]
    },
    "numbers": { ... },
    "symbols": { ... }
  }
}
```

### Supported Key Types
- `"char"`: Character key. Accepts `label` (required), optional `popup` (array of alternative strings), and optional `shiftLabel`.
- `"shift"`: Shift/Caps Lock toggle key.
- `"backspace"`: Delete key (supports single press and accelerating hold-to-repeat).
- `"space"`: Space bar (supports press and drag for caret navigation).
- `"enter"`: Return/Newline key.
- `"globe"`: Quick switch to system IME.
- `"page"`: Switch between keyboard layers. Requires `target` (`"letters"`, `"numbers"`, or `"symbols"`) and `label`.
- `"spacer"`: Empty layout spacer. Takes `weight` (relative width).

All keys support an optional float `weight` (default `1.0`) controlling proportional width within the row.

## Validation & Loading

`app.enk.keyboard.LayoutLoader` loads and parses the layout:
1. Verifies that all required pages (`letters`, `numbers`, `symbols`) are defined.
2. Ensures all rows contain at least one valid key specification.
3. Calculates key horizontal weights to guarantee proportional alignment across screen sizes.
4. Validates popup arrays and character encoding.

Tests in `app.enk.keyboard.LayoutLoaderTest` and `KeyboardPropertyTest` enforce that all loaded layouts are non-empty, parse cleanly, and satisfy structural invariants.

## Why Enk Ships English Only in 1.0

Enk 1.0 prioritizes establishing the core mechanical typing model:
- Zero touch latency on down-events.
- Physical typewriter acoustic synchronization and spring animations.
- Space-bar cursor dragging calibrated to mechanical pitch.
- Seamless fallback to the system IME (via globe key or Setup preference) for multilingual input, complex scripts, and user-preferred IMEs.

Subsequent updates will expand native layouts with community-verified punctuation, diacritics, and localized acoustic sets.
