package org.netbeans.modules.python.tasklist;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.modules.editor.hints.AnnotationHolder;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.tasklist.FileTaskScanner;
import org.netbeans.spi.tasklist.Task;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = FileTaskScanner.class, path = "TaskList/Scanners")
public class PythonActionItems extends FileTaskScanner {

    private static final String TASKLIST_ERROR = "nb-tasklist-error";
    private static final String TASKLIST_WARNING = "nb-tasklist-warning";

    public PythonActionItems() {
        super("Python Hints", "Python Files Hints", null);
    }

    @Override
    public List<? extends Task> scan(FileObject resource) {
        //FIXME-EXTERNAL: PythonDiagCollector is a hack as Full project diagnostic is not available yet with pylsp see:
        ////https://github.com/python-lsp/python-lsp-server/discussions/280
        //For now Full project Action items will report flake8 based diagnostics (Disabled)
        //This feature will also serve for Error badge
        //StatusDisplayer.getDefault().setStatusText("Project ActionsItems not available yet!");
        if (!resource.getExt().equals("py") || StringUtils.containsAny(
                Paths.get(resource.getPath()).toString(), PythonUtility.EXCLUDED_DIRS)) {
            return Collections.EMPTY_LIST;
        }
        AnnotationHolder ah = AnnotationHolder.getInstance(resource);
        if (ah == null) {
            return Collections.EMPTY_LIST;
        }
        List<ErrorDescription> errors = ah.getErrors();
        List<Task> tasks = new ArrayList<>();
        if (/*PythonDiagCollector.errors*/errors != null) {
            /*PythonDiagCollector.errors*/
            errors.forEach(error -> {
                try {
                    Task task = Task.create(error.getFile(),
                            severityToTaskListString(error.getSeverity()),
                            error.getDescription(),
                            error.getRange().getBegin().getLine() + 1);

                    tasks.add(task);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            });
        }
        return tasks;
    }

    private static String severityToTaskListString(Severity severity) {
        if (severity == Severity.ERROR) {
            return TASKLIST_ERROR;
        }
        return TASKLIST_WARNING;
    }

    @Override
    public void attach(Callback callback) {
        if (callback != null) {
            callback.refreshAll();
        }
    }

    @Override
    public void notifyFinish() {
        super.notifyFinish();
    }

    @Override
    public void notifyPrepare() {
        super.notifyPrepare();
    }

}
