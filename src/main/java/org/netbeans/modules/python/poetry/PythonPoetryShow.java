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
        id = "org.netbeans.modules.python.poetry.PythonPoetryShow"
)
@ActionRegistration(
        displayName = "#CTL_PythonPoetryShow",
        lazy = false,
        asynchronous = true
)
@Messages("CTL_PythonPoetryShow=Show Packages")
public final class PythonPoetryShow extends AbstractAction {

    PythonProject p;

    public PythonPoetryShow(PythonProject p) {
        putValue(Action.NAME, Bundle.CTL_PythonPoetryShow());
        this.p = p;
    }

    public PythonPoetryShow() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            FileObject projectDirectory = p.getProjectDirectory();
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(FileUtil.toFile(projectDirectory));

            pb.command(PythonUtility.getProjectPythonExe(projectDirectory), "-m", "poetry", "show", "--tree");
            PythonUtility.manageRunEnvs(pb);

            ExecutionService service = ExecutionService.newService(() -> pb.start(),
                    PythonUtility.getExecutorDescriptor(new PythonOutputLine(), () -> {
                    }, () -> {
                    }, false, true),
                    String.format("Poetry %s (%s)", Bundle.CTL_PythonPoetryShow(),
                            ProjectUtils.getInformation(p).getDisplayName()));

            service.run();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
