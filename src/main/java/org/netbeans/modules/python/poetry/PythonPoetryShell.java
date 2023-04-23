package org.netbeans.modules.python.poetry;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.repl.PythonVEnvConsoleActionTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.poetry.PythonPoetryShell"
)
@ActionRegistration(
        displayName = "#CTL_PythonPoetryShell",
        lazy = false,
        asynchronous = true
)
@Messages("CTL_PythonPoetryShell=Shell")
public final class PythonPoetryShell extends AbstractAction {

    PythonProject p;

    public PythonPoetryShell(PythonProject p) {
        putValue(Action.NAME, Bundle.CTL_PythonPoetryShell());
        this.p = p;
    }

    public PythonPoetryShell() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PythonVEnvConsoleActionTopComponent pythonVEnvConsoleActionTopComponent
                = new PythonVEnvConsoleActionTopComponent(p, PythonProject.POETRY);
        pythonVEnvConsoleActionTopComponent.open();
        pythonVEnvConsoleActionTopComponent.requestActive();
    }
}
