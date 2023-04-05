@TemplateRegistrations({
    @TemplateRegistration(
            folder = "Python",
            content = "Python.py",
            displayName = "#Python_file",
            description = "PythonDescription.html",
            createHandlerClass = org.netbeans.api.templates.CreateFromTemplateHandler.class,
            scriptEngine = "freemarker",
            requireProject = false
    ),
    @TemplateRegistration(
            folder = "Python",
            content = "EmptyPython.py",
            displayName = "#Python_file_1",
            description = "PythonDescription.html",
            createHandlerClass = org.netbeans.api.templates.CreateFromTemplateHandler.class,
            scriptEngine = "freemarker",
            requireProject = false
    ),
    @TemplateRegistration(
            folder = "Python",
            content = "PythonUnittest.py",
            displayName = "#Python_Unittest_file",
            description = "PythonUnitTestDescription.html",
            createHandlerClass = org.netbeans.api.templates.CreateFromTemplateHandler.class,
            scriptEngine = "freemarker",
            requireProject = false
    ),
    @TemplateRegistration(
            folder = "Python",
            content = "PythonPytest.py",
            displayName = "#Python_Pytest_file",
            description = "PythonPyTestDescription.html",
            createHandlerClass = org.netbeans.api.templates.CreateFromTemplateHandler.class,
            scriptEngine = "freemarker",
            requireProject = false
    )
})
@OptionsPanelController.ContainerRegistration(
        id = "PythonOptions",
        categoryName = "#OptionsCategory_Name_PythonOptions",
        iconBase = "org/netbeans/modules/python/python32.png",
        keywords = "#OptionsCategory_Keywords_PythonOptions",
        keywordsCategory = "PythonOptions"
)
@NbBundle.Messages(
        value = {
            "OptionsCategory_Name_PythonOptions=Python",
            "OptionsCategory_Keywords_PythonOptions=Python",
            "Python_file=Python File",
            "Python_file_1=Empty Python File",
            "Python_Unittest_file=Python Unit Test File",
            "Python_Pytest_file=Python PyTest File"
        }
)
package org.netbeans.modules.python;

import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.api.templates.TemplateRegistrations;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
