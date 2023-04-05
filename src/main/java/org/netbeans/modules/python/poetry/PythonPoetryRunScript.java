package org.netbeans.modules.python.poetry;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.poetry.PythonPoetryRunScript"
)
@ActionRegistration(
        displayName = "#CTL_PythonPoetryRunScript",
        lazy = false,
        asynchronous = true
)
@Messages("CTL_PythonPoetryRunScript=Run")
public final class PythonPoetryRunScript extends AbstractAction {

    PythonProject p;
    String script;

    public PythonPoetryRunScript(PythonProject p, String script) {
        putValue(Action.NAME, script);
        this.p = p;
        this.script = script;
    }

    public PythonPoetryRunScript() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            FileObject projectDirectory = p.getProjectDirectory();
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(FileUtil.toFile(projectDirectory));

            pb.command(PythonUtility.getProjectPythonExe(projectDirectory), "-m", "poetry", "run", script);
            PythonUtility.manageRunEnvs(pb);

            ExecutionService service = ExecutionService.newService(() -> pb.start(),
                    PythonUtility.getExecutorDescriptor(new PythonOutputLine(), () -> {
                    }, () -> {
                    }, false),
                    String.format("Poetry %s (%s)", Bundle.CTL_PythonPoetryRunScript(),
                            script));

            service.run();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
