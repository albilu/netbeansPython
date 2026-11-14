package org.netbeans.modules.python.editor;

/**
 *
 * @author albilu
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
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
import org.netbeans.api.editor.caret.CaretInfo;
import org.netbeans.api.editor.caret.EditorCaret;
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
        List<JTextComponent> editors = new ArrayList<>();
        for (JTextComponent editor : EditorRegistry.componentList()) {
            if (editor.getDocument() == doc) {
                editors.add(editor);
            }
        }
        applyEdits(doc, edits, startLimit, endLimit, editors);
    }

    /** Applies the edits within the limits, keeping the carets of the editors at their code. */
    static void applyEdits(Document doc, List<? extends TextEdit> edits, Integer startLimit, Integer endLimit,
            List<JTextComponent> editors) {
        if (edits == null || edits.isEmpty()) {
            return;
        }
        try {
            String text = doc.getText(0, doc.getLength());
            String formatted = FormatterEdits.apply(text, edits);
            if (formatted == null) {
                Logger.getLogger(PythonReformatTask.class.getName()).log(Level.INFO,
                        "Ignoring invalid or overlapping formatting edits: {0}", edits);
                return;
            }
            List<FormatterEdits.Change> changes = FormatterEdits.changes(text, formatted, startLimit, endLimit);
            if (changes.isEmpty()) {
                return;
            }
            List<Carets> carets = new ArrayList<>();
            for (JTextComponent editor : editors) {
                carets.add(new Carets(editor));
            }
            // Only the changed text is replaced: unchanged code keeps its
            // positions and annotations.
            for (int k = changes.size() - 1; k >= 0; k--) {
                FormatterEdits.Change change = changes.get(k);
                if (change.end() > change.start()) {
                    doc.remove(change.start(), change.end() - change.start());
                }
                if (!change.text().isEmpty()) {
                    doc.insertString(change.start(), change.text(), null);
                }
            }
            for (Carets caret : carets) {
                caret.restore(text, changes);
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * The carets of an editor, kept at their code when formatting replaces
     * the text around them. Swing would otherwise move them to the end of
     * the replaced text, e.g. the end of the document.
     */
    private static final class Carets {
        private final JTextComponent editor;
        private final int[] original;

        Carets(JTextComponent editor) {
            this.editor = editor;
            this.original = positions(editor);
        }

        void restore(String text, List<FormatterEdits.Change> changes) {
            int[] target = new int[original.length];
            for (int k = 0; k < original.length; k++) {
                target[k] = FormatterEdits.map(text, changes, original[k]);
            }
            int[] afterEdit = positions(editor);
            if (Arrays.equals(target, afterEdit)) {
                return;
            }
            Document document = editor.getDocument();
            Runnable update = () -> {
                // Unless the carets were moved meanwhile.
                if (editor.getDocument() == document && Arrays.equals(positions(editor), afterEdit)) {
                    setPositions(editor, target);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                update.run();
            } else {
                SwingUtilities.invokeLater(update);
            }
        }

        /** Dot and mark of each caret, in the order of the editor's carets. */
        private static int[] positions(JTextComponent editor) {
            Caret caret = editor.getCaret();
            if (caret instanceof EditorCaret) {
                List<CaretInfo> carets = ((EditorCaret) caret).getCarets();
                int[] positions = new int[2 * carets.size()];
                for (int k = 0; k < carets.size(); k++) {
                    positions[2 * k] = carets.get(k).getDot();
                    positions[2 * k + 1] = carets.get(k).getMark();
                }
                return positions;
            }
            return caret == null ? new int[0] : new int[]{caret.getDot(), caret.getMark()};
        }

        private static void setPositions(JTextComponent editor, int[] positions) {
            Caret caret = editor.getCaret();
            if (caret instanceof EditorCaret) {
                // EditorCaret.setDot() would remove all carets but the last one,
                // and carets that met in replaced text are already merged.
                Document document = editor.getDocument();
                List<Position> dotsAndMarks = new ArrayList<>();
                try {
                    for (int position : positions) {
                        dotsAndMarks.add(document.createPosition(position));
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                    return;
                }
                ((EditorCaret) caret).replaceCarets(dotsAndMarks, null);
            } else if (caret != null) {
                caret.setDot(positions[1]);
                caret.moveDot(positions[0]);
            }
        }
    }

    @Override
    public ExtraLock reformatLock() {
        return null;
    }

}
