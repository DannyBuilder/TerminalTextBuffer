package terminal;

/**
 * This is a class that holds all the TerminalBuffer attributes to make the terminal easy to recover,
 * and to make it easy to trace if the attributes or the terminal logic is slowing down the applicaiton
 *
 */
public class TextAttributes {

    private TerminalColor foreground;
    private TerminalColor background;
    private boolean bold;
    private boolean italic;
    private boolean underline;

    /**
     * Creates default attributes: DEFAULT colors, no style flags.
     */
    public TextAttributes() {
        this.foreground = TerminalColor.DEFAULT;
        this.background = TerminalColor.DEFAULT;
        this.bold      = false;
        this.italic    = false;
        this.underline = false;
    }


    /**
     * Getters for all the attributes. Use this java doc for all of them
     * @return - the parameter that is asked
     */
    public TerminalColor getForeground() { return foreground; }
    public TerminalColor getBackground() { return background; }
    public boolean isBold()              { return bold; }
    public boolean isItalic()            { return italic; }
    public boolean isUnderline()         { return underline; }


    /**
     * Setters for all the parameters. Use this java doc for everything
     */
    public void setForeground(TerminalColor foreground) { this.foreground = foreground; }
    public void setBackground(TerminalColor background) { this.background = background; }
    public void setBold(boolean bold)                   { this.bold = bold; }
    public void setItalic(boolean italic)               { this.italic = italic; }
    public void setUnderline(boolean underline)         { this.underline = underline; }

    /**
     * Converts the three independent style flags to the TerminalStyle needed by Cell
     * @return TerminalStyle - the style of the terminal with the specified values
     */
    public TerminalStyle toTerminalStyle() {
        return new TerminalStyle(bold, italic, underline);
    }


    /**
     * Resets all attributes to their defaults.
     */
    public void reset() {
        foreground = TerminalColor.DEFAULT;
        background = TerminalColor.DEFAULT;
        bold       = false;
        italic     = false;
        underline  = false;
    }
}

