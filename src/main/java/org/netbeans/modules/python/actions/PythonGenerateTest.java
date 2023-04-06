package org.netbeans.modules.python.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonUtility;
import org.openide.LifecycleManager;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonGenerateTest {

    public static final Logger LOG = Logger.getLogger(PythonGenerateTest.class.getName());

    public static void runAction(DataObject context) {
        FileObject primaryFile = context.getPrimaryFile();
        FileObject primaryFileParent = primaryFile.getParent();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(primaryFileParent));

        PythonUtility.manageRunEnvs(pb);

        String[] params = {};
        List<String> argList = new ArrayList<>();
        try {
            Project owner = FileOwnerQuery.getOwner(primaryFile);
            if (owner != null) {
                Properties prop = PythonUtility.getProperties(owner);
                if (!prop.getProperty("nbproject.test.generator.params", "")
                        .isEmpty()) {
                    params = prop.getProperty("nbproject.test.generator.params", "")
                            .split(" ");
                }

                SourceGroup[] testSourceGroups = ProjectUtils.getSources(owner).getSourceGroups("testsources");
                String testClassName = FilenameUtils
                        .removeExtension(FileUtil
                                .getRelativePath(owner.getProjectDirectory(), primaryFile)) + "Test.py";
                Path generatedTestFile = Paths.get(testSourceGroups[0].getRootFolder().getPath())
                        .resolve(testClassName);
                String outputPath = prop.getProperty("nbproject.test.generator.dir", testSourceGroups.length > 0
                        ? generatedTestFile.getParent().toFile().toPath().toString()
                        : Paths.get(primaryFileParent.getPath()).toString());

                argList.addAll(Arrays.asList(PythonUtility
                        .getProjectPythonExe(context
                                .getPrimaryFile()),
                        "-m",
                        "pynguin",
                        "--project-path",
                        Paths.get(primaryFileParent.getPath()).toString(),
                        "--module-name",
                        primaryFile.getName(),
                        "--output-path",
                        outputPath,
                        "-v"
                ));
                argList.addAll(Arrays.asList(params));
                pb.command(argList);

                LOG.info(() -> Arrays.toString(argList.toArray()));
                ExecutionService service = ExecutionService.newService(() -> pb.start(),
                        PythonUtility.getExecutorDescriptor(
                                new PythonOutputLine(), () -> {
                                    StatusDisplayer.getDefault()
                                            .setStatusText("Pynguin Generation");
                                    LifecycleManager.getDefault().saveAll();
                                }, () -> {
                                    File generated = generatedTestFile.getParent().resolve("test_" + primaryFile.getNameExt()).toFile();
                                    if (generated != null) {
                                        generated.renameTo(generatedTestFile.toFile());
                                        try {
                                            DataObject.find(FileUtil.toFileObject(generatedTestFile.toFile())).getLookup().lookup(EditorCookie.class)
                                                    .open();
                                        } catch (DataObjectNotFoundException ex) {
                                            Exceptions.printStackTrace(ex);
                                        }

                                    }
                                },
                                true, true
                        ),
                        String.format("%s%s%s", "Generating (", generatedTestFile.toFile().getName(), ")"));

                service.run();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
