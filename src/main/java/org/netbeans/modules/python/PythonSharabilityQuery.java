package org.netbeans.modules.python;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;

/**
 *
 * @author albilu
 */
public class PythonSharabilityQuery implements SharabilityQueryImplementation2 {

    @Override
    public SharabilityQuery.Sharability getSharability(URI uri) {
        if (StringUtils.endsWithAny(uri.toString().replaceFirst("/$", ""), PythonUtility.EXCLUDED_DIRS)
                || uri.toString().endsWith("-project.properties")) {
            return SharabilityQuery.Sharability.NOT_SHARABLE;
        } else {
            return SharabilityQuery.Sharability.UNKNOWN;
        }
    }

}
