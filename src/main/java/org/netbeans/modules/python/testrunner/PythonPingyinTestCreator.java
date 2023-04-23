package org.netbeans.modules.python.testrunner;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.actions.PythonGenerateTest;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@TestCreatorProvider.Registration(identifier = "PythonPynguin", displayName = "Pynguin")
public class PythonPingyinTestCreator extends TestCreatorProvider {

    RequestProcessor RP = new RequestProcessor(this.getClass()
            .getName(), 2);

    @Override
    public boolean enable(FileObject[] activatedFOs) {
        if (activatedFOs == null || activatedFOs.length == 0) {
            return false;
        }
        return activatedFOs.length == 1 && !activatedFOs[0].isFolder()
                && FileOwnerQuery.getOwner(activatedFOs[0]) instanceof PythonProject;
    }

    @Override
    public void createTests(Context context) {
        RP.post(() -> {
            try {
                PythonGenerateTest.runAction(DataObject.find(context.getActivatedFOs()[0]));
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }

}
