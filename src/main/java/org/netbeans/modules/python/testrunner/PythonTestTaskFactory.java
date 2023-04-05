package org.netbeans.modules.python.testrunner;

/**
 *
 * @author albilu
 */
import java.util.Collection;
import java.util.Collections;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.modules.python.PythonUtility;

@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = TaskFactory.class)
public class PythonTestTaskFactory extends TaskFactory {

    @Override
    public Collection create(Snapshot snapshot) {
        return Collections.singleton(new PythonTestParserTask());
    }

}
