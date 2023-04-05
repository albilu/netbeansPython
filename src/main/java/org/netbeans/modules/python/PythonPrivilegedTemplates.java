package org.netbeans.modules.python;

import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;

/**
 *
 * @author albilu
 */
public class PythonPrivilegedTemplates implements RecommendedTemplates, PrivilegedTemplates {

    private static final String[] RECOMMENDED_TYPES = new String[]{
        "Python",
        "simple-files"
    };

    private static final String[] PRIVILEGED_NAMES = {
        "Templates/Python/Python.py",
        "Templates/Python/EmptyPython.py",
        "Templates/Python/PythonUnittest.py",
        "Templates/Python/PythonPytest.py",
        "simple-files"
    };

    @Override
    public String[] getPrivilegedTemplates() {
        return PRIVILEGED_NAMES;
    }

    @Override
    public String[] getRecommendedTypes() {
        return RECOMMENDED_TYPES;
    }

}
