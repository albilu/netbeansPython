package org.netbeans.modules.python.projectproperties;

/**
 *
 * @author albilu
 */
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.ProjectProblems;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonProjectProblemProvider;
import org.netbeans.modules.python.PythonPropertiesNotifier;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;

public class PythonCustomizerProvider implements CustomizerProvider {

    public final PythonProject project;

    public static final String CUSTOMIZER_FOLDER_PATH
            = "Projects/org-netbeans-modules-python/Customizer";

    public PythonCustomizerProvider(PythonProject project) {
        this.project = project;
    }

    @Override
    public void showCustomizer() {
        Dialog dialog = ProjectCustomizer.createCustomizerDialog(
                //Path to layer folder:
                CUSTOMIZER_FOLDER_PATH,
                //Lookup, which must contain, at least, the Project:
                Lookups.fixed(project),
                //Preselected category:
                "General",
                //OK button listener:
                new OKOptionListener(),
                //HelpCtx for Help button of dialog:
                null);
        dialog.setTitle("Project Properties - " + ProjectUtils
                .getInformation(project).getDisplayName());
        dialog.setVisible(true);
    }

    private class OKOptionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (ProjectManager.getDefault().isModified(project)) {

                PythonGeneralPanel.setProperties(project);

                if (PythonRunPanel.scriptTextField != null) {
                    PythonRunPanel.setProperties(project);
                }
                if (PythonBuildPanel.buildParamsTextField != null) {
                    PythonBuildPanel.setProperties(project);
                }

                if (PythonTestPanel.testFrameworkComboBox != null) {
                    PythonTestPanel.setProperties(project);
                }
                if (PythonPdocPanel.pdocParamsTextField != null) {
                    PythonPdocPanel.setProperties(project);
                }

                if (ProjectProblems.isBroken(project)) {
                    ProjectProblems.showAlert(project);
                }
            }

            PythonPropertiesNotifier.firePropertiesChange();
            PythonProjectProblemProvider.problemsProviderSupport.fireProblemsChange();
            try {
                ProjectManager.getDefault().saveProject(project);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

    }
}
