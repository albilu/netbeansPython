package org.netbeans.modules.python.debugger.breakpoints;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.JComponent;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Controller;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.Models;
import org.netbeans.spi.viewmodel.NodeActionsProvider;
import org.netbeans.spi.viewmodel.NodeActionsProviderFilter;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.text.Line;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 * @author albilu
 */
@DebuggerServiceRegistration(path = "BreakpointsView", types = NodeActionsProviderFilter.class)
public class BreakpointNodeActionsProvider implements NodeActionsProviderFilter {

    @Messages("LBL_Action_Go_To_Source=Go to Source")
    private static final Action GO_TO_SOURCE_ACTION = Models.createAction(
            Bundle.LBL_Action_Go_To_Source(),
            new Models.ActionPerformer() {
        @Override
        public boolean isEnabled(Object node) {
            return true;
        }

        @Override
        public void perform(Object[] nodes) {
            goToSource((PythonBreakpoint) nodes[0]);
        }
    },
            Models.MULTISELECTION_TYPE_EXACTLY_ONE);

    @Messages("LBL_Action_Customize=Properties")
    private static final Action CUSTOMIZE_ACTION = Models.createAction(
            Bundle.LBL_Action_Customize(),
            new Models.ActionPerformer() {
        @Override
        public boolean isEnabled(Object node) {
            return true;
        }

        @Override
        public void perform(Object[] nodes) {
            customize((PythonBreakpoint) nodes[0]);
        }
    },
            Models.MULTISELECTION_TYPE_EXACTLY_ONE);

    @Override
    public Action[] getActions(NodeActionsProvider original, Object node) throws UnknownTypeException {
        if (!(node instanceof PythonBreakpoint)) {
            return original.getActions(node);
        }

        Action[] oas = original.getActions(node);
        Action[] as = new Action[oas.length + 3];
        as[0] = GO_TO_SOURCE_ACTION;
        as[1] = null;
        System.arraycopy(oas, 0, as, 2, oas.length);
        as[as.length - 1] = CUSTOMIZE_ACTION;
        return as;
    }

    @Override
    public void performDefaultAction(NodeActionsProvider original, Object node) throws UnknownTypeException {
        if (node instanceof PythonBreakpoint) {
            goToSource((PythonBreakpoint) node);
        } else {
            original.performDefaultAction(node);
        }
    }

    public void addModelListener(ModelListener l) {
    }

    public void removeModelListener(ModelListener l) {
    }

    private static void goToSource(PythonBreakpoint b) {
        b.getLine().show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
    }

    @NbBundle.Messages("CTL_Breakpoint_Customizer_Title=Breakpoint Properties")
    private static void customize(PythonBreakpoint lb) {
        JComponent c = PythonBreakpointCustomizer.getCustomizerComponent(lb);
        HelpCtx helpCtx = HelpCtx.findHelp(c);
        if (helpCtx == null) {
            helpCtx = new HelpCtx("debug.add.breakpoint");
        }
        Controller cc;
        if (c instanceof ControllerProvider) {
            cc = ((ControllerProvider) c).getController();
        } else {
            cc = (Controller) c;
        }

        final Controller[] cPtr = new Controller[]{cc};
        final DialogDescriptor[] descriptorPtr = new DialogDescriptor[1];
        final Dialog[] dialogPtr = new Dialog[1];
        ActionListener buttonsActionListener = (ActionEvent ev) -> {
            if (descriptorPtr[0].getValue() == DialogDescriptor.OK_OPTION) {
                boolean ok = cPtr[0].ok();
                if (ok) {
                    dialogPtr[0].setVisible(false);
                }
            } else {
                dialogPtr[0].setVisible(false);
            }
        };
        DialogDescriptor descriptor = new DialogDescriptor(
                c,
                Bundle.CTL_Breakpoint_Customizer_Title(),
                true,
                DialogDescriptor.OK_CANCEL_OPTION,
                DialogDescriptor.OK_OPTION,
                DialogDescriptor.DEFAULT_ALIGN,
                helpCtx,
                buttonsActionListener
        );
        descriptor.setClosingOptions(new Object[]{});
        Dialog d = DialogDisplayer.getDefault().createDialog(descriptor);
        d.pack();
        descriptorPtr[0] = descriptor;
        dialogPtr[0] = d;
        d.setVisible(true);
    }
}
