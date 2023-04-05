/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.netbeans.modules.python.testrunner;

import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController;
import org.openide.text.Annotation;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
//Copy from org/netbeans/modules/gsf/testrunner/ui/annotation/TestMethodAnnotation.java
public class PythonTestMethodAnnotation extends Annotation {

    public static final Object DOCUMENT_METHODS_KEY = new Object() {
    };
    public static final Object DOCUMENT_ANNOTATIONS_KEY = new Object() {
    };
    public static final Object DOCUMENT_ANNOTATION_LINES_KEY = new Object() {
    };

    private final TestMethodController.TestMethod testMethod;

    public PythonTestMethodAnnotation(TestMethodController.TestMethod testMethod) {
        this.testMethod = testMethod;
    }

    @Override
    public String getAnnotationType() {
        return "org-netbeans-modules-python-testrunner-runnable-test-annotation";
    }

    @Override
    @NbBundle.Messages({
        "# {0} - the name of the method",
        "SD_TestMethod=Test Method: {0}"
    })
    public String getShortDescription() {
        return Bundle.SD_TestMethod(testMethod.method().getMethodName());
    }

}
