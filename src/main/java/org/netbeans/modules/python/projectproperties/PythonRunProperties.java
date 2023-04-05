package org.netbeans.modules.python.projectproperties;

import javax.swing.JComponent;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
public class PythonRunProperties
        implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String GENERAL = "Run";

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-python", position = 20)
    public static PythonRunProperties createRun() {
        return new PythonRunProperties();
    }

    @NbBundle.Messages("LBL_Config_Run=Run")
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(
                GENERAL,
                Bundle.LBL_Config_Run(),
                null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category,
            Lookup lkp) {
        return new PythonRunPanel(lkp.lookup(PythonProject.class));
    }

}
