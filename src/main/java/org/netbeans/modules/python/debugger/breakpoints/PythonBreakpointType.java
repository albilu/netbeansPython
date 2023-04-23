package org.netbeans.modules.python.debugger.breakpoints;

import javax.swing.JComponent;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.debugger.ui.BreakpointType;
import org.netbeans.spi.debugger.ui.Controller;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

@NbBundle.Messages({"PythonBreakpointTypeCategory=Python",
    "LineBreakpointTypeName=Python Line"})
@BreakpointType.Registration(displayName = "#LineBreakpointTypeName")
public class PythonBreakpointType extends BreakpointType {

    private PythonBreakpointCustomizerPanel cust;

    /* (non-Javadoc)
     * @see org.netbeans.spi.debugger.ui.BreakpointType#getCategoryDisplayName()
     */
    @Override
    public String getCategoryDisplayName() {
        return Bundle.PythonBreakpointTypeCategory();
    }

    /* (non-Javadoc)
     * @see org.netbeans.spi.debugger.ui.BreakpointType#getCustomizer()
     */
    @Override
    public JComponent getCustomizer() {
        if (cust == null) {
            cust = new PythonBreakpointCustomizerPanel();
        }
        return cust;
    }

    @Override
    public Controller getController() {
        getCustomizer();
        return cust.getController();
    }

    /* (non-Javadoc)
     * @see org.netbeans.spi.debugger.ui.BreakpointType#getTypeDisplayName()
     */
    @Override
    public String getTypeDisplayName() {
        return Bundle.LineBreakpointTypeName();
    }

    /* (non-Javadoc)
     * @see org.netbeans.spi.debugger.ui.BreakpointType#isDefault()
     */
    @Override
    public boolean isDefault() {
        FileObject mostRecentFile = EditorContextDispatcher.getDefault().getMostRecentFile();
        if (mostRecentFile == null) {
            return false;
        }
        String mimeType = mostRecentFile.getMIMEType();
        return PythonUtility.PYTHON_MIME_TYPE.equals(mimeType);
    }

}
