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
public class PythonPdocProperties
        implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String GENERAL = "Pdoc";

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-python", position = 35)
    public static PythonPdocProperties createPdoc() {
        return new PythonPdocProperties();
    }

    @NbBundle.Messages("LBL_Config_Pdoc=Pdoc")
    @Override
    public ProjectCustomizer.Category createCategory(Lookup lkp) {
        return ProjectCustomizer.Category.create(
                GENERAL,
                Bundle.LBL_Config_Pdoc(),
                null);
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category,
            Lookup lkp) {
        return new PythonPdocPanel(lkp.lookup(PythonProject.class));
    }

}
