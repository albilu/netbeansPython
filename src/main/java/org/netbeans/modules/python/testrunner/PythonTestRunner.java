package org.netbeans.modules.python.testrunner;

import java.io.File;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.ui.api.Manager;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController;
import org.netbeans.modules.python.project.PythonProject;
import org.openide.loaders.DataObject;

/**
 *
 * @author albilu
 */
public interface PythonTestRunner {

    void runSingleTest(PythonProject project, DataObject dob);

    void runAllTests(PythonProject project, DataObject dob);

    void runTestMethod(PythonProject project, TestMethodController.TestMethod lookup);

    void runner(PythonProject project, Manager manager, TestSession testSession, ProcessBuilder pb, String projectPythonExe, File testRunner);

}
