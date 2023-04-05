package org.netbeans.modules.python.options;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.javatuples.Quartet;

/**
 *
 * @author albilu
 */
public class PythonPackageRepoRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        label.setText(((Quartet) value).getValue0().toString());
        label.setHorizontalTextPosition(JLabel.RIGHT);
        return label;
    }
}
