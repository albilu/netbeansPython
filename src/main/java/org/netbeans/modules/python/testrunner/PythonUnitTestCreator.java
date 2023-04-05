package org.netbeans.modules.python.testrunner;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider;
import org.netbeans.modules.python.PythonProject;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@TestCreatorProvider.Registration(identifier = "PythonUnitTest", displayName = "UnitTest")
public class PythonUnitTestCreator extends TestCreatorProvider {

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
            PythonTestCreatorUtility.createTests(context, "Templates/Python/PythonUnittest.py");
        });
    }

}
