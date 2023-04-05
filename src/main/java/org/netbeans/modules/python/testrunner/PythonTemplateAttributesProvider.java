package org.netbeans.modules.python.testrunner;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.templates.CreateDescriptor;
import org.netbeans.api.templates.CreateFromTemplateAttributes;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
@ServiceProvider(service = CreateFromTemplateAttributes.class)
public class PythonTemplateAttributesProvider implements CreateFromTemplateAttributes {

    @Override
    public Map<String, ?> attributesFor(CreateDescriptor desc) {
        Map<String, Object> parameters = desc.getParameters();
        HashMap computedParams = new HashMap<>();
        computedParams.putAll(parameters);
        computedParams.putIfAbsent("className", desc.getProposedName());
        computedParams.putIfAbsent("methodName", "test_0");
        return computedParams;
    }

}
