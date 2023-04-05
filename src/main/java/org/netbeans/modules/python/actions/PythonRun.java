package org.netbeans.modules.python.actions;

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
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.LifecycleManager;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
public class PythonRun {

    public static final Logger LOG = Logger.getLogger(PythonRun.class.getName());

    @NbBundle.Messages("CTL_Run=Running Python")
    static public void runAction(Project owner, DataObject context) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(context.getPrimaryFile().getParent()));
        PythonUtility.manageRunEnvs(pb);

        String[] params = {};
        List<String> argList = new ArrayList<>();
        try {
            //Project owner = FileOwnerQuery.getOwner(context.getPrimaryFile());
            if (owner != null) {
                Properties conf = PythonUtility.getConf(owner);
                if (!conf.getProperty("nbproject.run.params", "").isEmpty()) {
                    params = conf.getProperty("nbproject.run.params", "")
                            .split(" ");
                }
            }
            if (owner != null && PythonUtility.isPoetry((PythonProject) owner)) {
                argList.addAll(Arrays.asList(PythonUtility
                        .getProjectPythonExe(context
                                .getPrimaryFile()), "-m", "poetry", "run", "python",
                        Paths.get(context.getPrimaryFile().getPath()).toString()));
            } else {
                argList.addAll(Arrays.asList(PythonUtility
                        .getProjectPythonExe(context
                                .getPrimaryFile()), Paths.get(context.getPrimaryFile()
                                .getPath()).toString()));
            }

            argList.addAll(Arrays.asList(params));
            pb.command(argList);

            LOG.info(() -> Arrays.toString(argList.toArray()));

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        ExecutionService service = ExecutionService.newService(() -> pb.start(),
                PythonUtility.getExecutorDescriptor(
                        new PythonOutputLine(), () -> {
                            StatusDisplayer.getDefault().setStatusText(Bundle.CTL_Run());
                            LifecycleManager.getDefault().saveAll();
                        }, () -> {
                        }, true), String.format("%s%s%s", "Run (", context.getPrimaryFile().getNameExt(), ")"));

        service.run();
    }
}
