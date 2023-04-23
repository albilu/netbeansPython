package org.netbeans.modules.python.testrunner;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider;
import org.netbeans.modules.python.project.PythonProject;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@TestCreatorProvider.Registration(identifier = "PythonPyTest", displayName = "PyTest")
public class PythonPyTestCreator extends TestCreatorProvider {

    RequestProcessor RP = new RequestProcessor(this.getClass()
            .getName(), 2);

    @Override
    public boolean enable(FileObject[] activatedFOs) {
        if (activatedFOs == null || activatedFOs.length == 0) {
            return false;
        }
        return FileOwnerQuery.getOwner(activatedFOs[0]) instanceof PythonProject;
    }

    @Override
    public void createTests(Context context) {
        RP.post(() -> {
            PythonTestCreatorUtility.createTests(context, "Templates/Python/PythonPytest.py");
        });
    }

}
