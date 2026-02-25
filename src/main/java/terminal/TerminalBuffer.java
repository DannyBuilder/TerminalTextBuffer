package terminal;

import java.util.*;

public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;
    // The screen is a List because we care about the O(1) property to get a line. When we are removing it will be O(n) because we need to shift
    // This is a design choice. We optimize for random access.
    // If we used [] we have to shift every element when we scroll.
    // I think the best solution is to implement a ring buffer (read online) but for simplicity I have decided not to
    private final List<Line> screen;
    // Scrollback is a deque because this gives O(1) adding a line at the end and O(1) removing the top line when it hits the cap
    // We optimize for adding/removing
    // We can use a LinkedList but this creates more memory overhead and makes it harder.
    private final Deque<Line> scrollback;
    private int cursorCol;
    private int cursorRow;
    private final TextAttributes attributes = new TextAttributes();

    /**
     * Creates a TerminalBuffer with the given parameters
     *
     * @param width         number of columns must be > 0
     * @param height        number of rows on screen must be > 0
     * @param maxScrollback maximum number of lines kept in scrollback history
     *                      must be >= 0. Use 0 to disable scrollback.
     * @throws IllegalArgumentException if conditions are not met
     */
    public TerminalBuffer(int width, int height, int maxScrollback) {
        if (width  <= 0) throw new IllegalArgumentException("Width must be positive, got: "  + width);
        if (height <= 0) throw new IllegalArgumentException("Height must be positive, got: " + height);
        if (maxScrollback < 0) throw new IllegalArgumentException("maxScrollback must be >= 0, got: " + maxScrollback);

        this.width         = width;
        this.height        = height;
        this.maxScrollback = maxScrollback;
        this.cursorCol     = 0;
        this.cursorRow     = 0;

        this.screen    = new ArrayList<>(height);
        this.scrollback = new ArrayDeque<>();

        for (int i = 0; i < height; i++) {
            screen.add(new Line(width));
        }
    }

    /**
     * Convenience constructor with a default scrollback of 1000 lines.
     */
    public TerminalBuffer(int width, int height) {
        this(width, height, 1000);
    }


    /**
     * Getters for all attributes. Use this javadoc for all of them
     * @return the parameter that was asked
     */
    public int getWidth()          { return width; }
    public int getHeight()         { return height; }
    public int getMaxScrollback()  { return maxScrollback; }
    public int getScrollbackSize() { return scrollback.size(); }
    public int getCursorCol() { return cursorCol; }
    public int getCursorRow() { return cursorRow; }


    /**
     * Setters for the TextAttribute. Use this java doc for all of them
     * Updates the attributes with the new parameter
     * @param color the parameter to update.
     */
    public void setForeground(TerminalColor color) { attributes.setForeground(color); }
    public void setBackground(TerminalColor color) { attributes.setBackground(color); }
    public void setBold(boolean bold)              { attributes.setBold(bold); }
    public void setItalic(boolean italic)          { attributes.setItalic(italic); }
    public void setUnderline(boolean underline)    { attributes.setUnderline(underline); }

    /**
     * Resets all attributes to their defaults.
     */
    public void resetAttributes() { attributes.reset(); }



    /**
     * Moves the cursor to an absolute position, clamped to screen bounds.
     * @param col target column (0-indexed); clamped to [0, width-1]
     * @param row target row   (0-indexed); clamped to [0, height-1]
     */
    public void setCursorPosition(int col, int row) {
        this.cursorCol = clampCol(col);
        this.cursorRow = clampRow(row);
    }

    /**
     * Moves the cursor up by n rows, clamped at row 0.
     * @param n - int the number of columns to move the cursor up
     * @throws IllegalArgumentException if you try to move it up negative number
     */
    public void moveCursorUp(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
        cursorRow = Math.max(0, cursorRow - n);
    }

    /**
     * Moves the cursor down by n rows, clamped at the last row.
     * @param n - int the number of rows to move dow
     * @throws IllegalArgumentException if n is a negative number
     */
    public void moveCursorDown(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
        cursorRow = Math.min(height - 1, cursorRow + n);
    }

    /**
     Moves the cursor left by n columns, clamped at column 0.
     @param n - in the number of columns we move to the left
     @throws IllegalArgumentException if n is a negative number
     */
    public void moveCursorLeft(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
        cursorCol = Math.max(0, cursorCol - n);
    }

    /**
     Moves the cursor right by n columns, clamped at the last column
     @param n - in the number of columns we move to the right
     @throws IllegalArgumentException if n is a negative number
     */
    public void moveCursorRight(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
        cursorCol = Math.min(width - 1, cursorCol + n);
    }


    /**
     * Writes starting at the current cursor position, overwriting
     * any existing cell content. Characters that would fall beyond the right edge are discarded
     * The cursor is clamped at the end of the line
     *
     * @param text the text to write; must not be null
     * @throws IllegalArgumentException - if the text is null
     */
    public void writeText(String text) {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        Line line = screen.get(cursorRow);
        for (char ch : text.toCharArray()) {
            if (cursorCol >= width) break;
            line.setCell(cursorCol, ch, attributes.getForeground(), attributes.getBackground(), attributes.toStyle());
            cursorCol++;
        }
        cursorCol = clampCol(cursorCol);
    }

    /**
     * Inserts text at the cursor position, wrapping to the next line when
     * the right edge is reached. Continues until all text is written or
     * the bottom of the screen is reached (remaining chars are discarded).
     * Moves the cursor.
     * @param text String - the string to write
     * @throws IllegalArgumentException if the text is null
     */
    public void insertText(String text) {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        for (char ch : text.toCharArray()) {
            // If we've hit the right edge, wrap to next line
            if (cursorCol >= width) {
                if (cursorRow >= height - 1) break; // at bottom, discard rest
                cursorCol = 0;
                cursorRow++;
            }
            // Shift everything from the right edge down to cursorCol+1 one step right
            // The cell at width-1 is pushed off and discarded
            Line line = screen.get(cursorRow);
            for (int col = width - 1; col > cursorCol; col--) {
                Cell src = line.getCell(col - 1);
                line.setCell(col, src.getCharacter(), src.getForeground(),
                        src.getBackground(), src.getStyle());
            }
            // Now write the new character into the freed slot
            line.setCell(cursorCol, ch, attributes.getForeground(),
                    attributes.getBackground(), attributes.toStyle());
            cursorCol++;
        }
        cursorCol = clampCol(cursorCol);
    }

    /**
     * Fills the entire current cursor row with ch using the current
     * attributes. The cursor position is NOT changed.
     *
     * @param ch the character to fill with
     */
    public void fillCurrentLine(char ch) {
        screen.get(cursorRow).fill(ch, attributes.getForeground(), attributes.getBackground(), attributes.toStyle());
    }

    /**
     * Inserts a blank line at the bottom of the screen. The top line is pushed
     * into scrollback and all remaining lines shift up by one row.
     */
    public void insertLineAtBottom() {
        Line evicted = screen.removeFirst();
        pushToScrollback(evicted.deepCopy());
        evicted.fill(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, TerminalStyle.DEFAULT);
        screen.add(evicted);
    }

    /**
     * Clears all cells on every screen row. The cursor and scrollback are not affected
     */
    public void clearScreen() {
        for (Line line : screen) {
            line.fill(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, TerminalStyle.DEFAULT);
        }
    }

    /**
     * Clears all screen cells AND discards the entire scrollback history.
     */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }


    /**
     * Returns the Cell at the given screen position.
     * May throw an exception. check checkScreenBoundaries
     * @param col column (0-indexed)
     * @param row screen row (0-indexed)
     * @return Cell the cell at the given index
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Cell getCell(int col, int row) {
        checkScreenBounds(col, row);
        return screen.get(row).getCell(col);
    }

    /**
     * Returns the char at the given screen position or throes an exception
     * @param col column (0-indexed)
     * @param row screen row (0-indexed)
     * @return Char the cell at the given index
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public char getChar(int col, int row){
        return getCell(col, row).getCharacter();
    }


    /**
     * Returns the TerminalStyle of the cell at the given screen position.
     * @param col - int the column (0 indexed)
     * @param row - int the row (0 indexed)
     * @return TerminalStyle the style of the cell
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public TerminalStyle getCellStyle(int col, int row) {
        return getCell(col, row).getStyle();
    }

    /**
     * Returns the content of a screen row as a String, with trailing spaces stripped.
     * @param row screen row (0-indexed, [0, height))
     * @return String the string of the line
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public String getScreenLine(int row) {
        checkRowBounds(row);
        return screen.get(row).toDisplayString();
    }

    /**
     * Returns the entire screen as a newline-delimited String.
     * Each line has trailing spaces stripped. Rows are ordered top to bottom.
     * @return String the string line by line stargint from the top line
     */
    public String getScreenContent() {
        StringBuilder sb = new StringBuilder(height * (width + 1));
        for (int row = 0; row < height; row++) {
            sb.append(screen.get(row).toDisplayString());
            if (row < height - 1) sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Returns the Cell at the given scrollback position.
     *
     * @param col   column (0-indexed)
     * @param sbRow scrollback row (0 = oldest line)
     * @return Cell the cell at the given position
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public Cell getScrollbackCell(int col, int sbRow) {
        checkScrollbackBounds(sbRow);
        return getScrollbackLineObject(sbRow).getCell(col);
    }

    /**
     * Gets the char from a scrollback
     * @param col   column (0-indexed)
     * @param sbRow scrollback row (0 = oldest line)
     * @return char the char at the given position
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public char getCharScroll(int col, int sbRow){
        return getScrollbackCell(col, sbRow).getCharacter();
    }

    /**
     * Returns the character at the given position in the full buffer.
     * Row 0 = oldest scrollback line. Row scrollbackSize = first screen line.
     *
     * @param col    column (0-indexed)
     * @param absRow absolute row across scrollback + screen combined
     * @throws IndexOutOfBoundsException if the index is out of bounds
     * @return char the character at that position
     */
    public char getCharAt(int col, int absRow) {
        if (absRow < scrollback.size()) {
            return getCharScroll(col, absRow);
        }
        int screenRow = absRow - scrollback.size();
        checkScreenBounds(col, screenRow);
        return getChar(col,screenRow);
    }

    /**
     * Get line that gets the line on screeen + scrollback combined
     * @param absRow int - the row to consider
     * @return String  the string of thr row
     */
    public String getLineAt(int absRow) {
        if (absRow < scrollback.size()) {
            return getScrollbackLineString(absRow);
        }
        int screenRow = absRow - scrollback.size();
        return getScreenLine(screenRow);
    }

    /**
     * return the textAttributes of a cell anywhere in the screen + scrollback
     * @param col int - column
     * @param absRow int - the absolute row of the overall thing
     * @return TextAttributes - the Textattributes of the cell
     */
    public TextAttributes getAttributesAt(int col, int absRow) {
        Cell cell;
        if (absRow < scrollback.size()) {
            cell = getScrollbackCell(col, absRow);
        } else {
            int screenRow = absRow - scrollback.size();
            cell = getCell(col, screenRow);
        }
        TextAttributes snapshot = new TextAttributes();
        snapshot.setForeground(cell.getForeground());
        snapshot.setBackground(cell.getBackground());
        snapshot.setBold(cell.getStyle().isBold());
        snapshot.setItalic(cell.getStyle().isItalic());
        snapshot.setUnderline(cell.getStyle().isUnderline());
        return snapshot;
    }

    /**
     * Returns the full attributes (fg, bg, style) of a cell on the screen.
     * @param col column (0-indexed)
     * @param row screen row (0-indexed)
     * @return TextAttributes snapshot of the cell's attributes
     */
    public TextAttributes getCellAttributes(int col, int row) {
        return getAttributesAt(col, row + scrollback.size());
    }

    /**
     * Returns the full attributes (fg, bg, style) of a cell in scrollback.
     * @param col   column (0-indexed)
     * @param sbRow scrollback row (0 = oldest)
     * @return TextAttributes snapshot of the cell's attributes
     */
    public TextAttributes getScrollbackCellAttributes(int col, int sbRow) {
        return getAttributesAt(col, sbRow);
    }

    /**
     * Returns the TerminalStyle of a scrollback cell.
     * @param col - int the column (0 indexed)
     * @param sbRow - int the row (0 indexed)
     * @return TerminalStyle the style of the cell
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public TerminalStyle getScrollbackCellStyle(int col, int sbRow) {
        return getScrollbackCell(col, sbRow).getStyle();
    }

    /**
     * Returns the content of a scrollback row as a String (trailing spaces stripped).
     *
     * @param sbRow screen row (0-indexed, [0, height))
     * @return String the string of the line
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public String getScrollbackLineString(int sbRow) {
        checkScrollbackBounds(sbRow);
        return getScrollbackLineObject(sbRow).toDisplayString();
    }

    /**
     * Returns the full buffer content (scrollback + screen) as a newline-
     * delimited String. Scrollback lines appear first (oldest at top).
     * @return String - the full content
     */
    public String getFullContent() {
        int totalLines = scrollback.size() + height;
        StringBuilder sb = new StringBuilder(totalLines * (width + 1));
        for (Line line : scrollback) {
            sb.append(line.toDisplayString()).append('\n');
        }
        for (int row = 0; row < height; row++) {
            sb.append(screen.get(row).toDisplayString());
            if (row < height - 1) sb.append('\n');
        }
        return sb.toString();
    }


    /**
     * A helper method that adds a line to the scrollback. if scrollback is full remove the top most line
     * @param line the line to add
     */
    private void pushToScrollback(Line line) {
        if (maxScrollback == 0) return;
        scrollback.addLast(line);
        if (scrollback.size() > maxScrollback) {
            scrollback.removeFirst();
        }
    }

    /**
     * Get the line at a given row
     * O(n) — acceptable since scrollback access is UI-driven, not on the hot path.
     * @param sbRow int - the row of the line we want
     * @return Line the line at the given row
     */
    private Line getScrollbackLineObject(int sbRow) {
        int i = 0;
        for (Line line : scrollback) {
            if (i == sbRow) return line;
            i++;
        }
        throw new IndexOutOfBoundsException("Scrollback row " + sbRow + " out of bounds");
    }

    /**
     * Clamps the col between the first and last col
     * @param col int the current column
     * @return int the column we get clamped to
     */
    private int clampCol(int col) { return Math.max(0, Math.min(width  - 1, col)); }

    /**
     * Clamps the row between the first and last row
     * @param row int the row to clamp
     * @return int - the clampedRow
     */
    private int clampRow(int row) { return Math.max(0, Math.min(height - 1, row)); }

    /**
     * Checks if a given row is in the bounds
     * @param row int - the row to check
     * @throws IndexOutOfBoundsException if the row is out of bounds
     */
    private void checkRowBounds(int row) {
        if (row < 0 || row >= height)
            throw new IndexOutOfBoundsException("Row " + row + " out of bounds [0, " + height + ")");
    }

    /**
     * Checks if a given index is inside the screen bounds
     * @param col int - the given column
     * @param row int - the given row
     * @throws IndexOutOfBoundsException if the col, row is outside of the bounds
     */
    private void checkScreenBounds(int col, int row) {
        checkRowBounds(row);
        if (col < 0 || col >= width)
            throw new IndexOutOfBoundsException("Col " + col + " out of bounds [0, " + width + ")");
    }

    /**
     * Checks if a given index is inside the scrollback bounds
     * @param sbRow int - the int of the row to check
     * @throws IndexOutOfBoundsException if the col, row is outside of the bounds
     */
    private void checkScrollbackBounds(int sbRow) {
        if (sbRow < 0 || sbRow >= scrollback.size())
            throw new IndexOutOfBoundsException(
                    "Scrollback row " + sbRow + " out of bounds [0, " + scrollback.size() + ")");
    }
}
