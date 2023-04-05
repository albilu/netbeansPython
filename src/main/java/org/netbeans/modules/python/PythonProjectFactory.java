package org.netbeans.modules.python;

import java.io.IOException;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ProjectFactory.class)
public class PythonProjectFactory implements ProjectFactory2 {

    static final Icon IMAGE = ImageUtilities.image2Icon(ImageUtilities
            .loadImage("org/netbeans/modules/python/python-2.png", true));

    //Specifies when a project is a project, i.e.,
    //if "pyproject.toml" is present in a folder:
    @Override
    public boolean isProject(FileObject projectDirectory) {
        return isProject2(projectDirectory) != null;
    }

    //Specifies when the project will be opened, i.e., if the project exists:
    @Override
    public Project loadProject(FileObject dir, ProjectState state)
            throws IOException {
        return isProject(dir) ? new PythonProject(dir, state) : null;
    }

    @Override
    public void saveProject(final Project project) throws IOException,
            ClassCastException {
        // leave unimplemented for the moment
    }

    @Override
    public ProjectManager.Result isProject2(FileObject projectDirectory) {
        if (projectDirectory.getFileObject("pyproject.toml") != null
                || projectDirectory.getFileObject("setup.py") != null) {
            return new ProjectManager.Result(IMAGE);
        }
        return null;
    }

}
