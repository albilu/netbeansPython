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
public class PythonTestProperties
        implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String GENERAL = "Test";

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-python", position = 30)
    public static PythonTestProperties createTest() {
        return new PythonTestProperties();
    }

    @NbBundle.Messages("LBL_Config_Test=Test")
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(
                GENERAL,
                Bundle.LBL_Config_Test(),
                null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category,
            Lookup lkp) {
        return new PythonTestPanel(lkp.lookup(PythonProject.class));
    }

}
