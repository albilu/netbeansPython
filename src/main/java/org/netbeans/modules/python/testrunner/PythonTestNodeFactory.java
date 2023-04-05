/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.netbeans.modules.python.testrunner;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.math.NumberUtils;
import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.ui.api.CallstackFrameNode;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodNode;
import org.netbeans.modules.gsf.testrunner.ui.api.TestsuiteNode;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
public class PythonTestNodeFactory extends org.netbeans.modules.gsf.testrunner.ui.api.TestRunnerNodeFactory {

    @Override
    public Node createTestMethodNode(Testcase tstcs, Project prjct) {
        return new PythonTestMethodNode(tstcs, prjct);
    }

    @Override
    public Node createCallstackFrameNode(String string, String string1) {
        return new PythonCallstackFrameNode(string, string1);
    }

    @Override
    public TestsuiteNode createTestSuiteNode(String string, boolean bln) {
        return new PythonTestSuiteNode(string, bln);
    }

    private static class PythonTestMethodNode extends TestMethodNode {

        Testcase testcase;
        Project project;

        public PythonTestMethodNode(Testcase testcase, Project project) {
            super(testcase, project);
            this.testcase = testcase;
            this.project = project;
        }

        @Override
        public Action getPreferredAction() {
            return new PythonJumpTestAction(testcase, project);
        }

    }

    private static class PythonCallstackFrameNode extends CallstackFrameNode {

        public PythonCallstackFrameNode(String frameInfo, String displayName) {
            super(frameInfo, displayName);
        }

    }

    private static class PythonTestSuiteNode extends TestsuiteNode {

        public PythonTestSuiteNode(String suiteName, boolean filtered) {
            super(suiteName, filtered);
        }

    }

    private static class PythonJumpTestAction extends AbstractAction {

        private static final RequestProcessor RP = new RequestProcessor(PythonJumpTestAction.class);
        Testcase testcase;
        Project project;

        public PythonJumpTestAction(Testcase testcase, Project project) {
            this.testcase = testcase;
            this.project = project;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String loc = testcase.getLocation();
            if (loc != null) {

                String[] location = loc.split("##");

                SwingUtilities.invokeLater(() -> {
                    try {
                        FileObject toFileObject = FileUtil.toFileObject(new File(location[0]));
                        if (toFileObject != null) {
                            DataObject dobj = DataObject.find(toFileObject);
                            if (dobj != null) {
                                LineCookie lc = (LineCookie) dobj.getLookup().lookup(LineCookie.class);
                                if (lc == null) {/* cannot do it */ return;
                                }
                                Line l = lc.getLineSet().getOriginal(NumberUtils.toInt(location[1], 0));
                                l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                            }
                        }
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
            }
        }

    }
}
