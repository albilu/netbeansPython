package org.netbeans.modules.python;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
public class PythonNodes {

    @StaticResource
    private static final String IMAGE = "org/netbeans/modules/python/package.png";

    @NodeFactory.Registration(projectType = "org-netbeans-modules-python", position = 1)
    public static NodeFactory sourceNode() {
        return new PythonSourceNodeFactory();
    }

    @NodeFactory.Registration(projectType = "org-netbeans-modules-python", position = 2)
    public static NodeFactory testsNode() {
        return new PythonTestNodeFactory();
    }

    @NodeFactory.Registration(projectType = "org-netbeans-modules-python", position = 5)
    public static NodeFactory importantNode() {
        return new PythonProjectFilesNodeFactory();
    }

    private static final class PythonSourceNodeFactory implements NodeFactory {

        @Override
        public NodeList createNodes(Project project) {

            //Optionally, only return a new node
            //if some item is in the project's lookup:
            //MyCoolLookupItem item = project.getLookup().lookup(MyCoolLookupItem.class);
            //if (item != null) {
            try {
                final SourceFilesNode nd = new SourceFilesNode(project);
                return NodeFactorySupport.fixedNodeList(nd);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            //}

            //If the above try/catch fails, e.g.,
            //our item isn't in the lookup,
            //then return an empty list of nodes:
            return NodeFactorySupport.fixedNodeList();

        }

        @NbBundle.Messages("CTL_SourceName=Sources")
        public static class SourceFilesNode extends FilterNode {

            public SourceFilesNode(Project proj) throws DataObjectNotFoundException {
                super(DataObject.find(proj.getProjectDirectory()).getNodeDelegate(),
                        new SourcesChildren(DataObject.find(proj.getProjectDirectory()).getNodeDelegate(),
                                proj)
                );
            }

            @Override
            public String getDisplayName() {
                return Bundle.CTL_SourceName();
            }

            @Override
            public Image getIcon(int type) {
                Image original = super.getIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.assignToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

            @Override
            public Image getOpenedIcon(int type) {
                Image original = super.getOpenedIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.assignToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

        }

        private static class SourcesChildren extends FilterNode.Children implements ChangeListener {

            Project proj;

            public SourcesChildren(Node original, Project proj) {
                super(original);
                this.proj = proj;
                PythonPropertiesNotifier.addChangeListener(this);

            }

            @Override
            protected void addNotify() {
                super.addNotify();
                createNodes(original);
            }

            @Override
            protected Node[] createNodes(Node object) {
                List<Node> result = new ArrayList<>();
                for (Node node : super.createNodes(object)) {
                    if (accept(node)) {
                        result.add(node);
                    }
                }
                return result.toArray(Node[]::new);
            }

            private boolean accept(Node node) {
                String displayName = node.getDisplayName();
                SourceGroup[] sourceGroups = ProjectUtils.getSources(proj).getSourceGroups("testsources");
                boolean isTest = false;
                if (sourceGroups.length > 0) {
                    isTest = Paths.get(sourceGroups[0].getRootFolder().getPath()).endsWith(displayName);

                }
                return !(!VisibilityQuery.getDefault().isVisible(node.getLookup().lookup(FileObject.class))
                        || StringUtils.equalsAny(displayName, PythonUtility.EXCLUDED_DIRS)
                        || displayName.equals("pyproject.toml")
                        || isTest
                        || StringUtils.endsWithAny(displayName, PythonUtility.IMPORTANT_FILES)
                        || displayName.startsWith("."));
            }

            @Override
            public void stateChanged(ChangeEvent ce) {
                if (ProjectManager.getDefault().isModified(proj)) {
                    changeOriginal(original);
                }
            }

        }

    }

    public static class PythonTestNodeFactory implements NodeFactory {

        @Override
        public NodeList createNodes(Project project) {

            //Optionally, only return a new node
            //if some item is in the project's lookup:
            //MyCoolLookupItem item = project.getLookup().lookup(MyCoolLookupItem.class);
            //if (item != null) {
            try {
                for (SourceGroup testSourceGroup : ProjectUtils.getSources(project).getSourceGroups("testsources")) {
                    final TestFilesNode nd = new TestFilesNode(project,/*StringUtils
                        .equalsAny(tfo, "", "tests") ? project.getProjectDirectory()
                        .getFileObject("tests") : FileUtil.toFileObject(new File(tfo))*/ testSourceGroup.getRootFolder());
                    return NodeFactorySupport.fixedNodeList(nd);
                }
                return NodeFactorySupport.fixedNodeList(new TestFilesNode(project, project.getProjectDirectory()
                        .createFolder("tests")));

            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            //}

            //If the above try/catch fails, e.g.,
            //our item isn't in the lookup,
            //then return an empty list of nodes:
            return NodeFactorySupport.fixedNodeList();

        }

        @NbBundle.Messages("CTL_TestsName=Tests")
        public static class TestFilesNode extends FilterNode {

            public TestFilesNode(Project project, FileObject fo) throws DataObjectNotFoundException {
                super(DataObject.find(fo).getNodeDelegate(),
                        new TestsChildren(DataObject.find(fo).getNodeDelegate(), project
                        )
                );
            }

            @Override
            public String getDisplayName() {
                return Bundle.CTL_TestsName();
            }

            @Override
            public Image getIcon(int type) {
                Image original = super.getIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.assignToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

            @Override
            public Image getOpenedIcon(int type) {
                Image original = super.getOpenedIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.assignToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

        }

        private static class TestsChildren extends FilterNode.Children implements ChangeListener {

            Project project;

            public TestsChildren(Node original, Project project) {
                super(original);
                this.project = project;
                PythonPropertiesNotifier.addChangeListener(this);
            }

            @Override
            protected void addNotify() {
                super.addNotify();
                createNodes(original);
            }

            @Override
            protected Node[] createNodes(Node object) {
                List<Node> result = new ArrayList<>();
                for (Node node : super.createNodes(object)) {
                    if (accept(node)) {
                        result.add(node);
                    }
                }
                return result.toArray(Node[]::new);
            }

            private boolean accept(Node node) {
                return !StringUtils.equalsAny(node.getDisplayName(), PythonUtility.EXCLUDED_DIRS)
                        || VisibilityQuery.getDefault().isVisible(node.getLookup().lookup(FileObject.class));
            }

            @Override
            public void stateChanged(ChangeEvent ce) {
                if (ProjectManager.getDefault().isModified(project)) {
                    for (SourceGroup testSourceGroup : ProjectUtils.getSources(project).getSourceGroups("testsources")) {
                        try {
                            changeOriginal(DataObject.find(testSourceGroup.getRootFolder()).getNodeDelegate());
                        } catch (DataObjectNotFoundException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }

        }

    }

    public static class PythonProjectFilesNodeFactory implements NodeFactory {

        @Override
        public NodeList createNodes(Project project) {

            //Optionally, only return a new node
            //if some item is in the project's lookup:
            //MyCoolLookupItem item = project.getLookup().lookup(MyCoolLookupItem.class);
            //if (item != null) {
            try {
                ProjectFilesNode nd = new ProjectFilesNode(project);
                return NodeFactorySupport.fixedNodeList(nd);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            //}

            //If the above try/catch fails, e.g.,
            //our item isn't in the lookup,
            //then return an empty list of nodes:
            return NodeFactorySupport.fixedNodeList();

        }

        @NbBundle.Messages("CTL_ImportantFiles=Project Files")
        public static class ProjectFilesNode extends FilterNode {

            private static final String IMAGE = "org/netbeans/modules/python/config-badge.gif";

            public ProjectFilesNode(Project proj) throws DataObjectNotFoundException {
                super(DataObject.find(proj.getProjectDirectory()).getNodeDelegate(),
                        new ProjectFilesChildren(DataObject.find(proj.getProjectDirectory()).getNodeDelegate()));
            }

            @Override
            public String getDisplayName() {
                return Bundle.CTL_ImportantFiles();
            }

            @Override
            public Image getIcon(int type) {
                Image original = super.getIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.addToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

            @Override
            public Image getOpenedIcon(int type) {
                Image original = super.getOpenedIcon(type);
                original = ImageUtilities.mergeImages(original,
                        ImageUtilities.addToolTipToImage(ImageUtilities
                                .loadImage(IMAGE), "<i>" + getDisplayName() + "</i>"), 7, 7);

                return original;
            }

        }

        private static class ProjectFilesChildren extends FilterNode.Children {

            public ProjectFilesChildren(Node original) {
                super(original);
            }

            @Override
            protected Node[] createNodes(Node object) {
                List<Node> result = new ArrayList<>();
                for (Node node : super.createNodes(object)) {
                    if (accept(node)) {
                        result.add(node);
                    }
                    if (node.getDisplayName().equals("nbproject")) {
                        FileObject fileObject = node.getLookup()
                                .lookup(FileObject.class).getFileObject("project.properties");
                        if (fileObject != null) {
                            Node lookup = fileObject.getLookup().lookup(Node.class);
                            result.add(lookup);
                        }

                    }
                }
                return result.toArray(Node[]::new);
            }

            private boolean accept(Node node) {
                String displayName = node.getDisplayName();
                return StringUtils.endsWithAny(displayName, PythonUtility.IMPORTANT_FILES)
                        && VisibilityQuery.getDefault().isVisible(node.getLookup().lookup(FileObject.class));
            }

        }

    }

}
