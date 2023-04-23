package org.netbeans.modules.python.debugger.models;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Action;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.modules.python.debugger.PythonDebugger.StateListener;
import org.netbeans.modules.python.debugger.PythonDebuggerCallStack;
import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.NodeActionsProvider;
import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.text.Line;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;

@DebuggerServiceRegistration(path = "PYthonSession/CallStackView", types = {TreeModel.class, NodeModel.class, NodeActionsProvider.class, TableModel.class})
public class PythonDebuggerCallStackModel implements TreeModel, NodeModel, NodeActionsProvider, TableModel,
        StateListener {

    private static final String CALL_STACK
            = "org/netbeans/modules/debugger/resources/callStackView/NonCurrentFrame";
    private static final String CURRENT_CALL_STACK
            = "org/netbeans/modules/debugger/resources/callStackView/CurrentFrame";

    @NbBundle.Messages("CTL_CallStackModel_noStack=No Stack Information")
    private static final Object[] NO_STACK = new Object[]{Bundle.CTL_CallStackModel_noStack()};

    private final PythonDebugger debugger;
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    public PythonDebuggerCallStackModel(ContextProvider contextProvider) {
        debugger = contextProvider.lookupFirst(null, PythonDebugger.class);
        debugger.addStateListener(WeakListeners.create(StateListener.class, this, debugger));
    }

    // TreeModel implementation ................................................
    /**
     * Returns the root node of the tree or null, if the tree is empty.
     *
     * @return the root node of the tree or null
     */
    @Override
    public Object getRoot() {
        return ROOT;
    }

    /**
     * Returns children for given parent on given indexes.
     *
     * @param parent a parent of returned nodes
     * @param from a start index
     * @param to a end index
     *
     * @throws NoInformationException if the set of children can not be resolved
     * @throws ComputingException if the children resolving process is time
     * consuming, and will be performed off-line
     * @throws UnknownTypeException if this TreeModel implementation is not able
     * to resolve children for given node type
     *
     * @return children for given parent on given indexes
     */
    @Override
    public Object[] getChildren(Object parent, int from, int to)
            throws UnknownTypeException {
        if (parent == ROOT) {
            List callStacks = debugger.getCallStacks();
            if (callStacks == null) {
                return NO_STACK;
            }
            Collections.reverse(callStacks);
            PythonDebuggerUtils.markStacks(callStacks);
            if (!callStacks.isEmpty()) {
                PythonDebuggerCallStack first = (PythonDebuggerCallStack) callStacks.get(0);
                first.makeCurrent();
            }
            return callStacks.toArray(PythonDebuggerCallStack[]::new);
        }
        throw new UnknownTypeException(parent);
    }

    /**
     * Returns true if node is leaf.
     *
     * @throws UnknownTypeException if this TreeModel implementation is not able
     * to resolve children for given node type
     * @return true if node is leaf
     */
    @Override
    public boolean isLeaf(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return false;
        }
        if (node instanceof PythonDebuggerCallStack) {
            return true;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns number of children for given node.
     *
     * @param node the parent node
     * @throws NoInformationException if the set of children can not be resolved
     * @throws ComputingException if the children resolving process is time
     * consuming, and will be performed off-line
     * @throws UnknownTypeException if this TreeModel implementation is not able
     * to resolve children for given node type
     *
     * @return true if node is leaf
     * @since 1.1
     */
    @Override
    public int getChildrenCount(Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return 1;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Registers given listener.
     *
     * @param l the listener to add
     */
    @Override
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    /**
     * Unregisters given listener.
     *
     * @param l the listener to remove
     */
    @Override
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }

    // NodeModel implementation ................................................
    /**
     * Returns display name for given node.
     *
     * @throws ComputingException if the display name resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve display name for given node type
     * @return display name for given node
     */
    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof PythonDebuggerCallStack) {
            PythonDebuggerCallStack frame = (PythonDebuggerCallStack) node;
            String name = String.format("%s:%s", frame.getName(), frame.getLine());
            return frame.isCurrent() ? PythonDebuggerUtils.toHTML(
                    name,
                    true,
                    false,
                    null
            ) : name;
        }
        if (node == ROOT) {
            return ROOT;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns icon for given node.
     *
     * @throws ComputingException if the icon resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve icon for given node type
     * @return icon for given node
     */
    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        if (node instanceof PythonDebuggerCallStack) {
            PythonDebuggerCallStack frame = (PythonDebuggerCallStack) node;
            return frame.isCurrent() ? CURRENT_CALL_STACK : CALL_STACK;
        }
        if (node == ROOT) {
            return null;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns tooltip for given node.
     *
     * @throws ComputingException if the tooltip resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve tooltip for given node type
     * @return tooltip for given node
     */
    @Override
    public String getShortDescription(Object node) throws UnknownTypeException {
        if (node instanceof PythonDebuggerCallStack) {
            PythonDebuggerCallStack frame = (PythonDebuggerCallStack) node;
            return String.format("%s:%s", frame.getSourceURI().toString(), frame.getLine());
        }
        throw new UnknownTypeException(node);
    }

    // NodeActionsProvider implementation ......................................
    /**
     * Performs default action for given node.
     *
     * @throws UnknownTypeException if this NodeActionsProvider implementation
     * is not able to resolve actions for given node type
     * @return display name for given node
     */
    @Override
    public void performDefaultAction(Object node) throws UnknownTypeException {
        if (node instanceof PythonDebuggerCallStack) {
            Line line = ((PythonDebuggerCallStack) node).location();
            if (line != null) {
                PythonDebuggerUtils.showLine(new Line[]{line});
            }
            ((PythonDebuggerCallStack) node).makeCurrent();
            return;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns set of actions for given node.
     *
     * @throws UnknownTypeException if this NodeActionsProvider implementation
     * is not able to resolve actions for given node type
     * @return display name for given node
     */
    @Override
    public Action[] getActions(Object node) throws UnknownTypeException {
        return new Action[]{};
    }

    // TableModel implementation ...............................................
    /**
     * Returns value to be displayed in column <code>columnID</code> and row
     * identified by <code>node</code>. Column ID is defined in by
     * {@link ColumnModel#getID}, and rows are defined by values returned from
     * {@link org.netbeans.spi.viewmodel.TreeModel#getChildren}.
     *
     * @param node a object returned from
     * {@link org.netbeans.spi.viewmodel.TreeModel#getChildren} for this row
     * @param columnID a id of column defined by {@link ColumnModel#getID}
     * @throws ComputingException if the value is not known yet and will be
     * computed later
     * @throws UnknownTypeException if there is no TableModel defined for given
     * parameter type
     *
     * @return value of variable representing given position in tree table.
     */
    @Override
    public Object getValueAt(Object node, String columnID) throws
            UnknownTypeException {
        if (columnID == null ? Constants.CALL_STACK_FRAME_LOCATION_COLUMN_ID == null
                : columnID.equals(Constants.CALL_STACK_FRAME_LOCATION_COLUMN_ID)) {
            if (node instanceof PythonDebuggerCallStack) {
                PythonDebuggerCallStack frame = (PythonDebuggerCallStack) node;
                URI sourceURI = frame.getSourceURI();
                if (sourceURI == null) {
                    return "";
                }
                String sourceName;
                try {
                    FileObject file = URLMapper.findFileObject(sourceURI.toURL());
                    sourceName = file.getPath();
                } catch (MalformedURLException ex) {
                    sourceName = sourceURI.toString();
                }
                int line = frame.getLine();
                if (line > 0) {
                    return sourceName + ':' + line;
                } else {
                    return sourceName + ":?";
                }
            }
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns true if value displayed in column <code>columnID</code> and row
     * <code>node</code> is read only. Column ID is defined in by
     * {@link ColumnModel#getID}, and rows are defined by values returned from
     * {@link TreeModel#getChildren}.
     *
     * @param node a object returned from {@link TreeModel#getChildren} for this
     * row
     * @param columnID a id of column defined by {@link ColumnModel#getID}
     * @throws UnknownTypeException if there is no TableModel defined for given
     * parameter type
     *
     * @return true if variable on given position is read only
     */
    @Override
    public boolean isReadOnly(Object node, String columnID) throws UnknownTypeException {
        if (columnID == null ? Constants.CALL_STACK_FRAME_LOCATION_COLUMN_ID == null
                : columnID.equals(Constants.CALL_STACK_FRAME_LOCATION_COLUMN_ID)) {
            if (node instanceof PythonDebuggerCallStack) {
                return true;
            }
            return true;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Changes a value displayed in column <code>columnID</code> and row
     * <code>node</code>. Column ID is defined in by {@link ColumnModel#getID},
     * and rows are defined by values returned from
     * {@link TreeModel#getChildren}.
     *
     * @param node a object returned from {@link TreeModel#getChildren} for this
     * row
     * @param columnID a id of column defined by {@link ColumnModel#getID}
     * @param value a new value of variable on given position
     * @throws UnknownTypeException if there is no TableModel defined for given
     * parameter type
     */
    @Override
    public void setValueAt(Object node, String columnID, Object value) throws UnknownTypeException {
        throw new UnknownTypeException(node);
    }

    // other mothods ...........................................................
    private void fireChanges() {
        ModelEvent.TreeChanged event = new ModelEvent.TreeChanged(this);
        for (ModelListener l : listeners) {
            l.modelChanged(event);
        }
    }

    @Override
    public void suspended(boolean suspended) {
        //fireChanges();
    }

    @Override
    public void finished() {
    }

    @Override
    public void currentThread() {
        fireChanges();
    }

    @Override
    public void currentFrame() {
        // fireChanges();
    }

}
