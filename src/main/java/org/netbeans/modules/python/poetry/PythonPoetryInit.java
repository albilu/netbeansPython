package org.netbeans.modules.python.poetry;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonUtility;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.poetry.PythonPoetryInit"
)
@ActionRegistration(
        displayName = "#CTL_PythonPoetryInit",
        lazy = false,
        asynchronous = true
)
@Messages("CTL_PythonPoetryInit=Init")
public final class PythonPoetryInit extends AbstractAction {

    DataObject dataObject;

    public PythonPoetryInit(DataObject dataObject) {
        putValue(Action.NAME, Bundle.CTL_PythonPoetryInit());
        this.dataObject = dataObject;
    }

    public PythonPoetryInit() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            FileObject fileObject = dataObject.getPrimaryFile();

            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(FileUtil.toFile(fileObject));

            pb.command(PythonUtility.getPlatformPythonExe(), "-m", "poetry", "init");
            PythonUtility.manageRunEnvs(pb);

            ExecutionService service = ExecutionService.newService(() -> pb.start(),
                    PythonUtility.getExecutorDescriptor(new PythonOutputLine(), () -> {
                    }, () -> {
                    }, false),
                    String.format("Poetry %s (%s)", Bundle.CTL_PythonPoetryInit(),
                            fileObject.getName()));

            service.run();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }
}
