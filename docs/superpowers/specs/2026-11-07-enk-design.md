# Enk — design

Date: 2026-11-07. Status: draft for approval. Working name: **Enk**.

## Intent and why

Enk is a free, open-source Android notes app that feels like a modern typewriter. It is inspired by Platen — Paper & Ink, an iPhone-only app by Jerry Munyoka Nkongolo (Rafikki Studio) that has no Android equivalent. There is also no maintained open-source typewriter-feel notes app on Android (the only F-Droid match, Post Writer, was last updated in 2019).

The author wants a FOSS release first, then a Play Store release where the FOSS feature set is roughly 80% of the Play version and the remaining ~20% is paid.

The app is for short, postcard-sized notes and thoughts. Its value is the feel of writing: a calm, tactile place to put a thought down.

## Goals

- A writing screen that feels like a physical machine: paper, ink, a visible marker, sound, a lamp.
- The signature interaction (below) is faithful to the approved prototype.
- Fast to open, fast to type, offline, private.
- A clean FOSS 1.0 that F-Droid can accept.
- An architecture that lets a private Play-only repo add paid packs and sync later without forking the core.

## What success looks like for 1.0

- Someone opens Enk, types a sentence, and the marker, slug, stamp, sound and bell feel like one physical machine, with no lag between key and stamp.
- The night lamp and its control panel feel as polished as the daytime screen.
- A user can file notes in the Box, find any note by search, add the widget, and export everything and re-import it without loss.
- The app is accepted on F-Droid: no network permission, no tracking, and every bundled asset has a recorded free license.
- The public repo is clean enough that another developer could add a pack without changing the core.

## Non-goals

- Long-form manuscript writing, rich text, or collaboration.
- Languages other than Latin text at 1.0 (no Arabic/RTL, no CJK). The fixed-width typewriter grid assumes Latin; other scripts are a later, separate design.
- Any network access, account, tracking, or ads in the FOSS build.
- Print-ready sheets and commissioned custom items (considered and not chosen).
- An AI co-writer or word prediction. The "component that writes alongside you" is the visible typing mechanism only.

## Anti-goals (a failure even if it technically works)

- Copying Platen's artwork, charm designs, capsule shapes, sounds, wording, or name. We reuse the idea (a marker on a rail under the text), never their assets.
- The marker or panel looking like pasted-on UI instead of one machined object. An earlier all-orange marker was rejected for exactly this.
- Anything that blocks the writer, such as a hard stop at the character cap.
- Assets in the core whose license is not FOSS-compatible.

## Constraints and decisions made by the author

- **Platform and stack:** Native Android, Kotlin, Jetpack Compose. Minimum Android 12 (API 31).
- **License and repos:** Two repos. Public FOSS core under Apache-2.0 with extension points. Private Play-only extension repo (billing, paid packs, Drive sync) is a later phase.
- **Assets:** The core ships only CC0 or CC-BY assets. Paid packs later use the author's own original art.
- **1.0 scope:** FOSS build only. No paid features and no Play Store release in 1.0.
- **Free build has the full writing experience:** paper, ink, sound, marker, lamp, unlimited notes. Paid features are packs and sync only.
- **Paid tier later (Play Store):** one-time purchase unlocking everything: marker styles and finishes, paper types and ink styles, charms and decorations, and Google Drive sync via the user's own Drive (no server run by the author). No subscription.
- **Privacy:** FOSS build has no network use and no analytics. A later Play build may add anonymous crash reports and purchase state only.
- **Name:** Enk. Availability is not yet verified on Google Play, F-Droid, or trademark databases.

## The signature interaction (approved)

The approved prototype is `spike-throwaway/marker-prototype-v2.html` (throwaway code, a visual and behavioral reference only; do not reuse it as product code).

**Layout.** Cream, grainy paper fills the screen under a dark top bar. A brushed-steel rail with end brackets crosses the screen at about 62% of its height. The paper feeds upward: the line currently being written always sits just above the rail, earlier lines above it, blank paper below the rail. The text uses a monospace typewriter face with slight per-character jitter (small vertical offset, rotation, and opacity variation) to look like imperfect ink.

**Caret.** A thin orange underline beneath the next character. It sits in clear space above the marker and never overlaps it.

**Marker.** A graphite carriage with steel collars top and bottom that match the rail, with fine grip grooves and a thin orange indicator strip as its only orange. It rides the rail directly beneath the caret column. There is no ruler or tick scale on the rail or panel. The marker moves one column per key with a short springy easing.

**Typing mechanism.** A small steel type-slug sits in a dark slot on the marker's top edge. Each keystroke pops the slug up briefly with the letter on its face, and the letter stamps onto the paper with a quick ink-bleed animation (scales down from slightly large, blur clears). Near the end of a line the marker sounds a bell.

**Scrub.** Dragging the marker moves the caret through the current page in proportion to finger position; on release it springs back to the caret's column.

**Night lamp.** Long-press the marker (about half a second) to toggle night writing. There is no lamp object. The marker's slot glows warm and a soft beam rises from it onto the text above; text near the beam is lit and the rest of the page stays dark. A recessed graphite control panel with a steel bezel slides up from the bottom with a spring, its three rows staggering in. It holds three faders: warmth, intensity, beam width. Each fader has a dark slot with a glowing fill in the beam's current color and a steel-collared graphite knob with a thin orange index line and grip grooves. Knobs lift and grow slightly while held and settle with a small spring. Faders move in 20 detents; each detent plays a short mechanical tick that rises in pitch with value and gives a light haptic pulse.

**Design language.** Teenage Engineering / Braun: matte soft-touch, tactile and physical (heavy soft shadows, real material highlights), precise, restrained, one orange accent. The look must stay consistent: rail, marker, panel, and knobs share the same steel-and-graphite material family.

**Behaviors the prototype did not settle.** Exact haptic patterns, final sound design, curved versus flat paper, tablet/landscape layout, reduced-motion and accessibility variants. These are left to the builder (see below).

## Writing behavior

- **Note size.** A page is about 3,000 characters. Writing past it starts a new page; nothing blocks the writer.
- **Corrections.** A user setting chooses strike-through (a crossed-out letter stays on the paper, as on a typewriter) or clean erase (the letter is removed). Default is clean erase.
- **Ink.** Ink darkness and ink style options exist in the free build at a basic level; richer ink styles and paper types are later paid packs.
- **Sounds.** Sampled mechanical typewriter sounds: key, space, return, bell, backspace. Four free sets (Classic, Antique, Soft, Keyboard) plus a mute option and separate volume for the bell. All samples must be CC0 or CC-BY. Sourcing is decided (see "Sound sources" below).
- **Dark/light.** The writing paper follows the resolved Mode (Light, Dark, or System) automatically; the manual lamp is a separate, user-toggled mode on top of it.

## Organizing, search, widget, data (1.0)

- **Box.** Notes live in a card-box metaphor: dividers the user names and reorders, notes filed behind them, a trash with recovery. The visual treatment is Enk's own, in the same material family as the writing screen. It deliberately does not copy Platen's flower tabs.
- **Search.** Full-text search across all notes, with results that open the note.
- **Widget.** A home-screen widget (paper styled) with a new-note button and a view of the most recent note.
- **Storage.** A local database on device. Export all notes to a zip of Markdown (or plain text) files and import from the same; this is the backup mechanism in 1.0.
- **Privacy.** No network permission in the FOSS build.

## Interface and themes (1.0)

Everything outside the writing screen uses a restrained glass interface (approved mockups in `docs/mockups/`, specified in the design system section 12): large titles that collapse as you scroll, a floating glass navigation bar (Write, Box, Find, Setup), opaque paper note cards, grouped settings lists, and spring-based motion. Glass appears only on floating chrome.

Themes recolor the interface only; the writing screen always keeps its paper, steel, and orange. There are five themes, each in light and dark: **Graphite** (the original Enk look and the default), **Apricot**, **Sky**, **Slate**, and **Mono**, plus **Dynamic**, which takes its colors from the Android wallpaper (Material You). A Mode setting chooses Light, Dark, or System (default System); this replaces the earlier idea of a separate "follow system theme" switch. The choice lives in Setup > Appearance.

## Later (not 1.0, but the architecture must allow it)

- Play Store release with a one-time unlock for all packs plus Google Drive sync.
- Paid packs: marker styles and finishes, paper types and ink styles, charms and decorations.
- Likely a pack/theme plug-in interface in the core so the private repo can contribute content without modifying the core.

## Sound sources (researched 2026-11-07)

The author asked that existing open-licensed sound packs be used where possible. Pages below were read on 2026-11-07 and each states CC0 (no attribution required, commercial use allowed). Recordings are long takes that must be cut into individual samples (a builder task). Audio quality and the space-key and backspace samples have not been listened to by the author or by me.

| Role | Source | License | Notes |
|---|---|---|---|
| Set 1, Classic (default) | keithpeter, "typewriter-olivetti-lettra-22.wav", Freesound #123344 | CC0 | 1:46, 44.1 kHz mono. Paper in, about 50 words typed, carriage return, bell at line end, paper out. Another open-source app (Nostos) already built a full typewriter pack from this one file, cutting 12 letter strikes, space, backspace and a 1.3 s return. That project dropped the bell as piercing, so Enk should keep the bell but make it soft and adjustable. |
| Set 2, Antique | monotraum, "old typewriter.wav", Freesound #434027 | CC0 | 1930s machine, 42.5 s, 24-bit stereo. Page does not list contents; the builder must audition and cut keystrokes. |
| Set 3, Soft | blukotek, "typewriter.wav", Freesound #246057 | CC0 | 25 s mono. Page does not list contents. May be softened further with a gentle low-pass and lower gain in code if needed. |
| Bell and return (shared fallback) | knufds, "Typewriter bell & carriage reset", Freesound #345955 | CC0 | 2.9 s, antique margin bell plus carriage return. |

Also usable (CC0, confirmed on each page): ramsamba's "Typewriter Bell" (Freesound #318687, 0.9 s) and "Typewriter Carriage Return" (#318686, 1.1 s), as alternatives to the shared bell and return.

Not usable without further checks: the Brother Charger 11 samples by Subtersonic (the author's site states no license for the pack, and the MIT project that bundles them states none for the recordings; deferred, see below), Dark Fantasy Studio's 40-sound itch.io pack (says copyright-free with no attribution, but says nothing about redistributing in open-source software), and the Zapsplat and SoundJay packs (attribution or site-terms licensing, not CC0).

**Decision (2026-11-07): ship four free sets in 1.0: Classic, Antique, Soft, and Keyboard.** The Keyboard set is OpenGameArt's "Keyboard Soundpack #1", a computer keyboard rather than a typewriter; the author downloads it and must confirm CC0 and the author's name on the page at download time (a search listing said CC0, but the page itself could not be opened when checked) and record both in `docs/sound-credits.md`. If that check fails, Keyboard is dropped and 1.0 ships with three.

**Deferred: the Brother Charger 11 set by Subtersonic.** The author wants it, but no license is stated anywhere checked, so it is not part of 1.0. It may be added later only after Subtersonic gives written permission covering open-source redistribution and the later paid Play release; until then the files stay out of the repo and the app.

Credits: all used sounds are credited in `docs/sound-credits.md` (draft), the repository's CREDITS file, and the in-app About screen.

Obligations and risks: keep a provenance file in the repo listing source URL, uploader and license for every sample, plus archived copies, since Freesound uploads can be removed. CC0 needs no attribution, but crediting the uploaders in the app's About screen is good practice. If a set proves unusable after listening, the fallback is to synthesize that set in code or record one.

## Decisions added after the builder review

Author decisions:
- **Input.** Use Android's real soft and hardware keyboards, so swipe typing, voice and paste work. Autocorrect is off by default, with a setting to turn it on. The author can tap an earlier line to edit it, and the paper feeds so that line sits above the rail.
- **Pages.** One note is an ordered set of pages of about 3,000 characters each. Scrub works within the current page. The writer moves between pages by swiping or tapping, and search opens the matching page.

Calls made by the writer of this document (change any of them):
- **Page breaks.** A break happens at a word boundary near 3,000 characters, never mid-word. Pasted text is split across pages the same way. Export writes one Markdown file per note with an invisible page marker between pages (`<!-- enk:page -->`).
- **Strike-through.** In strike-through mode a struck letter is stored with a flag. Struck letters count toward the page size because they occupy space on paper, are ignored by search, and are omitted from export by default.
- **Text layout.** Fixed column count, derived from screen width (about 28 on a typical phone), wrapping at word boundaries.
- **Extension interfaces.** The 1.0 core defines internal interfaces for marker finish, paper, ink, sound set and charm, with built-in implementations. No external pack loading in 1.0; the interfaces only have to avoid blocking it.
- **Box.** One level of dividers. A note belongs to exactly one divider or to an "Unfiled" inbox. Dividers can be reordered, and notes can be reordered within a divider. Trash is recoverable and auto-purges after 30 days. Trashed notes never appear in search or the widget.
- **Export and import.** A zip with one folder per divider and one Markdown file per note. Each file has front matter with a note ID, created and modified dates, and its divider. Trash is excluded. Import skips or replaces a note with the same ID, so re-importing is safe.
- **Widget.** Both a new-note button and a view of the most recent note.
- **Platform.** Android 12 (API 31) is the minimum for every build, F-Droid included. Phones are the primary target. Rendering uses Compose Canvas with no hard dependency on AGSL shaders (API 33). A "reduce effects" setting turns off heavy visuals on weaker devices.
- **Package ID.** Use a placeholder package ID until the name is verified.

## Left to the builder

Specific libraries, database schema, module structure, how packs are packaged and loaded, the audio engine, the animation implementation, sound sample sourcing, accessibility details (talkback, reduced motion, font scaling), tablet/landscape behavior, icon and branding, test strategy, and the F-Droid build setup. The builder should treat the spike only as a behavior and look reference.

## Open items for the author

- Verify the name "Enk" on Google Play, F-Droid, and trademark databases before release.
- Confirm that using Platen only as inspiration (idea, not assets) is acceptable after a quick legal read before the Play Store launch; this document is not legal advice.
