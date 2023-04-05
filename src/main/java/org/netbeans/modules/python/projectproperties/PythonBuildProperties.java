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
public class PythonBuildProperties
        implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String GENERAL = "Build";

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-python", position = 25)
    public static PythonBuildProperties createBuild() {
        return new PythonBuildProperties();
    }

    @NbBundle.Messages("LBL_Config_Build=Build")
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(
                GENERAL,
                Bundle.LBL_Config_Build(),
                null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category,
            Lookup lkp) {
        return new PythonBuildPanel(lkp.lookup(PythonProject.class));
    }

}
