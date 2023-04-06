package org.netbeans.modules.python.actions;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.LifecycleManager;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
public class PythonBuild {

    public static final Logger LOG = Logger.getLogger(PythonBuild.class.getName());

    @NbBundle.Messages({
        "CTL_BuildStatusMessage=Building Python Project",
        "CTL_BuildMessage=Build ("
    })
    public static void runAction(Project owner, FileObject context) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(context));
        PythonUtility.manageRunEnvs(pb);

        //Project owner = FileOwnerQuery.getOwner(context);
        try {
            String[] params = {};
            List<String> argList = new ArrayList<>();
            if (owner != null) {
                Properties prop = PythonUtility.getProperties(owner);
                if (!prop.getProperty("nbproject.build.params", "").isEmpty()) {
                    params = prop.getProperty("nbproject.build.params", "")
                            .split(" ");
                }
            }
            List<String> asList1 = null;
            boolean isPoetry = PythonUtility.isPoetry((PythonProject) owner);
            if (owner != null && owner.getProjectDirectory()
                    .getFileObject("pyproject.toml") != null && !isPoetry) {
                asList1 = Arrays.asList(/*osShell[1],*/PythonUtility.getProjectPythonExe(context),
                        "-m", "build");
            } else if (owner != null && owner.getProjectDirectory()
                    .getFileObject("setup.py") != null) {
                asList1 = Arrays.asList(/*osShell[1],*/PythonUtility.getProjectPythonExe(context),
                        Paths.get(owner.getProjectDirectory().getFileObject("setup.py").getPath())
                                .toString(), "build");
            } else if (isPoetry) {
                asList1 = Arrays.asList(/*osShell[1],*/PythonUtility.getProjectPythonExe(context),
                        "-m", "poetry", "build");
            }
            argList.addAll(asList1);
            argList.addAll(Arrays.asList(params));
            pb.command(argList);

            LOG.info(() -> Arrays.toString(argList.toArray()));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        ExecutionService service = ExecutionService
                .newService(() -> pb.start(), PythonUtility.getExecutorDescriptor(new PythonOutputLine(),
                        () -> {
                            if (owner != null) {
                                FileUtils.deleteQuietly(Paths.get(owner.getProjectDirectory().getPath()).resolve("dist").toFile());
                                FileUtils.deleteQuietly(Paths.get(owner.getProjectDirectory().getPath()).resolve("build").toFile());
                            }
                            StatusDisplayer.getDefault().setStatusText(Bundle.CTL_BuildStatusMessage());
                            LifecycleManager.getDefault().saveAll();
                        }, () -> {
                        }, true, true),
                        String.format("%s%s%s", Bundle.CTL_BuildMessage(), context.getName(), ")"));

        service.run();
    }
}
