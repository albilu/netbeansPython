package org.netbeans.modules.python.editor;

/**
 *
 * @author albilu
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.Utils;
import org.netbeans.modules.python.PythonUtility;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

public class PythonReformatTask
        implements org.netbeans.modules.editor.indent.spi.ReformatTask {

    private final Context context;

    public PythonReformatTask(Context context) {
        this.context = context;
    }

    @Override
    public void reformat() throws BadLocationException {
        Document document = context.document();
        boolean rangeOrDoc = context.startOffset() != 0
                || context.endOffset() != document.getLength();
        FileObject fileObject = NbEditorUtilities.getFileObject(document);
        if (fileObject != null && !StringUtils.containsAny(fileObject.getPath(),
                PythonUtility.EXCLUDED_DIRS)) {
            List<LSPBindings> bindings = LSPBindings.getBindings(fileObject);
            for (LSPBindings binding : bindings) {
                boolean documentFormatting = Utils.isEnabled(binding
                        .getInitResult().getCapabilities()
                        .getDocumentFormattingProvider());
                boolean rangeFormatting = Utils.isEnabled(binding
                        .getInitResult().getCapabilities()
                        .getDocumentRangeFormattingProvider());
                if (rangeFormatting && rangeOrDoc) {
                    rangeFormat(NbEditorUtilities.getFileObject(document), binding);
                } else if (documentFormatting && !rangeOrDoc) {
                    documentFormat(NbEditorUtilities.getFileObject(document), binding);
                }
            }
        }

    }

    /** The range sent to the server, and the whole lines its edits may change. */
    record SelectedLines(int requestStart, int requestEnd, int changeStart, int changeEnd) {
    }

    static SelectedLines selectedLines(Document document, int start, int end) {
        Element root = document.getDefaultRootElement();
        // A selection ending at the start of a line does not include that line.
        // Formatters round ranges to whole lines and would otherwise format it.
        if (end > start && root.getElement(root.getElementIndex(end)).getStartOffset() == end) {
            end--;
        }
        int firstLineStart = root.getElement(root.getElementIndex(start)).getStartOffset();
        int lastLineEnd = Math.min(document.getLength(),
                root.getElement(root.getElementIndex(end)).getEndOffset());
        return new SelectedLines(start, end, firstLineStart, lastLineEnd);
    }

    private void rangeFormat(FileObject fo, LSPBindings bindings)
            throws BadLocationException {
        SelectedLines lines = selectedLines(context.document(), context.startOffset(), context.endOffset());
        DocumentRangeFormattingParams drfp = new DocumentRangeFormattingParams();
        drfp.setTextDocument(new TextDocumentIdentifier(Utils.toURI(fo)));
        drfp.setOptions(new FormattingOptions(
                IndentUtils.indentLevelSize(context.document()),
                IndentUtils.isExpandTabs(context.document())));
        drfp.setRange(new Range(
                Utils.createPosition(context.document(), lines.requestStart()),
                Utils.createPosition(context.document(), lines.requestEnd())));
        List<? extends TextEdit> edits = new ArrayList<>();
        try {
            edits = bindings.getTextDocumentService().rangeFormatting(drfp).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "LSP document rangeFormat failed for " + fo,
                    ex);
        }

        applyTextEdits(edits, lines.changeStart(), lines.changeEnd());
    }

    private void documentFormat(FileObject fo, LSPBindings bindings)
            throws BadLocationException {
        DocumentFormattingParams dfp = new DocumentFormattingParams();
        dfp.setTextDocument(new TextDocumentIdentifier(Utils.toURI(fo)));
        dfp.setOptions(new FormattingOptions(
                IndentUtils.indentLevelSize(context.document()),
                IndentUtils.isExpandTabs(context.document())));
        List<? extends TextEdit> edits = new ArrayList<>();
        try {
            edits = bindings.getTextDocumentService().formatting(dfp).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "LSP document format failed for " + fo,
                    ex);
        }

        applyTextEdits(edits, context.startOffset(), context.endOffset());
    }

    private void applyTextEdits(List<? extends TextEdit> edits, int startLimit, int endLimit) {
        if (context.document() instanceof StyledDocument) {
            NbDocument.runAtomic((StyledDocument) context.document(), () -> {
                applyEditsNoLock(context.document(), edits, startLimit, endLimit);
            });
        } else {
            applyEditsNoLock(context.document(), edits, startLimit, endLimit);
        }
    }

    public static void applyEditsNoLock(Document doc, List<? extends TextEdit> edits, Integer startLimit, Integer endLimit) {
        if (edits == null || edits.isEmpty()) {
            return;
        }
        List<CaretLocation> carets = new ArrayList<>();
        for (JTextComponent editor : EditorRegistry.componentList()) {
            if (editor.getDocument() == doc) {
                carets.add(new CaretLocation(editor));
            }
        }
        edits
                .stream()
                .sorted(rangeReverseSort)
                .forEach(te -> {
                    try {
                        int start = editOffset(doc, te.getRange().getStart());
                        int end = editOffset(doc, te.getRange().getEnd());
                        if (start < 0 || end < start || end > doc.getLength()) {
                            return;
                        }
                        // Formatters replace whole lines or the whole document. Apply
                        // only the changed text: limits then apply to real changes, and
                        // unchanged code keeps its positions and annotations.
                        String original = doc.getText(start, end - start);
                        String replacement = te.getNewText() == null ? "" : te.getNewText();
                        int prefix = commonPrefix(original, replacement);
                        int suffix = commonSuffix(original, replacement, prefix);
                        int changeStart = start + prefix;
                        int changeEnd = end - suffix;
                        String text = replacement.substring(prefix, replacement.length() - suffix);
                        if ((changeStart == changeEnd && text.isEmpty())
                                || (startLimit != null && changeStart < startLimit)
                                || (endLimit != null && changeEnd > endLimit)) {
                            return;
                        }
                        carets.forEach(caret -> caret.replacing(changeStart, changeEnd));
                        doc.remove(changeStart, changeEnd - changeStart);
                        doc.insertString(changeStart, text, null);
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
        carets.forEach(CaretLocation::restore);
    }

    static int commonPrefix(String original, String replacement) {
        int max = Math.min(original.length(), replacement.length());
        int length = 0;
        while (length < max && original.charAt(length) == replacement.charAt(length)) {
            length++;
        }
        // Never split a surrogate pair between unchanged and replaced text.
        return length > 0 && Character.isHighSurrogate(original.charAt(length - 1)) ? length - 1 : length;
    }

    static int commonSuffix(String original, String replacement, int prefix) {
        int max = Math.min(original.length(), replacement.length()) - prefix;
        int length = 0;
        while (length < max && original.charAt(original.length() - 1 - length)
                == replacement.charAt(replacement.length() - 1 - length)) {
            length++;
        }
        return length > 0 && Character.isLowSurrogate(original.charAt(original.length() - length)) ? length - 1 : length;
    }

    private static int editOffset(Document document, org.eclipse.lsp4j.Position position) {
        if (position.getLine() < 0 || position.getCharacter() < 0) {
            return -1;
        }
        Element root = document.getDefaultRootElement();
        // Python formatters use the following line as EOF even when the last
        // line has no newline. LSP positions beyond a line/document are clamped.
        if (position.getLine() >= root.getElementCount()) {
            return document.getLength();
        }
        Element line = root.getElement(position.getLine());
        int end = Math.min(document.getLength(), line.getEndOffset() - 1);
        return line.getStartOffset() + Math.min(position.getCharacter(), end - line.getStartOffset());
    }

    /** Keep the cursor and selection on their original lines when a server
     * replaces the whole document. Swing otherwise moves them to its end. */
    private static final class CaretLocation {
        private final JTextComponent editor;
        private final Document document;
        private final int dotLine;
        private final int dotColumn;
        private final int markLine;
        private final int markColumn;
        private boolean dotReplaced;
        private boolean markReplaced;

        CaretLocation(JTextComponent editor) {
            this.editor = editor;
            document = editor.getDocument();
            Element root = document.getDefaultRootElement();
            int dot = editor.getCaret().getDot();
            int mark = editor.getCaret().getMark();
            dotLine = root.getElementIndex(dot);
            dotColumn = dot - root.getElement(dotLine).getStartOffset();
            markLine = root.getElementIndex(mark);
            markColumn = mark - root.getElement(markLine).getStartOffset();
        }

        void replacing(int start, int end) {
            int dot = editor.getCaret().getDot();
            int mark = editor.getCaret().getMark();
            dotReplaced |= start <= dot && dot < end;
            markReplaced |= start <= mark && mark < end;
        }

        private int offset(int line, int column) {
            Element root = document.getDefaultRootElement();
            Element element = root.getElement(Math.min(line, root.getElementCount() - 1));
            return Math.min(element.getStartOffset() + column,
                    Math.min(document.getLength(), element.getEndOffset() - 1));
        }

        void restore() {
            if (!dotReplaced && !markReplaced) {
                return;
            }
            int dotAfterEdit = editor.getCaret().getDot();
            int markAfterEdit = editor.getCaret().getMark();
            Runnable update = () -> {
                if (editor.getDocument() == document
                        && editor.getCaret().getDot() == dotAfterEdit
                        && editor.getCaret().getMark() == markAfterEdit) {
                    editor.getCaret().setDot(markReplaced ? offset(markLine, markColumn) : markAfterEdit);
                    editor.getCaret().moveDot(dotReplaced ? offset(dotLine, dotColumn) : dotAfterEdit);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                update.run();
            } else {
                SwingUtilities.invokeLater(update);
            }
        }
    }

    private static final Comparator<TextEdit> rangeReverseSort = (s1, s2) -> {
        int l1 = s1.getRange().getEnd().getLine();
        int l2 = s2.getRange().getEnd().getLine();
        int c1 = s1.getRange().getEnd().getCharacter();
        int c2 = s2.getRange().getEnd().getCharacter();
        if (l1 != l2) {
            return l2 - l1;
        } else {
            return c2 - c1;
        }
    };

    @Override
    public ExtraLock reformatLock() {
        return null;
    }

}
