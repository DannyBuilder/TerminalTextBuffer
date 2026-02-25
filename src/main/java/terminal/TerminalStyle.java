package terminal;

/**
 * A handy constant for default text so we don't have to type "false, false, false" everywhere
 * @param isBold boolean - is the style bolded
 * @param isItalic boolean - is the style italics
 * @param isUnderline boolean - is the style underlined
 */
public record TerminalStyle(boolean isBold, boolean isItalic, boolean isUnderline) {
    public static final TerminalStyle DEFAULT = new TerminalStyle(false, false, false);
}
