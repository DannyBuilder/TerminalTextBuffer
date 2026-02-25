package terminal;

/**
 * This is a cell class that will hold values for a cell.
 * ASSUMPTION: Creating a huge grid of objects will be expensive but for the sake of simplicity and readability I decided to make it as a class.
 * A better solution would be to store it as an integer (last 4 for foreground color, 4 for background, 2-4 for style and 8 for the char)
 */
public class Cell {
    private char character;
    private TerminalColor foreground;
    private TerminalColor background;
    private TerminalStyle style;


    /**
     * Default constructor creates an empty space with default colors
     */
    public Cell() {
        this(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, TerminalStyle.DEFAULT);
    }

    /**
     * The full constructor. Creates a cell with the given parameters
     * @param character char - the char ot ' ' if empty
     * @param foreground TerminalColor - The color of the foreground
     * @param background TerminalColor - the color of the background
     * @param style TerminalStyle - the style fo the cell
     */
    public Cell(char character, TerminalColor foreground, TerminalColor background, TerminalStyle style) {
        this.character = character;
        this.foreground = foreground;
        this.background = background;
        this.style = style;
    }

    /**
     * Getters for the parameters
     * @return character - the char parameter
     */
    public char getCharacter() { return character; }
    public TerminalColor getForeground() { return foreground; }
    public TerminalColor getBackground() { return background; }
    public TerminalStyle getStyle() { return style; }

    /**
     * One big set method that sets all the parameters of the cell without creating a new object.
     * @param character char - new character
     * @param foreground TerminalColor - new foreground color
     * @param background TerminalColor - new background color
     * @param style TerminalStyle - new terminal style
     */
    public void set(char character, TerminalColor foreground, TerminalColor background, TerminalStyle style) {
        this.character = character;
        this.foreground = foreground;
        this.background = background;
        this.style = style;
    }

    /**
     * Resets the cell to default values
     */
    public void reset() {
        set(' ', TerminalColor.DEFAULT, TerminalColor.DEFAULT, TerminalStyle.DEFAULT);
    }
}
