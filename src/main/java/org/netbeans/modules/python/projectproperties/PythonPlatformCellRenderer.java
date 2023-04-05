package org.netbeans.modules.python.projectproperties;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.netbeans.modules.python.PythonUtility;
import org.openide.util.Pair;

/**
 *
 * @author albilu
 */
public class PythonPlatformCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value != null) {
            if (value instanceof Pair) {
                label.setIcon(PythonUtility.getPythonIcon());
                label.setText(((Pair) value).first().toString());
                label.setToolTipText(((Pair) value).second().toString());
            } else {
                label.setIcon(value.toString().isBlank() ? null : PythonUtility.getPythonIcon());
                label.setText(value.toString());

            }
            label.setHorizontalTextPosition(JLabel.RIGHT);
        }
        return label;

    }
}
