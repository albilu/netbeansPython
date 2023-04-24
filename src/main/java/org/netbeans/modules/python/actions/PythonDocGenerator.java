package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonUtility;
import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.actions.PythonDocGenerator"
)
@ActionRegistration(
        displayName = "#CTL_PythonDocGenerator"
)
@Messages({
    "CTL_PythonDocGenerator=Pdoc Generator",
    "CTL_PythonDocMessage=Generating Documentation"
})
public final class PythonDocGenerator implements ActionListener {

    public static final Logger LOG = Logger.getLogger(PythonDocGenerator.class.getName());
    private final Project context;

    public PythonDocGenerator(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(context.getProjectDirectory()));
        PythonUtility.manageRunEnvs(pb);

        try {
            String[] params = {};
            List<String> argList = new ArrayList<>();
            Properties prop = PythonUtility.getProperties(context, false);
            params = prop.getProperty("nbproject.pdoc.params", "-o docs")
                    .split(" ");
            List<String> asList1 = null;

            asList1 = Arrays.asList(/*osShell[1],*/PythonUtility.getLspPythonExe(),
                    "-m", "pdoc", Paths.get(context.getProjectDirectory().getPath()).toString());
            argList.addAll(asList1);
            argList.addAll(Arrays.asList(params));
            pb.command(argList);

            LOG.info(() -> Arrays.toString(argList.toArray()));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        ExecutionService service = ExecutionService.newService(() -> pb.start(),
                PythonUtility.getExecutorDescriptor(
                        new PythonOutputLine(), () -> {
                            StatusDisplayer.getDefault().setStatusText(Bundle.CTL_PythonDocMessage());
                            LifecycleManager.getDefault().saveAll();
                        }, () -> {
                        }, true, true), String.format("%s%s%s", "Pdoc (", context.getProjectDirectory().getName(), ")"));

        service.run();
    }

}
