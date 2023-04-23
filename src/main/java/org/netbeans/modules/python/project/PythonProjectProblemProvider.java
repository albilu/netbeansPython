package org.netbeans.modules.python.project;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.projectproperties.PythonCustomizerProvider;
import org.netbeans.spi.project.ui.ProjectProblemResolver;
import org.netbeans.spi.project.ui.ProjectProblemsProvider;
import org.netbeans.spi.project.ui.support.ProjectProblemsProviderSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
public class PythonProjectProblemProvider implements ProjectProblemsProvider {

    private final Project project;
    public static ProjectProblemsProviderSupport problemsProviderSupport;

    public PythonProjectProblemProvider(Project project) {
        assert project != null;
        this.project = project;
        this.problemsProviderSupport = new ProjectProblemsProviderSupport(project);

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        problemsProviderSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        problemsProviderSupport.removePropertyChangeListener(listener);
    }

    @Override
    public Collection<? extends ProjectProblem> getProblems() {
        return problemsProviderSupport.getProblems(new ProjectProblemsProviderSupport.ProblemsCollector() {
            Collection<ProjectProblemsProvider.ProjectProblem> currentProblems = new ArrayList<>(3);

            @Override
            public Collection<? extends ProjectProblem> collectProblems() {
                isLspServerInstalled(currentProblems);
                selectVenv(currentProblems);
                isVenvDependenciesInstalled(currentProblems);
                return currentProblems;

            }

        });

    }

    @NbBundle.Messages({
        "CTL_ServerNotInstalled=LSP Server not installed",
        "CTL_Server=LSP Server"
    })
    private void isLspServerInstalled(Collection<ProjectProblem> currentProblems) {
        try {
            String serverVersion = PythonUtility.getServerVersion();
            if (!serverVersion.startsWith("__main__.py")) {
                currentProblems.add(ProjectProblem.createError(Bundle.CTL_Server(),
                        Bundle.CTL_ServerNotInstalled(),
                        new LSPServerProblemResolverImpl(project)));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @NbBundle.Messages({
        "CTL_VenvSelection=Recommended to select the Virtual Environment for this Project",
        "CTL_Venv=Virtual Environment"
    })
    private void selectVenv(Collection<ProjectProblem> currentProblems) {
        try {
            String projectPythonExe = PythonUtility.getProjectPythonExe(project.getProjectDirectory());
            if ((project.getProjectDirectory().getFileObject(".venv") != null
                    || project.getProjectDirectory().getFileObject("venv") != null)
                    && !projectPythonExe.contains(".venv")) {
                currentProblems.add(ProjectProblem.createWarning(Bundle.CTL_Venv(), Bundle.CTL_VenvSelection(),
                        new VENVSelectionProblemResolverImpl(project)));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @NbBundle.Messages("CTL_VenvMissingDeps=Missing Dependencies")
    private void isVenvDependenciesInstalled(Collection<ProjectProblem> currentProblems) {
        try {
            String projectPythonExe = PythonUtility.getProjectPythonExe(project.getProjectDirectory());
            if (!projectPythonExe.contains(".venv")) {
                return;
            }

            if (!StringUtils.containsAny(PythonUtility.getPipList(projectPythonExe), PythonUtility.VENV_DEPS)) {
                currentProblems.add(ProjectProblem.createError(Bundle.CTL_Venv(), Bundle.CTL_VenvMissingDeps(),
                        new VENVProblemResolverImpl(project)));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static class LSPServerProblemResolverImpl implements ProjectProblemResolver {

        public LSPServerProblemResolverImpl(Project p) {
        }

        @Override
        public Future<Result> resolve() {
            FutureTask<Result> toRet = new FutureTask<>(() -> {
                new RequestProcessor().post(() -> {
                    PythonUtility.installLsp(this.getClass().getClassLoader());
                    problemsProviderSupport.fireProblemsChange();
                });
                return Result.create(Status.RESOLVED);
            });
            toRet.run();
            return toRet;
        }

    }

    private static class VENVProblemResolverImpl implements ProjectProblemResolver {

        Project p;

        public VENVProblemResolverImpl(Project p) {
            this.p = p;
        }

        @Override
        public Future<Result> resolve() {
            FutureTask<Result> toRet = new FutureTask<>(() -> {
                new RequestProcessor().post(() -> {
                    PythonUtility.venvPackageInstaller(p.getProjectDirectory());
                    problemsProviderSupport.fireProblemsChange();
                });
                return Result.create(Status.RESOLVED);
            });
            toRet.run();
            return toRet;
        }

    }

    private static class VENVSelectionProblemResolverImpl implements ProjectProblemResolver {

        Project p;

        public VENVSelectionProblemResolverImpl(Project p) {
            this.p = p;
        }

        @Override
        public Future<Result> resolve() {
            FutureTask<Result> toRet = new FutureTask<>(() -> {
                try {
                    p.getLookup().lookup(PythonCustomizerProvider.class).showCustomizer();
                } catch (Exception e) {
                    return Result.create(Status.UNRESOLVED);
                }
                return Result.create(Status.RESOLVED);
            });
            toRet.run();
            return toRet;
        }

    }

}
