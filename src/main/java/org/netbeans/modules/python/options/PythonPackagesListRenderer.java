package org.netbeans.modules.python.options;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import org.openide.util.ImageUtilities;
import org.openide.util.Pair;

/**
 *
 * @author albilu
 */
public class PythonPackagesListRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;
    static final Icon ICON = ImageUtilities.image2Icon(ImageUtilities
            .loadImage("org/netbeans/modules/python/passed.png", false));
    static final Icon ICON_ERROR = ImageUtilities.image2Icon(ImageUtilities
            .loadImage("org/netbeans/modules/python/test-error_16.png", false));
    static final Icon ICON_WARN = ImageUtilities.image2Icon(ImageUtilities
            .loadImage("org/netbeans/modules/python/warning.svg", false));

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        Pair<String, Boolean> pyPackage = (Pair<String, Boolean>) value;
        label.setText(pyPackage.first());
        label.setHorizontalTextPosition(JLabel.LEFT);
        if (pyPackage.second()) {
            label.setToolTipText("Installed");
            label.setIcon(ICON);
        } else if (pyPackage.first().equals("pynguin")) {
            label.setToolTipText("Not Installed");
            label.setIcon(ICON_WARN);
        } else {
            label.setToolTipText("Not Installed");
            label.setIcon(ICON_ERROR);

        }
        return label;
    }

}
