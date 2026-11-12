package org.netbeans.modules.python.editor;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Test;
import org.netbeans.api.editor.caret.CaretInfo;
import org.netbeans.api.editor.caret.EditorCaret;
import org.netbeans.editor.BaseDocument;

import static org.junit.Assert.assertEquals;

/**
 * The edits below are the responses of python-lsp-server 1.15.0 with yapf
 * 0.43.0, autopep8 2.0.4 and python-lsp-black 2.0.0 (black 26.10.1).
 */
public class PythonReformatTaskTest {

    private static final String HEADER_SOURCE = "# header\nx=1\ny=2\nz=3\n";

    /** Selection: line 1 with its line break, so it ends at column 0 of line 2. */
    private static final int SELECTION_START = 9;
    private static final int SELECTION_END = 13;

    /** YAPF formats one line more than requested and deletes and inserts lines separately. */
    @Test
    public void yapfFormatSelectionKeepsTheSelectedStatementAndOtherLines() throws Exception {
        BaseDocument document = document(HEADER_SOURCE);
        PythonReformatTask.SelectedLines lines = PythonReformatTask.selectedLines(document, SELECTION_START, SELECTION_END);
        assertEquals("The request ends on the selected line", 12, lines.requestEnd());
        formatSelection(document, SELECTION_START, SELECTION_END,
                edit(1, 0, 2, 0, ""),
                edit(2, 0, 3, 0, ""),
                edit(3, 0, 3, 0, "x = 1\n"),
                edit(3, 0, 3, 0, "y = 2\n"));
        assertEquals("# header\nx = 1\ny=2\nz=3\n", text(document));
    }

    @Test
    public void autopep8FormatSelectionWithWholeDocumentEdit() throws Exception {
        BaseDocument document = document(HEADER_SOURCE);
        formatSelection(document, SELECTION_START, SELECTION_END,
                edit(0, 0, 4, 0, "# header\nx = 1\ny=2\nz=3\n"));
        assertEquals("# header\nx = 1\ny=2\nz=3\n", text(document));
    }

    @Test
    public void blackFormatSelection() throws Exception {
        BaseDocument document = document(HEADER_SOURCE);
        formatSelection(document, SELECTION_START, SELECTION_END,
                edit(1, 0, 2, 0, "x = 1\n"));
        assertEquals("# header\nx = 1\ny=2\nz=3\n", text(document));
    }

    /** YAPF joins the selected statement's lines and also reformats the next line. */
    @Test
    public void yapfFormatSelectionJoiningLines() throws Exception {
        BaseDocument document = document("x = foo(\n    1)\ny=2\nz=3\n");
        formatSelection(document, 0, 16,
                edit(0, 0, 1, 0, ""),
                edit(1, 0, 2, 0, ""),
                edit(2, 0, 3, 0, ""),
                edit(3, 0, 3, 0, "x = foo(1)\n"),
                edit(3, 0, 3, 0, "y = 2\n"));
        assertEquals("x = foo(1)\ny=2\nz=3\n", text(document));
    }

    /** Re-indenting only the selected part of a block would break the code. */
    @Test
    public void formatSelectionDoesNotReindentPartOfABlock() throws Exception {
        List<TextEdit> yapf = List.of(
                edit(1, 0, 2, 0, ""),
                edit(2, 0, 3, 0, ""),
                edit(3, 0, 3, 0, "    a = 1\n"),
                edit(3, 0, 3, 0, "    b = 2\n"));
        BaseDocument document = document("if x:\n  a=1\n  b=2\n");
        formatSelection(document, 6, 12, yapf.toArray(TextEdit[]::new));
        assertEquals("if x:\n  a=1\n  b=2\n", text(document));
        // The same response for a selection of the whole block.
        formatSelection(document, 6, 18, yapf.toArray(TextEdit[]::new));
        assertEquals("if x:\n    a = 1\n    b = 2\n", text(document));
    }

    @Test
    public void insertsAtTheSamePositionKeepTheirOrder() throws Exception {
        BaseDocument document = document("x=1\ny=2\n");
        PythonReformatTask.applyEdits(document, List.of(
                edit(0, 0, 1, 0, ""),
                edit(1, 0, 2, 0, ""),
                edit(2, 0, 2, 0, "x = 1\n"),
                edit(2, 0, 2, 0, "y = 2\n")), 0, document.getLength(), List.of());
        assertEquals("x = 1\ny = 2\n", text(document));
    }

    @Test
    public void formatterEndOfDocumentPositionWithoutFinalNewline() throws Exception {
        BaseDocument document = document("x=1");
        PythonReformatTask.applyEdits(document, List.of(edit(0, 0, 1, 0, "x = 1\n")),
                0, document.getLength(), List.of());
        assertEquals("x = 1\n", text(document));
    }

    @Test
    public void invalidOrOverlappingEditsChangeNothing() throws Exception {
        BaseDocument document = document("x=1\n");
        PythonReformatTask.applyEdits(document, List.of(edit(0, 0, 0, 3, "x = 1"), edit(0, 1, 0, 2, " = ")),
                0, document.getLength(), List.of());
        PythonReformatTask.applyEdits(document, List.of(edit(0, 2, 0, 1, "")),
                0, document.getLength(), List.of());
        PythonReformatTask.applyEdits(document, null, 0, document.getLength(), List.of());
        assertEquals("x=1\n", text(document));
    }

    /**
     * #127: the cursor moved to the end of the document. Black replaces the
     * whole text and adds blank lines above the cursor.
     */
    @Test
    public void formatKeepsTheCursorAtItsCode() throws Exception {
        String source = "import os\ndef f( a,b ):\n    return a+b\nx=f( 1,2 )\n";
        String formatted = "import os\n\n\ndef f(a, b):\n    return a + b\n\n\nx = f(1, 2)\n";
        SwingUtilities.invokeAndWait(() -> {
            JTextArea editor = new JTextArea();
            editor.setDocument(document(source));
            editor.setCaretPosition(source.indexOf("x=f"));
            editor.moveCaretPosition(source.indexOf("f( 1"));
            formatDocument(editor, List.of(edit(0, 0, 4, 0, formatted)));
            assertEquals(formatted, editor.getText());
            assertEquals("Selection start", formatted.indexOf("x = f"), editor.getCaret().getMark());
            assertEquals("Cursor", formatted.indexOf("f(1"), editor.getCaret().getDot());
        });
    }

    private static final String CARETS_SOURCE = "# header\nx=1+2\nz = 3\n";
    private static final String CARETS_FORMATTED = "# header\nx = 1 + 2\nz = 3\n";
    private static final List<TextEdit> CARETS_YAPF = List.of(
            edit(1, 0, 2, 0, ""),
            edit(2, 0, 2, 0, "x = 1 + 2\n"));

    @Test
    public void formatKeepsAllCarets() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane editor = editorWithCarets(2, 2, 11, 11);
            formatDocument(editor, CARETS_YAPF);
            assertEquals(CARETS_FORMATTED, editor.getText());
            assertCarets(editor, 2, 2, 13, 13);
        });
    }

    @Test
    public void formatKeepsAllCaretsFromABackgroundThread() throws Exception {
        List<JEditorPane> editor = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> editor.add(editorWithCarets(2, 2, 11, 11)));
        formatDocument(editor.get(0), CARETS_YAPF);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(CARETS_FORMATTED, editor.get(0).getText());
            assertCarets(editor.get(0), 2, 2, 13, 13);
        });
    }

    /** "header" selected, and in the changed line from "1" to the line end. */
    @Test
    public void formatKeepsAllSelections() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane editor = editorWithCarets(8, 2, 14, 11);
            formatDocument(editor, CARETS_YAPF);
            assertEquals(CARETS_FORMATTED, editor.getText());
            assertCarets(editor, 8, 2, 18, 13);
        });
    }

    /** EditorCaret merges carets that meet while text is replaced. */
    @Test
    public void formatKeepsCaretsInTheSameChangedText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane editor = editorWithCarets(10, 10, 12, 12);
            formatDocument(editor, CARETS_YAPF);
            assertEquals(CARETS_FORMATTED, editor.getText());
            assertCarets(editor, 11, 11, 15, 15);
        });
    }

    private static final String PARENTHESES_SOURCE = "x=(((1)))\ny=(((2)))\n# anchor\nz=(((3)))\n";
    private static final String PARENTHESES_FORMATTED = "x = 1\ny = 2\n# anchor\nz = 3\n";
    /** Black (python-lsp-black 2.0.0) removes the redundant parentheses. */
    private static final List<TextEdit> PARENTHESES_BLACK = List.of(edit(0, 0, 4, 0, PARENTHESES_FORMATTED));

    /** The cursor before "# anchor" stays there although code before it was removed. */
    @Test
    public void formatKeepsTheCursorWhenCodeIsRemoved() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane editor = editorWithCarets(PARENTHESES_SOURCE, 20, 20);
            formatDocument(editor, PARENTHESES_BLACK);
            assertEquals(PARENTHESES_FORMATTED, editor.getText());
            assertCarets(editor, 12, 12);
        });
    }

    @Test
    public void formatKeepsTheCursorWhenCodeIsRemovedFromABackgroundThread() throws Exception {
        List<JEditorPane> editor = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> editor.add(editorWithCarets(PARENTHESES_SOURCE, 20, 20)));
        formatDocument(editor.get(0), PARENTHESES_BLACK);
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(PARENTHESES_FORMATTED, editor.get(0).getText());
            assertCarets(editor.get(0), 12, 12);
        });
    }

    /**
     * Carets in removed code move after the kept code before it: before
     * "(((2" to after "y =", after "3)))" to after "3"; inside "anchor"
     * stays inside it.
     */
    @Test
    public void formatMovesCaretsInRemovedCodeToKeptCode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JEditorPane editor = editorWithCarets(PARENTHESES_SOURCE, 12, 12, 24, 24, 38, 38);
            formatDocument(editor, PARENTHESES_BLACK);
            assertEquals(PARENTHESES_FORMATTED, editor.getText());
            assertCarets(editor, 9, 9, 16, 16, 26, 26);
        });
    }

    /** An editor whose carets have the given dot and mark pairs, the last one added last. */
    private static JEditorPane editorWithCarets(int... dotsAndMarks) {
        return editorWithCarets(CARETS_SOURCE, dotsAndMarks);
    }

    /** An editor with {@code source} whose carets have the given dot and mark pairs. */
    private static JEditorPane editorWithCarets(String source, int... dotsAndMarks) {
        JEditorPane editor = new JEditorPane() {
            @Override
            public Toolkit getToolkit() {
                // EditorCaret copies selections to the system selection, which
                // fails without a display. It skips that if access is denied.
                throw new SecurityException("No system selection in tests");
            }
        };
        editor.setDocument(document(source));
        EditorCaret caret = new EditorCaret();
        editor.setCaret(caret);
        Document document = editor.getDocument();
        try {
            caret.setDot(dotsAndMarks[1]);
            caret.moveDot(dotsAndMarks[0]);
            for (int k = 2; k < dotsAndMarks.length; k += 2) {
                caret.addCaret(document.createPosition(dotsAndMarks[k]), Position.Bias.Forward,
                        document.createPosition(dotsAndMarks[k + 1]), Position.Bias.Forward);
            }
        } catch (BadLocationException ex) {
            throw new AssertionError(ex);
        }
        assertCarets(editor, dotsAndMarks);
        return editor;
    }

    private static void assertCarets(JTextComponent editor, int... dotsAndMarks) {
        List<Integer> expected = new ArrayList<>();
        for (int position : dotsAndMarks) {
            expected.add(position);
        }
        List<Integer> actual = new ArrayList<>();
        for (CaretInfo caret : ((EditorCaret) editor.getCaret()).getCarets()) {
            actual.add(caret.getDot());
            actual.add(caret.getMark());
        }
        assertEquals("Dot and mark of each caret", expected, actual);
    }

    /** As {@code rangeFormat()} for a selection from {@code start} to {@code end}. */
    private static void formatSelection(Document document, int start, int end, TextEdit... edits) {
        PythonReformatTask.SelectedLines lines = PythonReformatTask.selectedLines(document, start, end);
        PythonReformatTask.applyEdits(document, List.of(edits), lines.changeStart(), lines.changeEnd(), List.of());
    }

    /** As {@code documentFormat()}, which holds the document lock. */
    private static void formatDocument(JTextComponent editor, List<TextEdit> edits) {
        BaseDocument document = (BaseDocument) editor.getDocument();
        document.runAtomic(() -> PythonReformatTask.applyEdits(document, edits, 0, document.getLength(), List.of(editor)));
    }

    private static BaseDocument document(String text) {
        BaseDocument document = new BaseDocument(false, "text/plain");
        try {
            document.insertString(0, text, null);
        } catch (BadLocationException ex) {
            throw new AssertionError(ex);
        }
        return document;
    }

    private static String text(Document document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }

    private static TextEdit edit(int startLine, int startColumn, int endLine, int endColumn, String text) {
        return new TextEdit(new Range(new org.eclipse.lsp4j.Position(startLine, startColumn),
                new org.eclipse.lsp4j.Position(endLine, endColumn)), text);
    }
}
