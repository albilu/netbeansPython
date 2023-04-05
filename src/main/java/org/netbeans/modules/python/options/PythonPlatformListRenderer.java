package org.netbeans.modules.python.options;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.javatuples.Triplet;

/**
 *
 * @author albilu
 */
public class PythonPlatformListRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        Triplet<String, String, Boolean> pyObject = (Triplet<String, String, Boolean>) value;
        label.setText(pyObject.getValue1());
        label.setHorizontalTextPosition(JLabel.LEFT);
        if (pyObject.getValue2()) {
            label.setIcon(PythonPackagesListRenderer.ICON);
        }
        if (!new File(pyObject.getValue0()).exists()) {
            label.setForeground(Color.GRAY);
        }
        return label;
    }

}
