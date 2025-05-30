package org.netbeans.modules.python.testrunner;

import java.io.IOException;
import java.util.Properties;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.ui.api.Manager;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.project.PythonProject;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonTestManager {

    public static TestSession getSession(Project owner, String runner) {
        return new TestSession(ProjectUtils.getInformation(owner).getName() + " (" + runner + ")",
                owner, TestSession.SessionType.TEST);
    }

    public static Manager getTestManager() {
        Manager manager = Manager.getInstance();
        manager.setNodeFactory(new PythonTestNodeFactory());
        return manager;
    }

    public static Object[] getTestRunParams(PythonProject project) {
        try {
            Properties prop = PythonUtility.getProperties(project, false);
            return new Object[]{prop.getProperty("nbproject.test.runner", "unittest"),
                PythonUtility.getParamsTokenizer().reset(
                prop.getProperty("nbproject.test.params", "*Test.py")).getTokenArray()};
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

}
