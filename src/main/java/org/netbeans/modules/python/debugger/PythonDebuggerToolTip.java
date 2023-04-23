package org.netbeans.modules.python.debugger;

import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.text.Annotation;
import org.openide.text.DataEditorSupport;
import org.openide.text.Line;
import org.openide.text.Line.Part;
import org.openide.text.NbDocument;
import org.openide.util.RequestProcessor;

public class PythonDebuggerToolTip extends Annotation implements Runnable {

    private Part lp;
    private EditorCookie ec;

    @Override
    public String getShortDescription() {
        DebuggerEngine currentEngine = DebuggerManager.getDebuggerManager().
                getCurrentEngine();
        if (currentEngine == null) {
            return null;
        }
        PythonDebugger d = currentEngine.lookupFirst(null, PythonDebugger.class);
        if (d == null) {
            return null;
        }

        Part partLine = (Part) getAttachedAnnotatable();
        if (partLine == null) {
            return null;
        }
        Line line = partLine.getLine();
        DataObject dob = DataEditorSupport.findDataObject(line);
        if (dob == null) {
            return null;
        }
        EditorCookie editCookie = dob.getLookup().lookup(EditorCookie.class);

        if (editCookie == null) {
            return null;
        }
        this.lp = partLine;
        this.ec = editCookie;
        RequestProcessor.getDefault().post(this);
        return null;
    }

    @Override
    public void run() {
        //if (expression == null) return;
        if (lp == null || ec == null) {
            return;
        }
        StyledDocument doc;
        try {
            doc = ec.openDocument();
        } catch (IOException ex) {
            return;
        }
        JEditorPane ep = EditorContextDispatcher.getDefault().getCurrentEditor();
        if (ep == null) {
            return;
        }
        String expression = getIdentifier(doc, ep, NbDocument.
                findLineOffset(doc, lp.getLine().getLineNumber()) + lp.getColumn());
        if (expression == null) {
            return;
        }
        DebuggerEngine currentEngine = DebuggerManager.getDebuggerManager().
                getCurrentEngine();
        if (currentEngine == null) {
            return;
        }
        PythonDebugger d = currentEngine.lookupFirst(null, PythonDebugger.class);
        if (d == null /*|| (frame = d.getCurrentFrame()) == null*/) {
            return;
        }
        try {

            String command = String.format("%s %s ;; %s", "whatis", expression, expression);
            String value = d.getPdbClient().sendCommandAndGetResponse(command);

            if (/*!value.equals(expression)*/!value.isEmpty()) {
                String toolTipText;
                //String type = d.getPdbClient().sendCommandAndGetResponse(String.format("%s %s", "whatis", value));
                //if (!type.isEmpty()) {
                //toolTipText = expression + " = (" + type + ") " + value;
                //} else {
                //toolTipText = expression + " = " + value;
                //}
                toolTipText = expression + " = " + value;
                firePropertyChange(PROP_SHORT_DESCRIPTION, null, toolTipText);
            }

        } catch (IOException e) {

        }

    }

    @Override
    public String getAnnotationType() {
        return null; // Currently return null annotation type
    }

    private static String getIdentifier(StyledDocument doc, JEditorPane ep, int offset) {
        String t = null;
        if ((ep.getSelectionStart() <= offset)
                && (offset <= ep.getSelectionEnd())) {
            t = ep.getSelectedText();
        }
        if (t != null) {
            return t;
        }

        int line = NbDocument.findLineNumber(doc, offset);
        int col = NbDocument.findLineColumn(doc, offset);
        try {
            Element lineElem = NbDocument.findLineRootElement(doc).getElement(line);

            if (lineElem == null) {
                return null;
            }
            int lineStartOffset = lineElem.getStartOffset();
            int lineLen = lineElem.getEndOffset() - lineStartOffset;
            t = doc.getText(lineStartOffset, lineLen);
            lineLen = t.length();
            int identStart = col;
            while (identStart > 0
                    && (Character.isJavaIdentifierPart(t.charAt(identStart - 1))
                    || (t.charAt(identStart - 1) == '.'))) {
                identStart--;
            }
            int identEnd = col;
            while (identEnd < lineLen
                    && Character.isJavaIdentifierPart(t.charAt(identEnd))) {
                identEnd++;
            }
            if (identStart == identEnd) {
                return null;
            }
            String ident = t.substring(identStart, identEnd);
            while (identEnd < lineLen
                    && Character.isWhitespace(t.charAt(identEnd))) {
                identEnd++;
            }
            return ident;
        } catch (BadLocationException e) {
            return null;
        }
    }

}
