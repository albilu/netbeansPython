package org.netbeans.modules.python.debugger.breakpoints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.Customizer;
import javax.swing.JPanel;
import org.netbeans.spi.debugger.ui.Controller;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
public class PythonBreakpointCustomizer extends JPanel implements Customizer, Controller {

    private PythonBreakpoint b;
    private PythonBreakpointCustomizerPanel c;

    public PythonBreakpointCustomizer() {
    }

    @Override
    public void setObject(Object bean) {
        if (!(bean instanceof PythonBreakpoint)) {
            throw new IllegalArgumentException(bean.toString());
        }
        this.b = (PythonBreakpoint) bean;
        init(b);
    }

    private void init(PythonBreakpoint b) {
        c = getCustomizerComponent(b);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(c, gbc);
    }

    @NbBundle.Messages("ACSD_Breakpoint_Customizer_Dialog=Customize this breakpoint's properties")
    public static PythonBreakpointCustomizerPanel getCustomizerComponent(PythonBreakpoint lb) {
        PythonBreakpointCustomizerPanel c;
        c = new PythonBreakpointCustomizerPanel(lb);
        c.getAccessibleContext().setAccessibleDescription(Bundle.ACSD_Breakpoint_Customizer_Dialog());
        return c;
    }

    @Override
    public boolean ok() {
        Controller cc;
        cc = c.getController();
        return cc.ok();
    }

    @Override
    public boolean cancel() {
        Controller cc;
        cc = c.getController();
        return cc.cancel();
    }

}
