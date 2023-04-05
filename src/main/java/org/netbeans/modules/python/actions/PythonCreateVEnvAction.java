package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.ui.ProjectProblems;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.actions.PythonCreateVEnvAction"
)
@ActionRegistration(
        displayName = "#CTL_PythonCreateVEnvAction",
        lazy = false,
        asynchronous = true
)
@Messages({
    "CTL_PythonCreateVEnvAction=Create VENV",
    "CTL_PythonCreateVEnvMessage=Creating"
})
public final class PythonCreateVEnvAction extends AbstractAction
        implements LookupListener, ContextAwareAction {

    public static final Logger LOG = Logger.getLogger(PythonCreateVEnvAction.class.getName());

    private static final long serialVersionUID = 1L;

    private final Lookup context;
    Lookup.Result<PythonProject> lkpInfo;

    public PythonCreateVEnvAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PythonCreateVEnvAction(Lookup context) {
        putValue(Action.NAME, Bundle.CTL_PythonCreateVEnvAction());
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
                .get().getProjectDirectory().getFileObject(".venv") == null) {
            setEnabled(true);
            return;
        }
        setEnabled(false);
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PythonCreateVEnvAction(context);
    }

    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        PythonProject get = lkpInfo.allInstances().stream().findFirst().get();
        try {
            createEnv(get, PythonUtility
                    .getPlatformPythonExe(), PythonUtility.getVenv(get));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void createEnv(PythonProject get, String python, String virtualmanager) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(get.getProjectDirectory()/*.getParent()*/));
        PythonUtility.manageRunEnvs(pb);

        List<String> argList = new ArrayList<>();
        argList.addAll(Arrays.asList(/*osShell[1],*/python,
                "-m",
                virtualmanager,
                Paths.get(get.getProjectDirectory().getPath()).resolve(".venv").toString())
        );
        pb.command(argList);

        LOG.info(() -> Arrays.toString(argList.toArray()));

        ExecutionService service = ExecutionService.newService(() -> pb.start(),
                PythonUtility.getExecutorDescriptor(
                        new PythonOutputLine(),
                        () -> {
                            StatusDisplayer.getDefault()
                                    .setStatusText(String.format("%s %s",
                                            Bundle.CTL_PythonCreateVEnvMessage(), virtualmanager));
                            LifecycleManager.getDefault().saveAll();
                        },
                        () -> {
                            if (ProjectProblems.isBroken(get)) {
                                ProjectProblems.showAlert(get);
                            }
                        }, true),
                String.format("%s %s (%s)", Bundle.CTL_PythonCreateVEnvMessage(),
                        virtualmanager, get.getProjectDirectory().getNameExt()));

        service.run();
    }
}
