package org.netbeans.modules.python.projectproperties;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.netbeans.modules.python.PythonUtility;

/**
 *
 * @author albilu
 */
public class PythonFrameworkCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        switch ((String) value) {
            case ("unittest"):
                label.setIcon(PythonUtility.getPythonIcon());
                label.setHorizontalTextPosition(JLabel.RIGHT);
                break;
            case ("pytest"):
                label.setIcon(PythonUtility.getPytestIcon());
                label.setHorizontalTextPosition(JLabel.RIGHT);
                break;
            case ("pynguin"):
                label.setIcon(PythonUtility.getPynguinIcon());
                label.setHorizontalTextPosition(JLabel.RIGHT);
                break;
            default:
                throw new AssertionError();
        }
        return label;

    }
}
