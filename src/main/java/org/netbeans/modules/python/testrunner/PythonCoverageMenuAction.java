package org.netbeans.modules.python.testrunner;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.codecoverage.api.CoverageActionFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

//Copy from org/netbeans/modules/gradle/java/coverage/CoveragePopup.java
@ActionID(category = "Project", id = "org.netbeans.modules.python.testrunner.PythonCoverageMenuAction")
@ActionRegistration(displayName = "Test Coverage", lazy = false) // NOI18N
@ActionReference(path = "Projects/org-netbeans-modules-python/Actions", position = 1205)
public class PythonCoverageMenuAction extends AbstractAction implements ContextAwareAction {

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public PythonCoverageMenuAction() {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup ctx) {
        Project p = ctx.lookup(Project.class);
        if (p == null) {
            return this;
        }
        if (p.getProjectDirectory().getFileObject(".coverage") == null) {
            return this;
        }
        return ((ContextAwareAction) CoverageActionFactory.createCollectorAction(null, null))
                .createContextAwareInstance(ctx);
    }

}
