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
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.actions.PythonCleanVEnvAction"
)
@ActionRegistration(
        displayName = "#CTL_PythonCleanVEnvAction",
        lazy = false,
        asynchronous = true
)
@NbBundle.Messages({
    "CTL_PythonCleanVEnvAction=Clean VENV",
    "CTL_PythonCleanVEnvMessage=Cleaning"
})
public final class PythonCleanVEnvAction extends AbstractAction
        implements LookupListener, ContextAwareAction {

    public static final Logger LOG = Logger.getLogger(PythonCleanVEnvAction.class.getName());

    private static final long serialVersionUID = 1L;

    private final Lookup context;
    Lookup.Result<PythonProject> lkpInfo;

    public PythonCleanVEnvAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PythonCleanVEnvAction(Lookup context) {
        putValue(Action.NAME, Bundle.CTL_PythonCleanVEnvAction());
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
        return new PythonCleanVEnvAction(context);
    }

    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        PythonProject get = lkpInfo.allInstances().stream().findFirst().get();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(get.getProjectDirectory()/*.getParent()*/));
        PythonUtility.manageRunEnvs(pb);
        String virtualmanager = PythonUtility.getVenv(get);

        List<String> argList = new ArrayList<>();
        try {
            argList.addAll(Arrays.asList(PythonUtility
                    .getPlatformPythonExe(),
                    "-m",
                    virtualmanager,
                    "--clear",
                    Paths.get(get.getProjectDirectory().getFileObject(".venv").getPath()).toString())
            );
            pb.command(argList);
            LOG.info(() -> Arrays.toString(argList.toArray()));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        ExecutionService service = ExecutionService
                .newService(() -> pb.start(),
                        PythonUtility.getExecutorDescriptor(new PythonOutputLine(),
                                () -> {
                                    StatusDisplayer.getDefault()
                                            .setStatusText(String.format("%s %s",
                                                    Bundle.CTL_PythonCleanVEnvMessage(), virtualmanager));
                                    LifecycleManager.getDefault().saveAll();
                                }, () -> {
                                }, true, true),
                        String.format("%s %s (%s)", "Clean",
                                virtualmanager, get.getProjectDirectory().getNameExt())
                );

        service.run();
    }
}
