package org.netbeans.modules.python.packagemanager;

import javax.swing.tree.DefaultMutableTreeNode;
import org.javatuples.Triplet;

/**
 *
 * @author albilu
 */
public class PythonRootTreeNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    Triplet node;

    public PythonRootTreeNode(Triplet node) {
        this.node = node;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getUserObject() {
        return node;
    }

    @Override
    public String toString() {
        return node.getValue0().toString();
    }

}
