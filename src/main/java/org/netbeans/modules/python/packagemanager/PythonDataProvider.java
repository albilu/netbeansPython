package org.netbeans.modules.python.packagemanager;

import javax.swing.tree.DefaultMutableTreeNode;
import org.javatuples.Triplet;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.swing.outline.RenderDataProvider;
import org.openide.util.ImageUtilities;

/**
 *
 * @author albilu
 */
public class PythonDataProvider implements RenderDataProvider {

    @Override
    public java.awt.Color getBackground(Object o) {
        return null;
    }

    @Override
    public String getDisplayName(Object o) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
        Triplet leafNode = (Triplet) node
                .getUserObject();
        return leafNode.getValue0().toString();

    }

    @Override
    public java.awt.Color getForeground(Object o) {
        return null;
    }

    @Override
    public javax.swing.Icon getIcon(Object o) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
        if (node.isLeaf()) {
            return ImageUtilities.image2Icon(PythonUtility.getPythonPackageIcon().getImage());

        }
        return null;
    }

    @Override
    public String getTooltipText(Object o) {
        return null;
    }

    @Override
    public boolean isHtmlDisplayName(Object o) {
        return false;
    }

}
