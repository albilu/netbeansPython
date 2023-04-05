package org.netbeans.modules.python.editor;

import org.netbeans.modules.csl.api.GsfLanguage;
import org.netbeans.modules.csl.core.Language;
import org.netbeans.modules.csl.core.LanguageRegistry;
import org.netbeans.modules.editor.NbEditorDocument;
import org.netbeans.modules.python.PythonUtility;

/**
 *
 * @author albilu
 */
public class PythonGsfDocument extends NbEditorDocument {

    private GsfLanguage language = null;

    public PythonGsfDocument(String mimeType) {
        super(mimeType);
    }

    @Override
    public boolean isIdentifierPart(char ch) {
        if (language == null) {
            Language l = LanguageRegistry.getInstance().getLanguageByMimeType(PythonUtility.PYTHON_MIME_TYPE);
            if (l != null) {
                language = l.getGsfLanguage();
            }
        }
        return language != null ? language.isIdentifierChar(ch)
                : (Character.toString(ch).equals("_") ? true : super.isIdentifierPart(ch));
    }

}
