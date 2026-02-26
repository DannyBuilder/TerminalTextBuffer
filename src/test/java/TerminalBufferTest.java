import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import terminal.Cell;
import terminal.TerminalBuffer;
import terminal.TerminalColor;
import terminal.TextAttributes;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private TerminalBuffer buf;

    @BeforeEach
    void setUp() {
        buf = new TerminalBuffer(10, 5, 5);
    }

    @Nested
    class ConstructionTests {

        @Test
        @DisplayName("Stores dimensions, cursor at origin, screen blank, scrollback empty")
        void initialState() {
            assertEquals(10, buf.getWidth());
            assertEquals(5, buf.getHeight());
            assertEquals(5, buf.getMaxScrollback());
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
            assertEquals(0, buf.getScrollbackSize());
            for (int row = 0; row < buf.getHeight(); row++) {
                assertEquals("", buf.getScreenLine(row));
            }
        }

        @Test
        @DisplayName("Default constructor gives 1000-line scrollback")
        void defaultScrollback() {
            assertEquals(1000, new TerminalBuffer(80, 24).getMaxScrollback());
        }

        @Test
        @DisplayName("Invalid dimensions throw IllegalArgumentException")
        void invalidDimensionsThrow() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(0, 5, 5));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(10, -1, 5));
            assertThrows(IllegalArgumentException.class, () -> new TerminalBuffer(10, 5, -1));
        }
    }

    @Nested
    class AttributeTests {

        @Test
        @DisplayName("Written cells carry current fg, bg, and style flags")
        void cellsCarryAttributes() {
            buf.setForeground(TerminalColor.RED);
            buf.setBackground(TerminalColor.BLUE);
            buf.setBold(true);
            buf.setItalic(true);
            buf.setUnderline(true);
            buf.writeText("A");
            Cell c = buf.getCell(0, 0);
            assertEquals(TerminalColor.RED, c.getForeground());
            assertEquals(TerminalColor.BLUE, c.getBackground());
            assertTrue(c.getStyle().isBold());
            assertTrue(c.getStyle().isItalic());
            assertTrue(c.getStyle().isUnderline());
        }

        @Test
        @DisplayName("resetAttributes returns pen to defaults for subsequent writes")
        void resetRestoresDefaults() {
            buf.setForeground(TerminalColor.GREEN);
            buf.setBold(true);
            buf.resetAttributes();
            buf.writeText("A");
            assertEquals(TerminalColor.DEFAULT, buf.getCell(0, 0).getForeground());
            assertFalse(buf.getCell(0, 0).getStyle().isBold());
        }

        @Test
        @DisplayName("Attribute changes do not retroactively affect already-written cells")
        void attributeChangeNotRetroactive() {
            buf.setForeground(TerminalColor.CYAN);
            buf.writeText("A");
            buf.setForeground(TerminalColor.WHITE);
            assertEquals(TerminalColor.CYAN, buf.getCell(0, 0).getForeground());
        }

        @Test
        @DisplayName("Adjacent cells can hold different attributes")
        void perCellAttributes() {
            buf.setForeground(TerminalColor.RED);
            buf.writeText("A");
            buf.setForeground(TerminalColor.GREEN);
            buf.writeText("B");
            assertEquals(TerminalColor.RED, buf.getCell(0, 0).getForeground());
            assertEquals(TerminalColor.GREEN, buf.getCell(1, 0).getForeground());
        }
    }

    @Nested
    class CursorTests {

        @Test
        @DisplayName("setCursorPosition sets location and clamps out-of-bounds values")
        void setCursorClamped() {
            buf.setCursorPosition(4, 2);
            assertEquals(4, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());

            buf.setCursorPosition(-1, 999);
            assertEquals(0, buf.getCursorCol());
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        @DisplayName("Move cursor in all directions, clamped at bounds")
        void moveCursorAllDirections() {
            buf.setCursorPosition(5, 3);
            buf.moveCursorUp(2);     assertEquals(1, buf.getCursorRow());
            buf.moveCursorDown(10);  assertEquals(4, buf.getCursorRow());
            buf.moveCursorLeft(3);   assertEquals(2, buf.getCursorCol());
            buf.moveCursorRight(10); assertEquals(9, buf.getCursorCol());
        }

        @Test
        @DisplayName("Negative movement throws IllegalArgumentException")
        void negativeMovementThrows() {
            assertThrows(IllegalArgumentException.class, () -> buf.moveCursorUp(-1));
            assertThrows(IllegalArgumentException.class, () -> buf.moveCursorDown(-1));
            assertThrows(IllegalArgumentException.class, () -> buf.moveCursorLeft(-1));
            assertThrows(IllegalArgumentException.class, () -> buf.moveCursorRight(-1));
        }
    }

    @Nested
    @DisplayName("writeText")
    class WriteTextTests {

        @Test
        @DisplayName("Writes at cursor, advances cursor, does not wrap")
        void basicWrite() {
            buf.writeText("Hello");
            assertEquals("Hello", buf.getScreenLine(0));
            assertEquals(5, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("Overwrites existing content")
        void overwritesExisting() {
            buf.writeText("AAAA");
            buf.setCursorPosition(1, 0);
            buf.writeText("BB");
            assertEquals("ABBA", buf.getScreenLine(0));
        }

        @Test
        @DisplayName("Characters beyond right edge discarded, cursor clamped, no wrap")
        void overflowBehaviour() {
            buf.writeText("12345678901234");
            assertEquals("1234567890", buf.getScreenLine(0));
            assertEquals(9, buf.getCursorCol());
            assertEquals("", buf.getScreenLine(1));
        }

        @Test
        @DisplayName("null throws IllegalArgumentException")
        void nullThrows() {
            assertThrows(IllegalArgumentException.class, () -> buf.writeText(null));
        }
    }

    @Nested
    @DisplayName("insertText")
    class InsertTextTests {

        @Test
        @DisplayName("Shifts existing content right, overflow discarded")
        void shiftsRight() {
            buf.writeText("ABCDEFGHIJ");
            buf.setCursorPosition(0, 0);
            buf.insertText("X");
            assertEquals('X', buf.getChar(0, 0));
            assertEquals('A', buf.getChar(1, 0));
            // J was pushed off — last char must not be J
            assertNotEquals('J', buf.getChar(9, 0));
        }

        @Test
        @DisplayName("Wraps to next line when right edge is reached")
        void wrapsToNextLine() {
            buf.setCursorPosition(8, 0);
            buf.insertText("ABCD");
            assertEquals('A', buf.getChar(8, 0));
            assertEquals('B', buf.getChar(9, 0));
            assertEquals('C', buf.getChar(0, 1));
            assertEquals('D', buf.getChar(1, 1));
            assertEquals(1, buf.getCursorRow());
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        @DisplayName("Discards remaining chars when bottom-right is reached")
        void discardsAtBottom() {
            buf.setCursorPosition(9, 4);
            buf.insertText("ABCDE"); // should not throw
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        @DisplayName("null throws IllegalArgumentException")
        void nullThrows() {
            assertThrows(IllegalArgumentException.class, () -> buf.insertText(null));
        }
    }


    @Nested
    class FillLineTests {

        @Test
        @DisplayName("Fills cursor row entirely, does not move cursor, other rows unaffected")
        void fillsCurrentRowOnly() {
            buf.setCursorPosition(3, 2);
            buf.fillCurrentLine('*');
            assertEquals("**********", buf.getScreenLine(2));
            assertEquals(3, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
            assertEquals("", buf.getScreenLine(0));
        }

        @Test
        @DisplayName("Space fill makes line appear empty; fill overwrites existing content")
        void fillOverwritesAndBlankAppearsEmpty() {
            buf.writeText("ABCDEFGHIJ");
            buf.setCursorPosition(0, 0);
            buf.fillCurrentLine(' ');
            assertEquals("", buf.getScreenLine(0));
        }
    }

    @Nested
    class InsertLineAtBottomTests {

        @Test
        @DisplayName("Evicts top line to scrollback, shifts screen up, adds blank bottom")
        void basicScroll() {
            buf.setCursorPosition(0, 0); buf.writeText("Row0");
            buf.setCursorPosition(0, 1); buf.writeText("Row1");
            buf.insertLineAtBottom();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("Row0", buf.getScrollbackLineString(0));
            assertEquals("Row1", buf.getScreenLine(0));
            assertEquals("", buf.getScreenLine(4));
        }

        @Test
        @DisplayName("Scrollback is an immutable snapshot of the evicted line")
        void scrollbackIsSnapshot() {
            buf.writeText("Original");
            buf.insertLineAtBottom();
            buf.setCursorPosition(0, 0);
            buf.writeText("XXXXXXXXXX");
            assertEquals("Original", buf.getScrollbackLineString(0));
        }

        @Test
        @DisplayName("Scrollback capped at max; oldest line evicted first (FIFO)")
        void cappedAndFifo() {
            for (int i = 0; i < 7; i++) {
                buf.setCursorPosition(0, 0);
                buf.writeText("L" + i);
                buf.insertLineAtBottom();
            }
            assertEquals(5, buf.getScrollbackSize());
            assertEquals("L2", buf.getScrollbackLineString(0));
            assertEquals("L6", buf.getScrollbackLineString(4));
        }

        @Test
        @DisplayName("maxScrollback=0 discards evicted lines entirely")
        void zeroScrollbackDiscards() {
            TerminalBuffer b = new TerminalBuffer(10, 5, 0);
            b.writeText("gone");
            b.insertLineAtBottom();
            assertEquals(0, b.getScrollbackSize());
        }
    }

    @Nested
    class ClearTests {

        @Test
        @DisplayName("clearScreen blanks all rows, keeps scrollback and cursor")
        void clearScreenKeepsScrollbackAndCursor() {
            buf.writeText("data");
            buf.insertLineAtBottom();
            buf.setCursorPosition(3, 2);
            buf.clearScreen();
            for (int row = 0; row < buf.getHeight(); row++) {
                assertEquals("", buf.getScreenLine(row));
            }
            assertEquals(1, buf.getScrollbackSize());
            assertEquals(3, buf.getCursorCol());
        }

        @Test
        @DisplayName("clearScreenAndScrollback removes everything, keeps cursor")
        void clearBothRemovesEverything() {
            buf.writeText("data");
            buf.insertLineAtBottom();
            buf.setCursorPosition(2, 2);
            buf.clearScreenAndScrollback();
            assertEquals(0, buf.getScrollbackSize());
            for (int row = 0; row < buf.getHeight(); row++) {
                assertEquals("", buf.getScreenLine(row));
            }
            assertEquals(2, buf.getCursorCol());
        }
    }

    @Nested
    class ContentAccessTests {

        @BeforeEach
        void writeData() {
            // scrollback[0] = "SB0" with RED fg + BOLD
            buf.setForeground(TerminalColor.RED);
            buf.setBold(true);
            buf.setCursorPosition(0, 0);
            buf.writeText("SB0");
            buf.insertLineAtBottom();
            // screen[0] = "SC0" with BLUE fg
            buf.resetAttributes();
            buf.setForeground(TerminalColor.BLUE);
            buf.setCursorPosition(0, 0);
            buf.writeText("SC0");
            buf.resetAttributes();
        }

        @Test
        @DisplayName("getCell and getChar return correct character and fg from screen")
        void getFromScreen() {
            assertEquals('S', buf.getChar(0, 0));
            assertEquals(TerminalColor.BLUE, buf.getCell(0, 0).getForeground());
        }

        @Test
        @DisplayName("getCell throws for out-of-bounds positions")
        void getCellOob() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCell(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCell(0, 5));
        }

        @Test
        @DisplayName("getCellStyle and getCellAttributes return correct style and full attrs")
        void screenAttributes() {
            buf.setCursorPosition(0, 1);
            buf.setBold(true);
            buf.writeText("X");
            assertTrue(buf.getCellStyle(0, 1).isBold());
            assertTrue(buf.getCellAttributes(0, 1).isBold());
        }

        @Test
        @DisplayName("getCellAttributes returns a snapshot — mutations do not affect the cell")
        void cellAttributesIsSnapshot() {
            TextAttributes attrs = buf.getCellAttributes(0, 0);
            attrs.setForeground(TerminalColor.WHITE);
            assertEquals(TerminalColor.BLUE, buf.getCell(0, 0).getForeground());
        }

        @Test
        @DisplayName("getScreenLine strips trailing spaces; getScreenContent has height-1 newlines")
        void screenLineAndContent() {
            assertEquals("SC0", buf.getScreenLine(0));
            long newlines = buf.getScreenContent().chars().filter(c -> c == '\n').count();
            assertEquals(buf.getHeight() - 1, newlines);
        }

        @Test
        @DisplayName("getCharScroll, getScrollbackCell, getScrollbackCellAttributes from scrollback")
        void getFromScrollback() {
            assertEquals('S', buf.getCharScroll(0, 0));
            assertEquals(TerminalColor.RED, buf.getScrollbackCell(0, 0).getForeground());
            TextAttributes attrs = buf.getScrollbackCellAttributes(0, 0);
            assertEquals(TerminalColor.RED, attrs.getForeground());
            assertTrue(attrs.isBold());
        }

        @Test
        @DisplayName("getScrollbackLineString throws for out-of-bounds index")
        void scrollbackOob() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getScrollbackLineString(1));
        }

        @Test
        @DisplayName("getCharAt, getLineAt, getAttributesAt unify screen and scrollback by absolute row")
        void unifiedAccess() {
            // absRow 0 = scrollback[0] = SB0 (RED), absRow 1 = screen[0] = SC0 (BLUE)
            assertEquals('S', buf.getCharAt(0, 0));
            assertEquals('S', buf.getCharAt(0, 1));
            assertEquals("SB0", buf.getLineAt(0));
            assertEquals("SC0", buf.getLineAt(1));
            assertEquals(TerminalColor.RED,  buf.getAttributesAt(0, 0).getForeground());
            assertEquals(TerminalColor.BLUE, buf.getAttributesAt(0, 1).getForeground());
        }

        @Test
        @DisplayName("getFullContent: scrollback lines appear before screen lines")
        void fullContent() {
            String full = buf.getFullContent();
            assertTrue(full.indexOf("SB0") < full.indexOf("SC0"));
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        @DisplayName("1x1 buffer: write, scroll, verify scrollback and blank screen")
        void oneByOne() {
            TerminalBuffer b = new TerminalBuffer(1, 1, 10);
            b.writeText("Q");
            b.insertLineAtBottom();
            assertEquals("Q", b.getScrollbackLineString(0));
            assertEquals("", b.getScreenLine(0));
        }

        @Test
        @DisplayName("Fill then write: per-cell attributes are independent")
        void fillThenWrite() {
            buf.setBackground(TerminalColor.WHITE);
            buf.fillCurrentLine(' ');
            buf.setBackground(TerminalColor.DEFAULT);
            buf.setCursorPosition(2, 0);
            buf.writeText("Hi");
            assertEquals(TerminalColor.WHITE,   buf.getCellAttributes(0, 0).getBackground());
            assertEquals(TerminalColor.DEFAULT,  buf.getCellAttributes(2, 0).getBackground());
        }

        @Test
        @DisplayName("Cursor stays in bounds through rapid movement and writes")
        void cursorAlwaysInBounds() {
            for (int i = 0; i < 20; i++) {
                buf.moveCursorRight(3); buf.moveCursorDown(2);
                buf.writeText("XY");
                buf.moveCursorLeft(5); buf.moveCursorUp(3);
            }
            assertTrue(buf.getCursorCol() >= 0 && buf.getCursorCol() <= buf.getWidth() - 1);
            assertTrue(buf.getCursorRow() >= 0 && buf.getCursorRow() <= buf.getHeight() - 1);
        }

        @Test
        @DisplayName("All 16 ANSI colors survive round-trip through a cell")
        void allColorsRoundTrip() {
            TerminalColor[] colors = {
                    TerminalColor.BLACK, TerminalColor.RED, TerminalColor.GREEN, TerminalColor.YELLOW,
                    TerminalColor.BLUE, TerminalColor.MAGENTA, TerminalColor.CYAN, TerminalColor.WHITE,
                    TerminalColor.BRIGHT_BLACK, TerminalColor.BRIGHT_RED, TerminalColor.BRIGHT_GREEN,
                    TerminalColor.BRIGHT_YELLOW, TerminalColor.BRIGHT_BLUE, TerminalColor.BRIGHT_MAGENTA,
                    TerminalColor.BRIGHT_CYAN, TerminalColor.BRIGHT_WHITE
            };
            TerminalBuffer b = new TerminalBuffer(16, 1, 0);
            for (int i = 0; i < colors.length; i++) {
                b.setForeground(colors[i]);
                b.setCursorPosition(i, 0);
                b.writeText("X");
            }
            for (int i = 0; i < colors.length; i++) {
                assertEquals(colors[i], b.getCell(i, 0).getForeground());
            }
        }
    }
}