package org.netbeans.modules.python.editor;

/**
 *
 * @author albilu
 */
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.editor.indent.spi.ReformatTask;

@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = ReformatTask.Factory.class)
public class PythonReformatTaskFactory implements ReformatTask.Factory {

    @Override
    public ReformatTask createTask(Context context) {
        return new PythonReformatTask(context);
    }

}
