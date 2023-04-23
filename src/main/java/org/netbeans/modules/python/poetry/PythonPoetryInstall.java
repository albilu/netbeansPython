package org.netbeans.modules.python.poetry;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.poetry.PythonPoetryInstall"
)
@ActionRegistration(
        displayName = "#CTL_PythonPoetryInstall",
        lazy = false,
        asynchronous = true
)
@Messages("CTL_PythonPoetryInstall=Install")
public final class PythonPoetryInstall extends AbstractAction {

    PythonProject p;

    public PythonPoetryInstall(PythonProject p) {
        putValue(Action.NAME, Bundle.CTL_PythonPoetryInstall());
        this.p = p;
    }

    public PythonPoetryInstall() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            FileObject projectDirectory = p.getProjectDirectory();
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(FileUtil.toFile(projectDirectory));

            pb.command(PythonUtility.getProjectPythonExe(projectDirectory), "-m", "poetry", "install");
            PythonUtility.manageRunEnvs(pb);

            ExecutionService service = ExecutionService.newService(() -> pb.start(),
                    PythonUtility.getExecutorDescriptor(new PythonOutputLine(), () -> {
                    }, () -> {
                    }, false, true),
                    String.format("Poetry %s (%s)", Bundle.CTL_PythonPoetryInstall(),
                            ProjectUtils.getInformation(p).getDisplayName()));

            service.run();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
