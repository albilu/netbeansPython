package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/**
 *
 * @author albilu
 */
@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.actions.PythonVEnvMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_PythonVEnvMenuAction",
        lazy = false,
        asynchronous = true
)
@NbBundle.Messages("CTL_PythonVEnvMenuAction=Virtual Environment")
public final class PythonVEnvMenuAction extends AbstractAction
        implements ActionListener, Presenter.Popup {

    private static final long serialVersionUID = 1L;

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public JMenuItem getPopupPresenter() {
        JMenu main = new JMenu(Bundle.CTL_PythonVEnvMenuAction());
        main.add(new PythonCreateVEnvAction());
        main.add(new PythonCleanVEnvAction());
        main.add(new PythonVEnvConsoleAction());
        return main;
    }
}
