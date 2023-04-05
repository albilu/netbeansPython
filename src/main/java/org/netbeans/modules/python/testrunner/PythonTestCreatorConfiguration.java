package org.netbeans.modules.python.testrunner;

import org.netbeans.modules.gsf.testrunner.ui.spi.TestCreatorConfiguration;
import org.netbeans.modules.gsf.testrunner.ui.spi.TestCreatorConfigurationProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = TestCreatorConfigurationProvider.class, position = 100)
public class PythonTestCreatorConfiguration implements TestCreatorConfigurationProvider {

    @Override
    public TestCreatorConfiguration createTestCreatorConfiguration(FileObject[] fos) {
        return new PythonTestCreator();
    }

}
