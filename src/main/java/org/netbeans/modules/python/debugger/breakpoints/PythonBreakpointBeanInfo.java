package org.netbeans.modules.python.debugger.breakpoints;

import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;

/**
 *
 * @author albilu
 */
public class PythonBreakpointBeanInfo extends SimpleBeanInfo {

    private static final Logger LOG = Logger.getLogger(PythonBreakpointBeanInfo.class.getName());

    @Override
    public BeanDescriptor getBeanDescriptor() {
        Class customizer = null;
        try {
            customizer = Class.forName("org.netbeans.modules.python.debugger.breakpoints.PythonBreakpointCustomizer",
                    true, Lookup.getDefault().lookup(ClassLoader.class));
        } catch (ClassNotFoundException cnfex) {
            LOG.log(Level.WARNING, "No BP customizer", cnfex);
        }
        return new BeanDescriptor(
                PythonBreakpoint.class,
                customizer);
    }

}
