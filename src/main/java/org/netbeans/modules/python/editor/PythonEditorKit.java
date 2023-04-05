package org.netbeans.modules.python.editor;

/**
 *
 * @author albilu
 */
import javax.swing.Action;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.TextAction;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.NbEditorKit;
import org.netbeans.modules.python.PythonUtility;

@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = EditorKit.class)
public class PythonEditorKit extends NbEditorKit {

    private static final long serialVersionUID = 1L;

    @Override
    public String getContentType() {
        return PythonUtility.PYTHON_MIME_TYPE;
    }

    @Override
    public Document createDefaultDocument() {
        return new PythonGsfDocument(PythonUtility.PYTHON_MIME_TYPE);
    }

    @Override
    protected Action[] createActions() {
        Action[] actions = new Action[]{new ToggleCommentAction("#")};
        return TextAction.augmentList(super.createActions(), actions);
    }

}
