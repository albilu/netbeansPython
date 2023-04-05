package org.netbeans.modules.python;

/**
 *
 * @author albilu
 */
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

public class PythonPropertiesNotifier {

    private static final ChangeSupport cs = new ChangeSupport(PythonPropertiesNotifier.class);

    public static void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    public static void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public static void firePropertiesChange() {
        cs.fireChange();
    }

}
