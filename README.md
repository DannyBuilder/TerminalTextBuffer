# Terminal Text Buffer

A Java implementation of the core data structure used inside terminal emulators. The buffer stores a grid of character cells, maintains a cursor, and splits content into a visible screen and a scrollback history.

---

## How it works

The buffer is split into two regions:

```
┌─────────────────────┐  ← oldest scrollback line (index 0)
│      SCROLLBACK     │  read-only history, up to maxScrollback lines
├─────────────────────┤  ← screen row 0
│       SCREEN        │  editable, exactly height lines
└─────────────────────┘  ← screen row height-1
```

When the terminal scrolls (`insertLineAtBottom`), the top screen line is deep-copied into scrollback, then cleared and recycled as the new blank bottom line. The cursor writes into the screen only. Scrollback is read-only once a line enters it.

---

## Design decisions

### `Cell` — class, not an int

Each cell is a mutable object holding a `char`, a `TerminalColor` foreground, a `TerminalColor` background, and a `TerminalStyle`. It is mutated in place via `set(...)` rather than replaced on every write, which avoids garbage collection overhead.

**Alternative:** Pack everything into a single `int` — 4 bits foreground, 4 bits background, 3 bits style, 8 bits char. This is more memory-efficient and cache-friendly but much harder to read and maintain. The object approach was chosen for clarity.

---

### `TerminalStyle` — record, not enum

Three independent booleans (`isBold`, `isItalic`, `isUnderline`) as a record. An earlier version used an enum with all 8 combinations (BOLD, BOLD_ITALIC, etc.) which was combinatorially ugly. The record is cleaner and Java generates the constructor and accessors automatically. `TerminalStyle.DEFAULT` is a constant for the no-style case.

---

### `TextAttributes` — separate class, not fields on the buffer

Instead of having TextAttributes we can have the variables directly in `TerminalBuffer` directly. A separate class keeps the buffer uncluttered and, more importantly, it is easier to restore in case of a failure,

---

### Screen — `List<Line>` (`ArrayList`)

The screen needs fast random access on every cell read and write — `ArrayList` gives O(1) `get(index)`. On scroll, `remove(0)` is O(n) in the screen height, but height is small (24–50 rows) so this is negligible.

**Alternative:** A ring buffer over a fixed `Line[]` array gives O(1) scroll with no shifting, but requires modular index arithmetic everywhere (`array[(head + row) % height]`), which makes the code harder to follow for a minor gain.

---

### Scrollback — `Deque<Line>` (`ArrayDeque`)

The two hot operations on scrollback are adding a new line at the back and evicting the oldest line from the front when the cap is hit. `ArrayDeque` is O(1) for both. Random access by index is O(n) but that only happens when the user physically scrolls up — a rare, human-speed operation.

**Alternative:** `ArrayList` gives O(1) random access but O(n) eviction from the front. With a scrollback of 1000+ lines that matters. `LinkedList` has O(1) add/remove but worse cache locality and higher memory overhead per node than `ArrayDeque`.

---

### Scrollback immutability

Lines are deep-copied before entering scrollback. This costs one allocation per scroll but guarantees screen edits can never corrupt history. The alternative — shared references with copy-on-write — saves allocations but is much harder to reason about.

---

### Absolute row addressing

`getCharAt`, `getLineAt`, and `getAttributesAt` use a single absolute row index where row 0 is the oldest scrollback line and row `scrollbackSize` is the top of the screen. This matches the order `getFullContent` returns and means callers do not need to know whether a row is in scrollback or screen.

---



# I did not implement the bonus but this is my overall idea for it

## Bonus — implementation notes
### Wide characters

CJK ideographs and emoji occupy two columns. The approach would be:

- Add `wideLeft` and `wideContinuation` boolean flags to `Cell`. The left cell holds the character; the right cell is a blank placeholder. Renderers skip continuation cells.
- Before writing a wide character, check it fits (`cursorCol + 2 <= width`), then call a `breakWide` helper that blanks any existing half-pair at the target columns to prevent orphaned cells.
- Advance the cursor by 2 after a wide write. `moveCursorLeft` must step back an extra column if it lands on a continuation cell.

### Resize

- **Width change:** Create new `Line` objects of the new width, copying cell content across. Lines narrower than before are padded with blanks; wider lines are truncated (checking the last cell is not a half-wide-pair first).
- **Height increase:** Append blank lines at the bottom. Existing content stays at the top.
- **Height decrease:** Push excess top lines into scrollback so nothing is silently lost. Clamp the cursor to the new bounds.
- **Not handled:** Soft-wrap re-flow. If `insertText` wrapped a line across two rows and the terminal is resized wider, those rows would not merge back. Tracking soft-wrap state requires adding a flag to each `Line` and is out of scope. This is also the approach most real terminal emulators take.