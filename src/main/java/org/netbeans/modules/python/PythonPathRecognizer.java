package org.netbeans.modules.python;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.parsing.spi.indexing.PathRecognizer;
//import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
//@ServiceProvider(service = PathRecognizer.class)
public class PythonPathRecognizer extends PathRecognizer {

    @Override
    public Set<String> getSourcePathIds() {
        return Collections.singleton(ClassPath.SOURCE);
    }

    @Override
    public Set<String> getLibraryPathIds() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getBinaryLibraryPathIds() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getMimeTypes() {
        return new HashSet<>(Arrays.asList(PythonUtility.PYTHON_MIME_TYPE));
    }

}
