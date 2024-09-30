package org.netbeans.modules.python.actions;

/**
 *
 * @author albilu
 */
import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.repl.PythonVEnvConsoleActionTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 *
 * @author albilu
 */
@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.actions.PythonVEnvConsoleAction"
)
@ActionRegistration(
        displayName = "#CTL_PythonVEnvConsoleAction",
        lazy = false,
        asynchronous = true
)
@NbBundle.Messages("CTL_PythonVEnvConsoleAction=VENV Console")
public final class PythonVEnvConsoleAction extends AbstractAction
        implements LookupListener, ContextAwareAction {

    private static final long serialVersionUID = 1L;

    private final Lookup context;
    Lookup.Result<PythonProject> lkpInfo;

    public PythonVEnvConsoleAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PythonVEnvConsoleAction(Lookup context) {
        putValue(Action.NAME, Bundle.CTL_PythonVEnvConsoleAction());
        this.context = context;
    }

    void init() {
        assert SwingUtilities.isEventDispatchThread() : "this shall be called just from AWT thread";
        if (lkpInfo != null) {
            return;
        }
        //The thing we want to listen for the presence or absence of
        //on the global selection
        lkpInfo = context.lookupResult(PythonProject.class);
        lkpInfo.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Collection<? extends PythonProject> allInstances = lkpInfo.allInstances();
        if (allInstances.isEmpty()) {
            setEnabled(false);
        } else if (allInstances.size() == 1 && allInstances.stream().findFirst()
                .get().getProjectDirectory().getFileObject(".venv") != null) {
            setEnabled(true);
            return;
        }
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PythonVEnvConsoleAction(context);
    }

    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        PythonProject get = lkpInfo.allInstances().stream().findFirst().get();
        PythonVEnvConsoleActionTopComponent pythonVEnvConsoleActionTopComponent
                = new PythonVEnvConsoleActionTopComponent(get, "venv");
        for (Mode mode : WindowManager.getDefault().getModes()) {
            if (mode.getName().equals("output") && mode.canDock(pythonVEnvConsoleActionTopComponent)) {
                mode.dockInto(pythonVEnvConsoleActionTopComponent);
                break;
            }
        }
        pythonVEnvConsoleActionTopComponent.open();
        pythonVEnvConsoleActionTopComponent.requestActive();
    }
}
