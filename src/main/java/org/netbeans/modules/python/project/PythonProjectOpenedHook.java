package org.netbeans.modules.python.project;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.ProjectProblems;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonProjectOpenedHook extends ProjectOpenedHook {

    private final Project project;
    private final FileObject projectDir;

    private final ClassPath source;

    public PythonProjectOpenedHook(Project project) {
        this.project = project;
        this.projectDir = project.getProjectDirectory();
        this.source = ClassPathSupport.createClassPath(project.getProjectDirectory());
    }

    @Override
    protected void projectOpened() {
        PythonUtility.createProperties(project);
        IndexingManager.getDefault().refreshIndex(projectDir.toURL(), null, false, true);
        try {
            String pythonStdLibPath = PythonUtility.getPythonStdLibPath(PythonUtility
                    .getProjectPythonExe(project.getProjectDirectory()));
            File file = new File(pythonStdLibPath);
            if (file.exists()) {
                IndexingManager.getDefault().refreshIndex(Utilities.toURI(file).toURL(), null, false, false);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        try {
            LSPBindings.ensureServerRunning(project, PythonUtility.PYTHON_MIME_TYPE);
            Gson create = new GsonBuilder()
                    .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
            Map<String, Object> settings = create
                    .fromJson(Files.readString(
                            PythonUtility.SETTINGS.toPath()),
                            new TypeToken<HashMap<String, Object>>() {
                            }.getType());
            if (settings != null) {
                Map<String, Object> paramsObject = new HashMap<>();
                settings.remove("auto_pop_completion");
                paramsObject.put("pylsp", settings);
                DidChangeConfigurationParams params = new DidChangeConfigurationParams(paramsObject);
                LSPBindings.getBindingsImpl(project, projectDir,
                        PythonUtility.PYTHON_MIME_TYPE).getWorkspaceService()
                        .didChangeConfiguration(params);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[]{source});
        if (ProjectProblems.isBroken(project)) {
            ProjectProblems.showAlert(project);
        }

    }

    @Override
    protected void projectClosed() {
        GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[]{source});
    }
}
