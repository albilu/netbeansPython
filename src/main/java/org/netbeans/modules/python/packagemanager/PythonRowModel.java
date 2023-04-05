package org.netbeans.modules.python.packagemanager;

import javax.swing.tree.DefaultMutableTreeNode;
import org.javatuples.Triplet;
import org.netbeans.swing.outline.RowModel;

/**
 *
 * @author albilu
 */
public class PythonRowModel implements RowModel {

    @Override
    public Class<String> getColumnClass(int column) {
        switch (column) {
            case 0:
                return String.class;
            default:
                assert false;
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Version";
            default:
                assert false;
        }
        return "";
    }

    @Override
    public Object getValueFor(Object node, int column) {
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) node;

        switch (column) {
            case 0:
                if (newNode.isLeaf()) {
                    Triplet triNode = (Triplet) newNode.getUserObject();
                    return triNode.getValue1();
                }
                return "";
            default:
                assert false;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public void setValueFor(Object node, int column, Object value) {
        //do nothing, nothing is editable
    }

}
