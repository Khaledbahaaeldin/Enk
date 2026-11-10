# Enk Box, Find & Setup Device QA Checklist (Batch 8)

Manual verification checklist for Tasks 21, 22, and 23 on Android virtual devices (AVDs) and physical hardware.

---

## Device & Environment Information
- **Device Model / AVD Profile**: Pixel 5 / Pixel 8 / Pixel 9
- **Android OS Version / API Level**: API 31 (Android 12) through API 35 (Android 15)
- **Navigation Mode**: Gesture Navigation / 3-Button Navigation
- **Theme & Mode Tested**: Graphite / Apricot / Sky / Slate / Mono / Dynamic (Light & Dark)
- **Build Type**: `debug` / `release`
- **Tester**:
- **Date**:

---

## 1. The Box Screen (Task 21)

- [ ] **1.1 Header & Meta Count**
  - Verify large title displays "The Box" and meta count shows "X notes" (or "0 notes", "1 note") in Courier Prime.
  - As list scrolls down, verify Large Title collapses into Compact Bar at the top.
- [ ] **1.2 Divider Strip**
  - Verify horizontal strip contains "Unfiled", custom user dividers, and a "+" button.
  - Tapping a divider filters the note list to notes in that divider.
  - Active divider shows lens indicator; switching dividers triggers card entrance animation (14 dp translate up + fade).
- [ ] **1.3 Ghost Card ("New note")**
  - Verify first item in list is the dashed/hair border ghost card.
  - Tapping ghost card creates a new note in the active divider and navigates to the Writer screen.
- [ ] **1.4 Note Cards**
  - Note cards show cream paper `#F2ECDF`, rounded corners (14 dp), grain texture, Courier Prime text, and subtle layered shadows.
  - Tapping a note card navigates to that note in the Writer.
  - Holding a note card for 420 ms triggers a 1.03 scale lift, darkens the scrim, and opens the thick glass context menu 10 dp below the card.
- [ ] **1.5 Context Menu**
  - Displays "Move to divider" and "Move to trash".
  - Tapping outside or system back dismisses menu and lowers card.
  - "Move to divider" opens bottom sheet with destination dividers.
  - "Move to trash" moves note to trash, decrements active count, and updates trash count.
- [ ] **1.6 New Divider Sheet**
  - Tapping "+" opens 320 dp bottom sheet.
  - Validates names: rejects blank, duplicate (case-insensitive), or >40 characters.
  - Dragging down past midpoint dismisses sheet.
- [ ] **1.7 Trash Screen & Delete Alert**
  - Trash shows struck-through note titles, "X days left" label, and paper-pill "Restore" button.
  - "Delete all forever" button opens thick glass alert dialog.
  - Confirming delete forever permanently erases all trashed notes.

---

## 2. Find / Search Screen (Task 22)

- [ ] **2.1 Header & Meta Count**
  - Title shows "Find". When query is empty, meta shows "all notes".
  - With search hits, meta shows "1 match" or "X matches".
- [ ] **2.2 Pinned Search Field**
  - 44 dp height, 22 dp corner radius, 18 dp magnifier icon in `t2`, placeholder "Search your notes".
  - Caret color matches active theme accent (`ui`).
  - Search field is pinned at top; scrolling result list does not scroll search field off screen.
- [ ] **2.3 Search Debounce & Query Execution**
  - Typing rapidly debounces for 200 ms before executing FTS search.
  - Blanking search field immediately clears results.
- [ ] **2.4 Empty & No-Result States**
  - Empty search field shows centered "Type to search your notes." in 14 sp `t3`.
  - Query with zero hits shows centered "Nothing found for “<query>”." in 14 sp `t3`.
- [ ] **2.5 Match Text Ink-Weight Highlighting**
  - Matches in card snippet are bold (weight 700), `#14110D`, with 1.5 dp underline and NO background highlight.
  - Tapping a result card opens that note at the matched page in the Writer (`Route.Note(noteId, pageIndex)`).

---

## 3. Setup Screen & Shell Controls (Task 23)

- [ ] **3.1 Header & Meta**
  - Title shows "Setup", meta shows "Enk 1.0".
- [ ] **3.2 Appearance Group**
  - 3x2 grid of theme tiles in exact DS order: Graphite, Apricot, Sky, Slate, Mono, Dynamic.
  - Active tile shows 1.03 scale, 2 dp ring in `g1` + 1.5 dp ring in `ui`, and a 14x3 dp index mark in `ui`.
  - Mode segmented control has Light, Dark, System. Dragging or tapping switches theme mode instantly.
- [ ] **3.3 Writing Group**
  - Correction segmented control: "Clean erase", "Strike-through".
  - Autocorrect toggle with spring animation and 48 dp touch height.
- [ ] **3.4 Sound Group**
  - Sound sets: Classic, Antique, Soft, Keyboard with active indicator.
  - Tapping a sound set immediately prepares the sound player with that sound set.
  - Secondary controls: Mute toggle, continuous Bell volume fader (knob scales 1.12 while held), Haptics toggle.
- [ ] **3.5 Display Group**
  - "Reduce effects" toggle switches transitions to 100 ms fades and disables spring overshoots.
- [ ] **3.6 Data Group (Export / Import via SAF)**
  - Tapping "Export all notes" launches Android Storage Access Framework file picker (`CreateDocument`). Saving writes full ZIP archive.
  - Tapping "Import notes" launches SAF document picker (`OpenDocument`). Selecting valid archive restores notes and dividers without loss. Selecting corrupted or non-markdown archive shows failure reason and preserves existing notes.
- [ ] **3.7 About Group**
  - Displays "Enk" with version "1.0.0".
  - Footer displays: "Free and open source · Apache-2.0 / No network, no tracking."

---

## 4. Navigation & System Bar Integration

- [ ] **4.1 Navigation Tabs**
  - Bottom Glass Nav Bar displays: Write, Box, Find, Setup.
  - Selected destination highlights with sliding glass lens.
- [ ] **4.2 System Bar Icons**
  - In light mode: dark status bar and navigation bar icons.
  - In dark mode: light status bar and navigation bar icons.
