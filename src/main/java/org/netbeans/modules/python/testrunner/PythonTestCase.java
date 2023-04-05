package org.netbeans.modules.python.testrunner;

import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

/**
 *
 * @author albilu
 */
public class PythonTestCase extends Testcase {

    public PythonTestCase(String name, String type, TestSession session) {
        super(name, type, session);
    }

    public PythonTestCase(String name, String display, String type, TestSession session) {
        super(name, display, type, session);
    }

}
