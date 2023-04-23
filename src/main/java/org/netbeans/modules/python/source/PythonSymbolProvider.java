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
import org.netbeans.spi.jumpto.symbol.SymbolDescriptor;
import org.netbeans.spi.jumpto.symbol.SymbolProvider;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = SymbolProvider.class)
public class PythonSymbolProvider implements SymbolProvider {

    private final AtomicBoolean canceled = new AtomicBoolean();
    private static final Logger LOG = Logger.getLogger(PythonSymbolProvider.class.getName());

    @Override
    public String name() {
        return "python";
    }

    @Override
    public String getDisplayName() {
        return "Python Symbols";
    }

    @Override
    public void computeSymbolNames(Context cntxt, Result result) {
        String text = cntxt.getText();
        SearchType kind = cntxt.getSearchType();

        Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
        List<PythonSymbolDescriptor> symbolDescriptors = new ArrayList<>();
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

                computeSymbols(openProject.getProjectDirectory().toURL(), openProject, kind, text, symbolDescriptors);

                String pythonStdLibPath = PythonUtility.getPythonStdLibPath(PythonUtility.getProjectPythonExe(openProject.getProjectDirectory()));
                File file = new File(pythonStdLibPath);
                if (!file.exists()) {
                    return;
                }

                computeSymbols(Utilities.toURI(file).toURL(), null, kind, text, symbolDescriptors);

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        List<PythonSymbolDescriptor> toJavaList = io.vavr.collection.List.ofAll(symbolDescriptors)
                .distinctBy(PythonSymbolDescriptor::getId)
                .toJavaList();

        result.addResult(toJavaList);
//        if (!symbolDescriptors.isEmpty()) {
//            result.setMessage("Fuzzy search enabled for Python");
//
//        }
    }

    private void computeSymbols(URL toURL, Project openProject, SearchType kind, String text, List<PythonSymbolDescriptor> typeDescriptors) {
        Collection<? extends IndexResult> types = PythonIndexQuery
                .searchIndex(toURL, PythonCustomIndexer.SYMBOLS_FIELD, kind,
                        text);
        for (IndexResult type : types) {
            for (Object value : type.getValues(PythonCustomIndexer.SYMBOLS_FIELD)) {
                String[] split = value.toString().split("#");
                typeDescriptors.add(new PythonSymbolDescriptor(split[0], openProject,
                        type.getFile(), NumberUtils.createInteger(split[1]), split[2]));

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

    class PythonSymbolDescriptor extends SymbolDescriptor {

        String methodName;
        Project project;
        FileObject fo;
        int offset;
        String className;

        public PythonSymbolDescriptor(String methodName, Project project, FileObject fo, int offset, String className) {
            this.methodName = methodName;
            this.project = project;
            this.fo = fo;
            this.offset = offset;
            this.className = className;

        }

        @Override
        public Icon getIcon() {
            return PythonUtility.getMethodIcon();
        }

        @Override
        public String getSymbolName() {
            return methodName;
        }

        @Override
        public String getOwnerName() {
            return className;
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
            GsfUtilities.open(fo, offset, methodName);
        }

        public String getId() {
            return String.format("%s%s%s", methodName, Paths.get(fo.getPath()).toString(), offset);
        }

    }

}
