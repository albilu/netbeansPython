package org.netbeans.modules.python.testrunner;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.editor.AnnotationDesc;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.ImplementationProvider;
import org.netbeans.editor.JumpList;
import org.netbeans.modules.gsf.testrunner.api.CommonUtils;
import org.netbeans.modules.gsf.testrunner.ui.annotation.PopupUtil;
import org.netbeans.modules.gsf.testrunner.ui.annotation.SelectActionPopup;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.NbDocument;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;

/**
 *
 * @author albilu
 */
//Copy from org/netbeans/modules/gsf/testrunner/ui/annotation/RunDebugTestGutterAction.java
@ActionID(id = "org.netbeans.modules.python.testrunner.PythonTestGlyphAction", category = "CommonTestRunner")
@ActionRegistration(displayName = "#NM_RunGutterAction", lazy = false)
@ActionReference(path = "Editors/GlyphGutterActions", position = 190)
@NbBundle.Messages("NM_RunGutterAction=Run")
public class PythonTestGlyphAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(PythonTestGlyphAction.class.getName());
    private static final Set<String> fixableAnnotations = new HashSet<>();

    static {
        fixableAnnotations.add("org-netbeans-modules-python-testrunner-runnable-test-annotation"); // NOI18N
    }

    public PythonTestGlyphAction() {
        putValue(NAME, Bundle.NM_RunGutterAction());
    }

    @Override
    public Object getValue(String key) {
        if ("supported-annotation-types".equals(key)) {//NOI18N
            return fixableAnnotations.toArray(new String[0]);
        }
        return super.getValue(key);
    }

    @Override
    @NbBundle.Messages({
        "ERR_NoTestMethod=No Test Method",
        "# {0} - method name",
        "DN_run.single.method=Run {0} method",
        "# {0} - method name",
        "DN_debug.single.method=Debug {0} method",
        "CAP_SelectAction=Select Action",})
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (!(source instanceof JTextComponent)) {
            StatusDisplayer.getDefault().setStatusText(Bundle.ERR_NoTestMethod());
            return; //probably right click menu
        }

        JTextComponent comp = (JTextComponent) source;
        Document doc = comp.getDocument();
        int caretPos = comp.getCaretPosition();
        int line = NbDocument.findLineNumber((StyledDocument) doc, caretPos);
        AnnotationDesc activeAnnotation = ((BaseDocument) doc).getAnnotations().getActiveAnnotation(line);

        if (activeAnnotation != null && fixableAnnotations.contains(activeAnnotation.getAnnotationType())) {
            Map<Integer, TestMethodController.TestMethod> annotationLines = (Map<Integer, TestMethodController.TestMethod>) doc.getProperty(PythonTestMethodAnnotation.DOCUMENT_ANNOTATION_LINES_KEY);
            TestMethodController.TestMethod testMethod = annotationLines.get(line);
            if (testMethod != null) {
                SingleMethod singleMethod = testMethod.method();

                List<SelectActionPopup.ActionDescription> actions = new ArrayList<>();
                ActionProvider ap = CommonUtils.getInstance().getActionProvider(singleMethod.getFile());
                if (ap != null) {
                    for (String command : new String[]{SingleMethod.COMMAND_RUN_SINGLE_METHOD,
                        SingleMethod.COMMAND_DEBUG_SINGLE_METHOD}) {
                        String displayName = NbBundle.getMessage(PythonTestGlyphAction.class, "DN_" + command, singleMethod.getMethodName());
                        if (Arrays.asList(ap.getSupportedActions()).contains(command) && ap.isActionEnabled(command, Lookups.singleton(testMethod))) {
                            actions.add(new SelectActionPopup.ActionDescription(displayName, () -> ap.invokeAction(command, Lookups.singleton(testMethod))));
                        }
                    }
                }

                if (actions.size() > 1) {
                    final Point[] p = new Point[1];

                    doc.render(() -> {
                        try {
                            int startOffset = NbDocument.findLineOffset((StyledDocument) doc, line);
                            p[0] = comp.modelToView(startOffset).getLocation();
                        } catch (BadLocationException ex) {
                            LOG.log(Level.WARNING, null, ex);
                        }
                    });

                    JumpList.checkAddEntry(comp/*, caretPos*/);

                    SwingUtilities.convertPointToScreen(p[0], comp);

                    PopupUtil.showPopup(new SelectActionPopup(Bundle.CAP_SelectAction(), actions), Bundle.CAP_SelectAction(), p[0].x, p[0].y, true, 0);
                } else if (actions.size() == 1) {
                    actions.get(0).action.run();
                }

                return;
            }
        }

        Action actions[] = ImplementationProvider.getDefault().getGlyphGutterActions((JTextComponent) source);

        if (actions == null) {
            return;
        }

        int nextAction = 0;

        while (nextAction < actions.length && actions[nextAction] != this) {
            nextAction++;
        }

        nextAction++;

        if (actions.length > nextAction) {
            Action a = actions[nextAction];
            if (a != null && a.isEnabled()) {
                a.actionPerformed(e);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        TopComponent activetc = TopComponent.getRegistry().getActivated();
        if (activetc instanceof CloneableEditorSupport.Pane) {
            return true;
        }
        return false;
    }

}
