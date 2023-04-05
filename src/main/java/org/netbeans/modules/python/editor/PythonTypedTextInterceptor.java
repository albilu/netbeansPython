package org.netbeans.modules.python.editor;

import javax.swing.text.BadLocationException;
import org.netbeans.api.editor.document.LineDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

//Copy from https://github.com/apache/netbeans/blob/master/java/languages.antlr/src/org/netbeans/modules/languages/antlr/AntlrTypedTextInterceptor.java
public class PythonTypedTextInterceptor implements TypedTextInterceptor {

    private int caretPosition = -1;

    @Override
    public boolean beforeInsert(Context context) throws BadLocationException {
        return false;
    }

    @Override
    public void insert(MutableContext context) throws BadLocationException {
        String txt = context.getText();
        if (context.getReplacedText().length() == 0) {
            switch (txt) {
                case "{":
                    context.setText("{}", 1);
                    break;
                case "}":
                    if ("}".equals(textAfter(context, 1))) {
                        skipNext(context);
                    }
                    break;
                case "[":
                    context.setText("[]", 1);
                    break;
                case "]":
                    if ("]".equals(textAfter(context, 1))) {
                        skipNext(context);
                    }
                    break;
                case "(":
                    context.setText("()", 1);
                    break;
                case ")":
                    if (")".equals(textAfter(context, 1))) {
                        skipNext(context);
                    }
                    break;
                case " ":
                    String b = textBefore(context, 1);
                    String a = textAfter(context, 1);
                    if (("{".equals(b) && "}".equals(a))
                            || ("[".equals(b) && "]".equals(a))) {
                        context.setText("  ", 1);
                    }
                    break;
                case "\"":
                    if ("\"".equals(textAfter(context, 1))) {
                        skipNext(context);
                    } else {
                        int quotes = quotesInLine(context, '"');
                        if (quotes % 2 == 0) {
                            context.setText("\"\"", 1);
                        }
                    }
                    break;
                case "'":
                    if ("'".equals(textAfter(context, 1))) {
                        skipNext(context);
                    } else {
                        int quotes = quotesInLine(context, '\'');
                        if (quotes % 2 == 0) {
                            context.setText("''", 1);
                        }
                    }
                    break;
            }
        }
    }

    private void skipNext(MutableContext context) {
        context.setText("", 0);
        caretPosition = context.getOffset() + 1;
    }

    private static String textAfter(Context context, int length) throws BadLocationException {
        int next = Math.min(length, context.getDocument().getLength() - context.getOffset());
        return context.getDocument().getText(context.getOffset(), next);
    }

    private static String textBefore(Context context, int lenght) throws BadLocationException {
        int pre = Math.min(lenght, context.getOffset());
        return context.getDocument().getText(context.getOffset() - pre, pre);
    }

    private static int quotesInLine(Context context, char quote) throws BadLocationException {
        LineDocument doc = (LineDocument) context.getDocument();
        int lineStart = LineDocumentUtils.getLineStart(doc, context.getOffset());
        int lineEnd = LineDocumentUtils.getLineEnd(doc, context.getOffset());
        char[] line = doc.getText(lineStart, lineEnd - lineStart).toCharArray();

        int quotes = 0;
        for (int i = 0; i < line.length; i++) {
            char d = line[i];
            if ('\\' == d) {
                i++;
                continue;
            }
            if (quote == d) {
                quotes++;
            }
        }
        return quotes;
    }

    @Override
    public void afterInsert(Context context) throws BadLocationException {
        if (caretPosition > -1) {
            context.getComponent().setCaretPosition(caretPosition);
            caretPosition = -1;
        }
    }

    @Override
    public void cancelled(Context context) {
    }

}
