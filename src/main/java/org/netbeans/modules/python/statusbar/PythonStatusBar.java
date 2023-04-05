package org.netbeans.modules.python.statusbar;

import java.awt.Component;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = StatusLineElementProvider.class)
public class PythonStatusBar implements StatusLineElementProvider {

    @Override
    public Component getStatusLineElement() {
        return new PythonStatusBarPanel();
    }

}
