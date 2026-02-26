package terminal;

/**
 * Represents a single row of Cells in the grid.
 * DESIGN DECISIONS:
 * Rows are mutable
 * Line is a Cell[] arr - we do not share Cell objects across Lines.
 * width is fixed at construction. If you want to change width creates a new line form the buffer
 */
public class Line {

    private final Cell[] cells;
    private final int width;

    /**
     * Constructor that creates a blank line of the given width. Every cell is default
     * @param width int - the number of columns must be > 0
     * @throws IllegalArgumentException if width is <=0
     */
    public Line(int width) {
        if (width <= 0)
            throw new IllegalArgumentException("Line width must be positive, got: " + width);
        this.width = width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = new Cell();
        }
    }


    /**
     * Getter for a cell from the line
     * @param col int - 0-indexed column must be in the correct bounds. Uses a private function to check
     * @return Cell - the cell at the given index
     * @throws IndexOutOfBoundsException if bounds are not met
     */
    public Cell getCell(int col) {
        checkBounds(col);
        return cells[col];
    }

    /**
     * Overwrites the cell at [col] with all four attributes at once.
     * Reuses the existing Cell object — no allocation.
     */
    public void setCell(int col, char ch, TerminalColor fg, TerminalColor bg, TerminalStyle style) {
        checkBounds(col);
        cells[col].set(ch, fg, bg, style);
    }


    /**
     * Fills the whole line with the same cell.
     */
    public void fill(char ch, TerminalColor fg, TerminalColor bg, TerminalStyle style) {
        for (Cell cell : cells) {
            cell.set(ch, fg, bg, style);
        }
    }


    /**
     * Returns the line content as a String, STRIPPING trailing spaces.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder(width);
        for (Cell cell : cells) {
            sb.append(cell.getCharacter());
        }
        // Trim trailing spaces (default fill char)
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return sb.substring(0, end);
    }

    /**
     * Creates a deep copy of this line, duplicating every Cell object.
     * @return Line a new line that is copied with setters to aviod new object allocation
     */
    public Line deepCopy() {
        Line copy = new Line(width);
        for (int col = 0; col < width; col++) {
            Cell src = cells[col];
            copy.cells[col].set(src.getCharacter(), src.getForeground(),
                    src.getBackground(), src.getStyle());
        }
        return copy;
    }


    /**
     * Helper method that checks if a given index is in the correct boundaries
     * @param col int - the column to be checked
     * @throws IndexOutOfBoundsException if the boundaries are not met
     */
    private void checkBounds(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " out of bounds for line width " + width);
        }
    }
}
