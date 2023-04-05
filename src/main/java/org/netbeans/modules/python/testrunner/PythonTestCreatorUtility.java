package org.netbeans.modules.python.testrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.gsf.testrunner.api.TestCreatorProvider;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonTestCreatorUtility {

    public static void createTests(TestCreatorProvider.Context context, String template) {
        try {
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("methodName", "test_0");

            FileObject[] activatedFOs = context.getActivatedFOs();
            FileObject configFile = FileUtil.getConfigFile(template);
            DataObject dtemplate = DataObject.find(configFile);

            Path p = Paths.get(context.getTargetFolder().getPath());
            Path p2 = null;
            for (FileObject activatedFO : activatedFOs) {
                FileObject projectDirectory = FileOwnerQuery.getOwner(activatedFO).getProjectDirectory();
                if (activatedFO.isFolder() /*&& StringUtils
                        .containsAny(Paths.get(activatedFO.getPath()).toString(), PythonUtility.EXCLUDED_DIRS)*/) {
                    File fo = FileUtil.toFile(activatedFO);
                    Collection<File> listFiles = FileUtils.listFiles(fo, new String[]{"py"}, true);
                    for (File listFile : listFiles) {
                        String testClassName = FilenameUtils
                                .removeExtension(FileUtil
                                        .getRelativePath(projectDirectory, FileUtil.toFileObject(listFile)))
                                + "Test";
                        p2 = p.resolve(testClassName);
                        generateTests(p2, hashMap, dtemplate, activatedFOs, activatedFO);
                    }

                } else {
                    if (activatedFOs.length > 1) {
                        String testClassName = FilenameUtils
                                .removeExtension(FileUtil
                                        .getRelativePath(projectDirectory, activatedFO))
                                + "Test";
                        p2 = p.resolve(testClassName);
                    } else {
                        p2 = p.resolve(context.getTestClassName().replaceAll("\\.", File.separator));

                    }

                    generateTests(p2, hashMap, dtemplate, activatedFOs, activatedFO);
                }
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    public static void generateTests(Path p2, Map<String, Object> hashMap, DataObject dtemplate,
            FileObject[] activatedFOs, FileObject activatedFO) throws IOException {
        FileUtils.createParentDirectories(p2.toFile());
        hashMap.put("className", FilenameUtils.getBaseName(p2.toFile().getName()));
        DataObject dobj = dtemplate.createFromTemplate(
                DataFolder.findFolder(FileUtil.toFileObject(p2.toFile().getParentFile())),
                p2.toFile().getName(), hashMap);

        if (activatedFOs.length == 1 && !activatedFO.isFolder()) {
            dobj.getLookup().lookup(OpenCookie.class).open();

        }
    }

}
