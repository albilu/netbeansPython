package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.PythonProject;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(
        category = "Project",
        id = "org.netbeans.modules.python.RunAction"
)
@ActionRegistration(
        displayName = "#CTL_RunAction", lazy = false
)
@ActionReferences({
    @ActionReference(path = "Editors/text/x-python/Popup", position = 0),
    @ActionReference(path = "Loaders/text/x-python/Actions", position = 250)
})
@Messages("CTL_RunAction=Run Single Python File")
public final class PythonRunAction extends AbstractAction implements LookupListener, ContextAwareAction {

    private static final long serialVersionUID = 1L;

    private Lookup context;
    Lookup.Result<DataObject> lkpInfo;

    public PythonRunAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PythonRunAction(Lookup context) {
        putValue(Action.NAME, Bundle.CTL_RunAction());
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        this.context = context;
    }

    void init() {
        assert SwingUtilities.isEventDispatchThread() : "this shall be called just from AWT thread";
        if (lkpInfo != null) {
            return;
        }
        //The thing we want to listen for the presence or absence of
        //on the global selection
        lkpInfo = context.lookupResult(DataObject.class);
        lkpInfo.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Collection<? extends DataObject> allInstances = lkpInfo.allInstances();
        if (allInstances.isEmpty()) {
            setEnabled(false);
            return;
        }
        FileObject primaryFile = lkpInfo.allInstances().stream()
                .findFirst().get().getPrimaryFile();
        Project owner = FileOwnerQuery.getOwner(primaryFile);
        if (owner != null && (owner instanceof PythonProject)) {
            setEnabled(false);
            return;
        }
        setEnabled(true);
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PythonRunAction(context);
    }

    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        PythonRun.runAction(null, lkpInfo.allInstances().stream().findFirst().get());
    }
}
