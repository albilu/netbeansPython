package org.netbeans.modules.python.testrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.gototest.TestLocator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = org.netbeans.spi.gototest.TestLocator.class)
public class PythonTestLocator implements TestLocator {

    private static final RequestProcessor RP = new RequestProcessor(PythonTestLocator.class.getName(), 2);
    private static final Logger LOGGER = Logger.getLogger(PythonTestLocator.class.getName());

    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File fo1, File fo2) {
            return fo1.toPath().toString().compareTo(fo2.toPath().toString());
        }
    }

    static final Comparator<File> FILE_OBJECT_COMAPARTOR = new FileComparator();

    @Override
    public boolean appliesTo(FileObject fo) {
        Project project = findProject(fo);
        if (project == null) {
            LOGGER.log(Level.INFO, "Project was not found for file {0}", fo);
            return false;
        }
        return fo.getMIMEType().equals(PythonUtility.PYTHON_MIME_TYPE);
    }

    @CheckForNull
    private static Project findProject(FileObject file) {
        return FileOwnerQuery.getOwner(file);
    }

    @Override
    public boolean asynchronous() {
        return true;
    }

    @Override
    public LocationResult findOpposite(FileObject fo, int caretOffset) {
        throw new UnsupportedOperationException("Go To Test is asynchronous");
    }

    @Override
    public void findOpposite(FileObject fo, int caretOffset, LocationListener callback) {
        RP.post(() -> {
            callback.foundLocation(fo, findOpposite(fo));
        });
    }

    private LocationResult findOpposite(FileObject fo) {
        String mimeType = FileUtil.getMIMEType(fo);

        if (PythonUtility.PYTHON_MIME_TYPE.equals(mimeType)) {
            FileObject test = findOpposite(fo, true);

            if (test != null) {
                return new LocationResult(test, -1);
            } else {
                return new LocationResult("Test not found.");
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public FileType getFileType(FileObject fo) {
        String mimeType = FileUtil.getMIMEType(fo);
        String name = fo.getName();
        if (mimeType.equals(PythonUtility.PYTHON_MIME_TYPE)) {
            return FileType.TESTED;

        } else if (name.endsWith("Test")) {
            return FileType.TEST;
        }
        return FileType.NEITHER;
    }

    static @CheckForNull
    FileObject findOpposite(FileObject fo, boolean toTest) {
        boolean testedFile = fo.getName().endsWith("Test");
        Project owner = FileOwnerQuery
                .getOwner(fo);
        String projectDirectoryPath = Paths.get(owner.getProjectDirectory().getPath()).toString();
        SourceGroup[] sourceGroups = ProjectUtils.getSources(owner)
                .getSourceGroups(testedFile ? Sources.TYPE_GENERIC : "testsources");
        for (SourceGroup sg : sourceGroups) {
            File toFile = FileUtil.toFile(sg.getRootFolder());
            try {
                List<File> listFiles = Files.walk(toFile.toPath()).map(Path::toFile)
                        .filter(file -> file.isFile() && StringUtils.endsWith(/*
                                                                               * FilenameUtils
                                                                               * .getBaseName(file.getName())
                         */
                        FileUtil.toFile(fo).toPath().toString(),
                        !testedFile
                                ? StringUtils.remove(StringUtils.remove(file.toPath().toString(),
                                        toFile.toPath().toString()),
                                        "Test")
                                : StringUtils.remove(file.toPath().toString(), projectDirectoryPath).replace(".py",
                                        "Test.py")))
                        .collect(Collectors.toList());

                listFiles.sort(FILE_OBJECT_COMAPARTOR);
                return listFiles.isEmpty() ? null : FileUtil.toFileObject(listFiles.get(listFiles.size() - 1));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return null;
    }
}
