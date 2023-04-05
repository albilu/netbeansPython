package org.netbeans.modules.python.testrunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.gsf.testrunner.ui.spi.TestCreatorConfiguration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Pair;

/**
 *
 * @author albilu
 */
public class PythonTestCreator extends TestCreatorConfiguration {

    @Override
    public boolean canHandleProject(String string) {
        return true;
    }

    @Override
    public void persistConfigurationPanel(TestCreatorConfiguration.Context cntxt) {
    }

    @Override
    public Object[] getTestSourceRoots(Collection<SourceGroup> clctn, FileObject fo) {
        List<Object> folders = new ArrayList<>();
        SourceGroup[] sourceGroups = ProjectUtils.getSources(FileOwnerQuery
                .getOwner(fo)).getSourceGroups("testsources");
        for (SourceGroup sg : sourceGroups) {
            if (!sg.contains(fo)) {
                if (!folders.contains(sg.getRootFolder())) {
                    folders.add(sg);
                }
            }
        }
        return folders.toArray();
    }

    @Override
    public Pair<String, String> getSourceAndTestClassNames(FileObject fo, boolean bln, boolean bln1) {
        // FIXME-NETBEANS: Needs to end absolutly with "Test". This is a bug
        // https://github.com/apache/netbeans/blob/c084119009d2e0f736f225d706bc1827af283501/php/php.codeception/src/org/netbeans/modules/php/codeception/create/CodeceptionTestCreatorConfiguration.java#L114
        return Pair.of(fo.getName(), FilenameUtils
                .removeExtension(FileUtil
                        .getRelativePath(FileOwnerQuery.getOwner(fo).getProjectDirectory(), fo))
                .replaceAll(Pattern.quote(File.separator), ".") + "Test");
    }

}
