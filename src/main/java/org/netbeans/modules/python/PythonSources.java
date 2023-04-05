package org.netbeans.modules.python;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.swing.event.ChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonSources implements Sources {

    Project project;

    public PythonSources(Project project) {
        this.project = project;
    }

    private final ChangeSupport changeSupport = new ChangeSupport(this);

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        if (Sources.TYPE_GENERIC.equals(type)) {
            ProjectInformation information = ProjectUtils.getInformation(project);
            return new SourceGroup[]{GenericSources.group(project, project.getProjectDirectory(),
                information.getName(), information.getDisplayName(), information.getIcon(),
                null
                )};
        }
        if ("testsources".equals(type)) {
            if (!OpenProjects.getDefault().isProjectOpen(project)) {
                return new SourceGroup[0];
            }
            try {
                Properties conf = PythonUtility.getConf(project);
                String tfo = conf.getProperty("nbproject.test.dir", "tests");
                ProjectInformation information = ProjectUtils.getInformation(project);
                FileObject fo = StringUtils.equalsAny(tfo, "", "tests") ? project.getProjectDirectory()
                        .getFileObject("tests") : FileUtil.toFileObject(new File(tfo));

                return fo == null ? new SourceGroup[0] : new SourceGroup[]{GenericSources.group(project, fo,
                    "Tests", "Tests", information.getIcon(),
                    null
                    )};
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        return new SourceGroup[0];
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

}
