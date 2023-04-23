package org.netbeans.modules.python.source;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.Icon;
import org.apache.commons.lang3.math.NumberUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.indexing.PythonCustomIndexer;
import org.netbeans.modules.python.indexing.PythonIndexQuery;
import org.netbeans.spi.jumpto.type.SearchType;
import org.netbeans.spi.jumpto.type.TypeDescriptor;
import org.netbeans.spi.jumpto.type.TypeProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = TypeProvider.class)
public class PythonTypeProvider implements TypeProvider {

    private final AtomicBoolean canceled = new AtomicBoolean();
    private static final Logger LOG = Logger.getLogger(PythonTypeProvider.class.getName());

    @Override
    public String name() {
        return "python";
    }

    @Override
    public String getDisplayName() {
        return "Python Types";
    }

    @Override
    public void computeTypeNames(Context cntxt, Result result) {
        //FIXME-NETBEANS in doc: https://bits.netbeans.org/dev/javadoc/org-netbeans-modules-jumpto/org/netbeans/spi/jumpto/type/TypeProvider.html#computeTypeNames-org.netbeans.spi.jumpto.type.TypeProvider.Context-org.netbeans.spi.jumpto.type.TypeProvider.Result-
        //It is said that a first call is made with project in context then a second call with project=null
        //Only one call is made with project=null
        //
        //filtering doesnt works correctly in some case its a bit weird
        //Update:seems to be fix by goto options settings (similarity)
        String text = cntxt.getText();
        SearchType kind = cntxt.getSearchType();

        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
        List<PythonTypeDescriptor> typeDescriptors = new ArrayList<>();
        result.setHighlightText(text);

        for (Project openProject : openProjects) {
            if (!(openProject instanceof PythonProject)) {
                continue;
            }
            try {
                if (canceled.get()) {
                    LOG.fine("Search canceled");
                    return;
                }

                computeTypes(openProject.getProjectDirectory().toURL(), openProject, kind, text, typeDescriptors);

                String pythonStdLibPath = PythonUtility.getPythonStdLibPath(PythonUtility.getProjectPythonExe(openProject.getProjectDirectory()));
                File file = new File(pythonStdLibPath);
                if (!file.exists()) {
                    return;
                }

                computeTypes(Utilities.toURI(file).toURL(), null, kind, text, typeDescriptors);

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        List<PythonTypeDescriptor> toJavaList = io.vavr.collection.List.ofAll(typeDescriptors)
                .distinctBy(PythonTypeDescriptor::getId)
                .toJavaList();

        result.addResult(toJavaList);
//        if (!typeDescriptors.isEmpty()) {
//            result.setMessage("Fuzzy search enabled for Python");
//
//        }
    }

    private void computeTypes(URL toURL, Project openProject, SearchType kind, String text, List<PythonTypeDescriptor> typeDescriptors) {
        Collection<? extends IndexResult> types = PythonIndexQuery
                .searchIndex(toURL, PythonCustomIndexer.TYPE_FIELD, kind,
                        text);
        for (IndexResult type : types) {
            for (Object value : type.getValues(PythonCustomIndexer.TYPE_FIELD)) {
                String[] split = value.toString().split("#");
                typeDescriptors.add(new PythonTypeDescriptor(split[0], openProject,
                        type.getFile(), NumberUtils.createInteger(split[1])));

            }

        }
    }

    @Override
    public void cancel() {
        canceled.set(true);
    }

    @Override
    public void cleanup() {
        canceled.set(false);
    }

    class PythonTypeDescriptor extends TypeDescriptor {

        String className;
        Project project;
        FileObject fo;
        int offset;

        public PythonTypeDescriptor(String className, Project project, FileObject fo, int offset) {
            this.className = className;
            this.project = project;
            this.fo = fo;
            this.offset = offset;

        }

        @Override
        public String getSimpleName() {
            return className;
        }

        @Override
        public String getOuterName() {
            return className;
        }

        @Override
        public String getTypeName() {
            return className;
        }

        @Override
        public String getContextName() {
            return project != null ? String.format("(%s)", Paths.get(project.getProjectDirectory()
                    .toURI()).relativize(Paths.get(fo.toURI())).toString())
                    : String.format("(%s)", Paths.get(fo.getPath()).toString());
        }

        @Override
        public Icon getIcon() {
            return PythonUtility.getClassIcon();
        }

        @Override
        public String getProjectName() {
            return project == null ? null : ProjectUtils.getInformation(this.project).getName();
        }

        @Override
        public Icon getProjectIcon() {
            return project == null ? null : PythonUtility.getPythonIcon();
        }

        @Override
        public FileObject getFileObject() {
            return fo;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public void open() {
            GsfUtilities.open(fo, offset, className);
        }

        public String getId() {
            return String.format("%s%s%s", className, Paths.get(fo.getPath()).toString(), offset);
        }

    }

}
