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
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ExtraLock;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.Utils;
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
        boolean rangeOrDoc = context.startOffset() != 0;
        FileObject fileObject = NbEditorUtilities.getFileObject(document);
        if (fileObject != null) {
            LSPBindings bindings = LSPBindings.getBindings(fileObject);
            if (bindings != null) {
                boolean documentFormatting = Utils.isEnabled(bindings
                        .getInitResult().getCapabilities()
                        .getDocumentFormattingProvider());
                boolean rangeFormatting = Utils.isEnabled(bindings
                        .getInitResult().getCapabilities()
                        .getDocumentRangeFormattingProvider());
                if (rangeFormatting && rangeOrDoc) {
                    rangeFormat(NbEditorUtilities.getFileObject(document), bindings);
                } else if (documentFormatting && !rangeOrDoc) {
                    documentFormat(NbEditorUtilities.getFileObject(document), bindings);
                }
            }
        }

    }

    private void rangeFormat(FileObject fo, LSPBindings bindings)
            throws BadLocationException {
        DocumentRangeFormattingParams drfp = new DocumentRangeFormattingParams();
        drfp.setTextDocument(new TextDocumentIdentifier(Utils.toURI(fo)));
        drfp.setOptions(new FormattingOptions(
                IndentUtils.indentLevelSize(context.document()),
                IndentUtils.isExpandTabs(context.document())));
        drfp.setRange(new Range(
                Utils.createPosition(context.document(), context.startOffset()),
                Utils.createPosition(context.document(), context.endOffset())));
        List<TextEdit> edits = new ArrayList<>();
        try {
            edits = new ArrayList<>(bindings.getTextDocumentService()
                    .rangeFormatting(drfp).get());
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    String.format("LSP document rangeFormat failed for {0}", fo),
                    ex);
        }

        applyTextEdits(edits);
    }

    private void documentFormat(FileObject fo, LSPBindings bindings)
            throws BadLocationException {
        DocumentFormattingParams dfp = new DocumentFormattingParams();
        dfp.setTextDocument(new TextDocumentIdentifier(Utils.toURI(fo)));
        dfp.setOptions(new FormattingOptions(
                IndentUtils.indentLevelSize(context.document()),
                IndentUtils.isExpandTabs(context.document())));
        List<TextEdit> edits = new ArrayList<>();
        try {
            edits.addAll(bindings.getTextDocumentService().formatting(dfp).get());
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    String.format("LSP document format failed for {0}", fo),
                    ex);
        }

        applyTextEdits(edits);
    }

    private void applyTextEdits(List<TextEdit> edits) {
        if (context.document() instanceof StyledDocument) {
            NbDocument.runAtomic((StyledDocument) context.document(), () -> {
                applyEditsNoLock(context.document(), edits, context.startOffset(),
                        context.endOffset());
            });
        } else {
            applyEditsNoLock(context.document(), edits, context.startOffset(),
                    context.endOffset());
        }
    }

    public static void applyEditsNoLock(Document doc, List<? extends TextEdit> edits, Integer startLimit, Integer endLimit) {
        edits
                .stream()
                .sorted(rangeReverseSort)
                .forEach(te -> {
                    try {
                        int start = Utils.getOffset(doc, te.getRange().getStart());
                        int end = Utils.getOffset(doc, te.getRange().getEnd());
                        if ((startLimit == null || start >= startLimit)
                                && (endLimit == null || end >= 0 || endLimit >= 0)) {
                            //https://github.com/apache/netbeans/blob/862379894fc239268e55f00bb4ce337d4c4437b8/ide/lsp.client/src/org/netbeans/modules/lsp/client/Utils.java#L182
                            doc.remove(start, (end - start < 0 || end - start > endLimit) ? endLimit - start : end - start);
                            doc.insertString(start, te.getNewText(), null);
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
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
