package org.netbeans.modules.python.packagemanager;

import java.io.IOException;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import org.javatuples.Triplet;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.statusbar.PythonStatusBarPanel;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonTreeWillExpand implements TreeWillExpandListener {

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (event.getPath().getLastPathComponent());
        if (node.getChildCount() > 1) {
            return;
        }
        Triplet name = (Triplet) node.getUserObject();
        switch (name.getValue0().toString()) {
            case "Installed":
                try {
                PythonPackagesModel.loadInstalled(PythonStatusBarPanel.currentPyPath.isEmpty()
                        ? PythonUtility.getPlatformPythonExe() : PythonStatusBarPanel.currentPyPath);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            break;
            case "PyPI":
                PythonPackagesModel.loadPyPI(node);
                break;
            default:
                PythonPackagesModel.loadUserRepos(node);
                break;
        }

    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (event.getPath().getLastPathComponent());
        PythonPackagesModel.collapse(node);
    }

}
