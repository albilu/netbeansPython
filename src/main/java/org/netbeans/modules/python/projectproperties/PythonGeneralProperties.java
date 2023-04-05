package org.netbeans.modules.python.projectproperties;

/**
 *
 * @author albilu
 */
import javax.swing.JComponent;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class PythonGeneralProperties
        implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String GENERAL = "General";

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-python", position = 10)
    public static PythonGeneralProperties createGeneral() {
        return new PythonGeneralProperties();
    }

    @NbBundle.Messages("LBL_Config_General=General")
    @Override
    public Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(
                GENERAL,
                Bundle.LBL_Config_General(),
                null);
    }

    @Override
    public JComponent createComponent(Category category, Lookup lkp) {
        return new PythonGeneralPanel(lkp.lookup(PythonProject.class));
    }

}
