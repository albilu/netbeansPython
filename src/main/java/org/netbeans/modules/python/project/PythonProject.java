package org.netbeans.modules.python.project;

/**
 *
 * @author albilu
 */
import com.electronwill.nightconfig.core.file.FileConfig;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.commons.io.FileUtils;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController;
import org.netbeans.modules.python.PythonPrivilegedTemplates;
import org.netbeans.modules.python.PythonSharabilityQuery;
import org.netbeans.modules.python.PythonSources;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.actions.PythonBuild;
import org.netbeans.modules.python.actions.PythonRun;
import org.netbeans.modules.python.coverage.PythonCodeCoverageProvider;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.modules.python.projectproperties.PythonCustomizerProvider;
import org.netbeans.modules.python.testrunner.PythonUnitTestRunner;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOrRenameOperationImplementation;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.SingleMethod;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeNotFoundException;
import org.openide.nodes.NodeOp;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public class PythonProject implements Project {

    @StaticResource()
    public static final String PYTHON_ICON = "org/netbeans/modules/python/python-2.png";

    public static final String POETRY = "poetry";
    public static final String PYTHON = "python";

    private final FileObject projectDir;
    private final ProjectState state;
    private Lookup lkp;

    PythonProject(FileObject dir, ProjectState state) {
        this.projectDir = dir;
        this.state = state;
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        if (lkp == null) {
            lkp = Lookups.fixed(new Object[]{ // register your features here
                new PythonInfo(),
                new PythonProjectOpenedHook(this),
                new PythonProjectLogicalView(this),
                new PythonCustomizerProvider(this),
                new PythonPrivilegedTemplates(),
                new PythonActionProvider(this),
                new PythonProjectMoveOrRenameOperation(),
                new PythonProjectCopyOperation(),
                new PythonProjectDeleteOperation(this),
                new PythonSources(this),
                new PythonSharabilityQuery(),
                new PythonCodeCoverageProvider(this),
                new PythonProjectStateHandler(state),
                new PythonProjectProblemProvider(this),}
            );
        }
        return lkp;
    }

    private final class PythonInfo implements ProjectInformation {

        @Override
        public Icon getIcon() {
            return new ImageIcon(ImageUtilities.loadImage(PYTHON_ICON));
        }

        @Override
        public String getName() {
            return getProjectDirectory().getName();
        }

        @Override
        public String getDisplayName() {
            return getName();
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener pcl) {
            // do nothing, won't change
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener pcl) {
            // do nothing, won't change
        }

        @Override
        public Project getProject() {
            return PythonProject.this;
        }

    }

    class PythonProjectLogicalView implements LogicalViewProvider {

        private final PythonProject project;

        public PythonProjectLogicalView(PythonProject project) {
            this.project = project;
        }

        @Override
        public Node createLogicalView() {
            try {
                // Obtain the project directory's node:
                FileObject projectDirectory = project.getProjectDirectory();
                DataFolder projectFolder = DataFolder.findFolder(projectDirectory);
                Node nodeOfProjectFolder = projectFolder.getNodeDelegate();
                // Decorate the project directory's node:
                return new ProjectNode(nodeOfProjectFolder, project);
            } catch (DataObjectNotFoundException donfe) {
                Exceptions.printStackTrace(donfe);
                // Fallback-the directory couldn't be created -
                // read-only filesystem or something evil happened
                return new AbstractNode(Children.LEAF);
            }
        }

        private final class ProjectNode extends FilterNode {

            final PythonProject project;

            public ProjectNode(Node node, PythonProject project)
                    throws DataObjectNotFoundException {
                super(node,
                        NodeFactorySupport.createCompositeChildren(
                                project,
                                "Projects/org-netbeans-modules-python/Nodes"
                        ),
                        new ProxyLookup(
                                new Lookup[]{
                                    Lookups.singleton(project),
                                    node.getLookup()
                                }));
                this.project = project;
            }

            @Override
            public Action[] getActions(boolean arg0) {
                return CommonProjectActions.forType("org-netbeans-modules-python");
            }

            @Override
            public Image getIcon(int type) {
                return ImageUtilities.loadImage(PYTHON_ICON);
            }

            @Override
            public Image getOpenedIcon(int type) {
                return getIcon(type);
            }

            @Override
            public String getDisplayName() {
                return project.getProjectDirectory().getName();
            }

            @Override
            public String getShortDescription() {
                FileObject projectDirectory = project.getProjectDirectory();
                FileObject fileObject = projectDirectory.getFileObject("pyproject.toml");
                if (fileObject == null) {
                    return super.getShortDescription();
                }
                try (FileConfig conf = FileConfig.of(FileUtil
                        .toFile(fileObject))) {
                    conf.load();
                    String loc = "Location: " + Paths.get(projectDirectory.getPath()).toString();
                    String desc = conf.getOrElse("project.description", "");
                    String pVersion = conf.getOrElse("project.version", "");
                    String pyVersion = PythonUtility.getVersion(PythonUtility
                            .getProjectPythonExe(projectDirectory));
                    StringBuilder s = new StringBuilder("<html>");
                    s.append(super.getShortDescription());
                    s.append("<br>");
                    s.append(loc);
                    if (!desc.isEmpty()) {
                        s.append("<br>");
                        s.append("Description: ");
                        s.append(desc);
                    }
                    if (!pVersion.isEmpty()) {
                        s.append("<br>");
                        s.append("Project version: ");
                        s.append(pVersion);
                    }
                    if (!pyVersion.isEmpty()) {
                        s.append("<br>");
                        s.append(pyVersion);
                    }
                    return s.append("</html>").toString();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return super.getShortDescription();
            }

        }

        // Copied from:
        // org.netbeans.modules.php.project.ui.logicalview.PhpLogicalViewProvider.java
        @Override
        public Node findPath(Node root, Object target) {
            Project p = root.getLookup().lookup(Project.class);
            if (p == null) {
                return null;
            }
            // Check each child node in turn.
            Node[] children = root.getChildren().getNodes(true);
            for (Node node : children) {
                if (target instanceof DataObject || target instanceof FileObject) {
                    FileObject kidFO = node.getLookup().lookup(FileObject.class);
                    if (kidFO == null) {
                        continue;
                    }
                    // Copied from
                    // org.netbeans.spi.java.project.support.ui.TreeRootNode.PathFinder.findPath:
                    FileObject targetFO = null;
                    if (target instanceof DataObject) {
                        targetFO = ((DataObject) target).getPrimaryFile();
                    } else {
                        targetFO = (FileObject) target;
                    }
                    Project owner = FileOwnerQuery.getOwner(targetFO);
                    if (!p.equals(owner)) {
                        return null; // Don't waste time if project does not own the fileobject
                    }
                    if (kidFO == targetFO) {
                        return node;
                    } else if (FileUtil.isParentOf(kidFO, targetFO)) {
                        String relPath = FileUtil.getRelativePath(kidFO, targetFO);

                        // first path without extension (more common case)
                        String[] path = relPath.split("/"); // NOI18N
                        path[path.length - 1] = targetFO.getName();

                        // first try to find the file without extension (more common case)
                        Node found = findNode(node, path);
                        if (found == null) {
                            // file not found, try to search for the name with the extension
                            path[path.length - 1] = targetFO.getNameExt();
                            found = findNode(node, path);
                        }
                        if (found == null) {
                            // can happen for tests that are underneath sources directory
                            continue;
                        }
                        if (hasObject(found, target)) {
                            return found;
                        }
                        Node parent = found.getParentNode();
                        Children kids = parent.getChildren();
                        children = kids.getNodes();
                        for (Node child : children) {
                            if (hasObject(child, target)) {
                                return child;
                            }
                        }
                    }
                }
            }
            return null;
        }

        private Node findNode(Node start, String[] path) {
            Node found = null;
            try {
                found = NodeOp.findPath(start, path);
            } catch (NodeNotFoundException ex) {
                // ignored
            }
            return found;
        }

        private boolean hasObject(Node node, Object obj) {
            if (obj == null) {
                return false;
            }
            FileObject fileObject = node.getLookup().lookup(FileObject.class);
            if (fileObject == null) {
                return false;
            }
            if (obj instanceof DataObject) {
                DataObject dataObject = node.getLookup().lookup(DataObject.class);
                if (dataObject == null) {
                    return false;
                }
                if (dataObject.equals(obj)) {
                    return true;
                }
                return hasObject(node, ((DataObject) obj).getPrimaryFile());
            } else if (obj instanceof FileObject) {
                return obj.equals(fileObject);
            }
            return false;
        }

    }

    private static final class PythonProjectMoveOrRenameOperation
            implements MoveOrRenameOperationImplementation {

        @Override
        public List<FileObject> getMetadataFiles() {
            return new ArrayList<>();
        }

        @Override
        public List<FileObject> getDataFiles() {
            return new ArrayList<>();
        }

        @Override
        public void notifyRenaming() throws IOException {
        }

        @Override
        public void notifyRenamed(String nueName) throws IOException {
        }

        @Override
        public void notifyMoving() throws IOException {
        }

        @Override
        public void notifyMoved(Project original, File originalPath,
                String nueName) throws IOException {
        }
    }

    private static final class PythonProjectCopyOperation
            implements CopyOperationImplementation {

        @Override
        public List<FileObject> getMetadataFiles() {
            return new ArrayList<>();
        }

        @Override
        public List<FileObject> getDataFiles() {
            return new ArrayList<>();
        }

        @Override
        public void notifyCopying() throws IOException {
        }

        @Override
        public void notifyCopied(Project prjct, File file, String string)
                throws IOException {
        }
    }

    private static final class PythonProjectDeleteOperation implements
            DeleteOperationImplementation {

        private final PythonProject project;

        private PythonProjectDeleteOperation(PythonProject project) {
            this.project = project;
        }

        @Override
        public List<FileObject> getMetadataFiles() {
            return new ArrayList<>();
        }

        @Override
        public List<FileObject> getDataFiles() {
            List<FileObject> files = new ArrayList<>();
            FileObject projectDirectory = project.getProjectDirectory();
            FileObject[] projectChildren = projectDirectory.getChildren();
            for (FileObject fileObject : projectChildren) {
                addFile(projectDirectory, fileObject.getNameExt(), files);
            }
            return files;
        }

        private void addFile(FileObject projectDirectory, String fileName,
                List<FileObject> result) {
            FileObject file = projectDirectory.getFileObject(fileName);
            if (file != null) {
                result.add(file);
            }
        }

        @Override
        public void notifyDeleting() throws IOException {
        }

        @Override
        public void notifyDeleted() throws IOException {
        }
    }

    class PythonActionProvider implements ActionProvider {

        PythonProject project;

        public PythonActionProvider(PythonProject project) {
            this.project = project;
        }

        @Override
        public String[] getSupportedActions() {
            return new String[]{
                ActionProvider.COMMAND_RENAME,
                ActionProvider.COMMAND_MOVE,
                ActionProvider.COMMAND_COPY,
                ActionProvider.COMMAND_DELETE,
                ActionProvider.COMMAND_RUN_SINGLE,
                ActionProvider.COMMAND_BUILD,
                ActionProvider.COMMAND_CLEAN,
                ActionProvider.COMMAND_TEST,
                ActionProvider.COMMAND_TEST_SINGLE,
                ActionProvider.COMMAND_RUN,
                SingleMethod.COMMAND_RUN_SINGLE_METHOD,//,
                ActionProvider.COMMAND_DEBUG,
                ActionProvider.COMMAND_DEBUG_SINGLE
            //TODO Enable Debug test actions
            //SingleMethod.COMMAND_DEBUG_SINGLE_METHOD
            };

        }

        @Override
        public void invokeAction(String string, Lookup lkp)
                throws IllegalArgumentException {
            switch (string) {
                case ActionProvider.COMMAND_RENAME:
                    DefaultProjectOperations.performDefaultRenameOperation(
                            project, "");
                    break;
                case ActionProvider.COMMAND_MOVE:
                    DefaultProjectOperations
                            .performDefaultMoveOperation(project);
                    break;
                case ActionProvider.COMMAND_COPY:
                    DefaultProjectOperations
                            .performDefaultCopyOperation(project);
                    break;
                case ActionProvider.COMMAND_DELETE:
                    DefaultProjectOperations
                            .performDefaultDeleteOperation(project);
                    break;
                case ActionProvider.COMMAND_RUN_SINGLE:
                    PythonRun.runAction(project, lkp.lookup(DataObject.class), false);
                    break;
                case ActionProvider.COMMAND_RUN:
                    try {
                    Properties prop = PythonUtility.getProperties(/*FileOwnerQuery
                            .getOwner(projectDir)*/project);
                    Object get = prop.getProperty("nbproject.run.script");
                    PythonRun.runAction(project, get != null
                            ? DataObject.find(FileUtil.toFileObject(new File(get
                                    .toString())))
                            : DataObject.find(projectDir.getFileObject("main.py")), false);
                } catch (DataObjectNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                break;
                case ActionProvider.COMMAND_TEST:
                    try {
                    SourceGroup[] sourceGroups = ProjectUtils.getSources(/*FileOwnerQuery
                            .getOwner(projectDir)*/project).getSourceGroups("testsources");
                    if (sourceGroups.length == 0) {
                        return;
                    }
                    new PythonUnitTestRunner().runAllTests(project,
                            DataObject.find(sourceGroups[0].getRootFolder()));
                } catch (DataObjectNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                }
                break;
                case ActionProvider.COMMAND_TEST_SINGLE:
                    new PythonUnitTestRunner().runSingleTest(project, lkp.lookup(DataObject.class));
                    break;
                case ActionProvider.COMMAND_BUILD:
                    PythonBuild.runAction(project, projectDir);
                    break;
                case ActionProvider.COMMAND_CLEAN:
                    FileUtils.deleteQuietly(FileUtil.toFile(projectDir).toPath().resolve("dist").toFile());
                    FileUtils.deleteQuietly(FileUtil.toFile(projectDir).toPath().resolve("build").toFile());
                    break;
                case SingleMethod.COMMAND_RUN_SINGLE_METHOD:
                    new PythonUnitTestRunner().runTestMethod(project,
                            lkp.lookup(TestMethodController.TestMethod.class));
                    break;
                case ActionProvider.COMMAND_DEBUG:
                    try {
                    Properties prop = PythonUtility.getProperties(/*FileOwnerQuery
                            .getOwner(projectDir)*/project);
                    Object get = prop.getProperty("nbproject.run.script");
                    PythonDebugger.startDebugger(project, get != null
                            ? DataObject.find(FileUtil.toFileObject(new File(get
                                    .toString())))
                            : DataObject.find(projectDir.getFileObject("main.py")), true);
                } catch (DataObjectNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                break;
                case ActionProvider.COMMAND_DEBUG_SINGLE:
                    PythonDebugger.startDebugger(project, lkp.lookup(DataObject.class), true);
                    break;

                default:
                    break;
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup lookup)
                throws IllegalArgumentException {
            switch (command) {
                case ActionProvider.COMMAND_RENAME:
                    return true;
                case ActionProvider.COMMAND_MOVE:
                    return true;
                case ActionProvider.COMMAND_COPY:
                    return true;
                case ActionProvider.COMMAND_DELETE:
                    return true;
                case ActionProvider.COMMAND_RUN_SINGLE:
                    FileObject fo = lookup.lookup(DataObject.class).getPrimaryFile();
                    return !fo.isFolder() && fo.getMIMEType().equals(PythonUtility.PYTHON_MIME_TYPE);
                case ActionProvider.COMMAND_RUN:
                    Properties prop = null;
                    try {
                        prop = PythonUtility.getProperties(/*FileOwnerQuery.getOwner(projectDir)*/project);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    return projectDir.getFileObject("main.py") != null
                            || (prop.getProperty("nbproject.run.script") != null
                            && FileUtil.toFileObject(new File(prop
                                    .getProperty("nbproject.run.script")
                                    .toString())) != null);
                case ActionProvider.COMMAND_BUILD:
                    return projectDir.getFileObject("pyproject.toml") != null
                            || projectDir.getFileObject("setup.py") != null;
                case ActionProvider.COMMAND_CLEAN:
                    return projectDir.getFileObject("dist") != null;
                case ActionProvider.COMMAND_TEST:
                    SourceGroup[] sourceGroups = ProjectUtils.getSources(/*FileOwnerQuery
                            .getOwner(projectDir)*/project).getSourceGroups("testsources");
                    return sourceGroups.length > 0 && sourceGroups[0].getRootFolder() != null;

                case ActionProvider.COMMAND_TEST_SINGLE:
                    FileObject fo1 = lookup.lookup(DataObject.class).getPrimaryFile();
                    return !fo1.isFolder() && fo1.getMIMEType().equals(PythonUtility.PYTHON_MIME_TYPE);

                case SingleMethod.COMMAND_RUN_SINGLE_METHOD:
                    return true;
                /*case SingleMethod.COMMAND_DEBUG_SINGLE_METHOD:
                    return true;*/
                case ActionProvider.COMMAND_DEBUG:
                    return true;
                case ActionProvider.COMMAND_DEBUG_SINGLE:
                    return true;
                default:
                    break;
            }
            return false;
        }
    }
}
