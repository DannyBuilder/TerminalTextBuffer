import org.junit.jupiter.api.Test;
import terminal.Cell;
import terminal.TerminalColor;
import terminal.TerminalStyle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cell Test class.
 */
class CellTest {

    @Test
    void testDefaultConstructor() {
        Cell cell = new Cell();

        assertEquals(' ', cell.getCharacter(), "Default character should be a space");
        assertEquals(TerminalColor.DEFAULT, cell.getForeground(), "Default foreground should be DEFAULT");
        assertEquals(TerminalColor.DEFAULT, cell.getBackground(), "Default background should be DEFAULT");
        assertEquals(TerminalStyle.DEFAULT, cell.getStyle(), "Default style should be DEFAULT");
    }

    @Test
    void testParameterizedConstructor() {
        TerminalStyle customStyle = new TerminalStyle(true, false, true);
        Cell cell = new Cell('A', TerminalColor.RED, TerminalColor.BLUE, customStyle);

        assertEquals('A', cell.getCharacter());
        assertEquals(TerminalColor.RED, cell.getForeground());
        assertEquals(TerminalColor.BLUE, cell.getBackground());
        assertEquals(customStyle, cell.getStyle());
    }

    @Test
    void testBulkSetMethod() {
        Cell cell = new Cell();
        TerminalStyle newStyle = new TerminalStyle(false, true, false);

        cell.set('X', TerminalColor.GREEN, TerminalColor.BLACK, newStyle);

        assertEquals('X', cell.getCharacter());
        assertEquals(TerminalColor.GREEN, cell.getForeground());
        assertEquals(TerminalColor.BLACK, cell.getBackground());
        assertEquals(newStyle, cell.getStyle());
    }

    @Test
    void testIndividualSetters() {
        Cell cell = new Cell();
        TerminalStyle customStyle = new TerminalStyle(true, true, true);

        cell.setCharacter('Z');
        assertEquals('Z', cell.getCharacter());
        assertEquals(TerminalColor.DEFAULT, cell.getForeground());

        cell.setForeground(TerminalColor.MAGENTA);
        assertEquals(TerminalColor.MAGENTA, cell.getForeground());

        cell.setBackground(TerminalColor.WHITE);
        assertEquals(TerminalColor.WHITE, cell.getBackground());

        cell.setStyle(customStyle);
        assertEquals(customStyle, cell.getStyle());
    }

    @Test
    void testResetMethod() {
        TerminalStyle customStyle = new TerminalStyle(true, false, false);
        Cell cell = new Cell('Q', TerminalColor.YELLOW, TerminalColor.CYAN, customStyle);
        cell.reset();
        assertEquals(' ', cell.getCharacter());
        assertEquals(TerminalColor.DEFAULT, cell.getForeground());
        assertEquals(TerminalColor.DEFAULT, cell.getBackground());
        assertEquals(TerminalStyle.DEFAULT, cell.getStyle());
    }
}
