package org.netbeans.modules.python;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class UpdateHandler {

    static final Pattern NB_VERSION = Pattern.compile("\\s+(\\d+)");

    public static void addUC() {

        String version = getVersion();
        if (version == null) {
            return;
        }

        String url = "https://raw.githubusercontent.com/albilu/netbeansPython/master/ppuc/"
                + version + "/updates.xml";

        try {
            UpdateUnitProviderFactory.getDefault()
                    .create("netbeansPythonUC", "netbeansPython Update Center", new URL(url));
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    public static String getVersion() {
        String netbeansProductVersion = System.getProperty("netbeans.productversion");
        Matcher matcher = NB_VERSION.matcher(netbeansProductVersion);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
