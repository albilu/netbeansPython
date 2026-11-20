package org.netbeans.modules.python.project;

import java.io.File;
import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.ProjectProblems;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.python.PythonLspConfiguration;
import org.netbeans.modules.python.PythonUtility;
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

//    private final ClassPath source;
    public PythonProjectOpenedHook(Project project) {
        this.project = project;
        this.projectDir = project.getProjectDirectory();
//        this.source = ClassPathSupport.createClassPath(project.getProjectDirectory());
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

        // A new server receives its settings and the project's interpreter once
        // started; one that kept running gets them again here (#144).
        LSPBindings.ensureServerRunning(project, PythonUtility.PYTHON_MIME_TYPE);
        PythonLspConfiguration.configureProject(project);
//        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[]{source});
        if (ProjectProblems.isBroken(project)) {
            ProjectProblems.showAlert(project);
        }

    }

    @Override
    protected void projectClosed() {
//        GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[]{source});
    }
}
