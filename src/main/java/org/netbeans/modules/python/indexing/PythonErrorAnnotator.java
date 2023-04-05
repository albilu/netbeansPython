package org.netbeans.modules.python.indexing;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.Action;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.masterfs.providers.AnnotationProvider;
import org.netbeans.modules.masterfs.providers.InterceptionListener;
import org.netbeans.modules.parsing.impl.indexing.errors.Utilities;
import org.netbeans.modules.parsing.spi.indexing.ErrorsCache;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import static org.openide.util.ImageUtilities.assignToolTipToImage;
import static org.openide.util.ImageUtilities.loadImage;
import static org.openide.util.ImageUtilities.mergeImages;
import org.openide.util.Lookup;
import static org.openide.util.NbBundle.getMessage;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author albilu
 */
//FIXME-BUG: Notice weird slowness on large directories (but not on debug mode) => contribution-welcome
@ServiceProvider(service = org.netbeans.modules.masterfs.providers.AnnotationProvider.class,
        supersedes = "org.netbeans.modules.parsing.ui.indexing.errors$AnnotationProvider", position = 100)
public class PythonErrorAnnotator extends AnnotationProvider {

    private static final Logger LOG = Logger.getLogger(PythonErrorAnnotator.class.getName());

    @StaticResource
    private static final String ERROR_BADGE_URL = "org/netbeans/modules/python/error-badge.png";

    final String PACKAGE_BADGE = getClass().getResource("/org/netbeans/modules/python/package.png").toExternalForm();
    private static final String IMAGE = "org/netbeans/modules/python/package.png";

    @Override
    public String annotateName(String name, Set files) {
        return null;
    }

    @Override
    public Image annotateIcon(Image icon, int iconType, Set<? extends FileObject> files) {
        FileObject fo = files.iterator().next();
        Project owner = FileOwnerQuery.getOwner(fo);

        if (!Utilities.isBadgesEnabled() || owner == null || !owner.getClass().getName()
                .equals("org.netbeans.modules.python.PythonProject")) {
            return null;
        }
        boolean inError = false;
        boolean singleFile = files.size() == 1;

        if (files instanceof NonRecursiveFolder) {
            FileObject folder = ((NonRecursiveFolder) files).getFolder();
            inError = isInError(folder, false, true);
            singleFile = false;
        } else {
            for (Object o : files) {
                if (o instanceof FileObject) {
                    FileObject f = (FileObject) o;

                    if (f.isFolder()) {
                        singleFile = false;
                        if (isInError(f, true, !inError)) {
                            inError = true;
                            continue;
                        }
                        if (inError) {
                            continue;
                        }
                    } else {
                        if (f.isData()) {
                            if (isInError(f, true, !inError)) {
                                inError = true;
                            }
                        }
                    }
                }
            }
        }

        if (Logger.getLogger(PythonErrorAnnotator.class.getName()).isLoggable(Level.FINE)) {
            Logger.getLogger(PythonErrorAnnotator.class.getName()).log(Level.FINE, "files={0}, in error={1}", new Object[]{files, inError});
        }

        if (owner.getClass().getName()
                .equals("org.netbeans.modules.python.PythonProject")
                && files.size() == 1 && fo.isFolder() && fo.getFileObject("__init__.py") != null) {
            icon = ImageUtilities.mergeImages(icon, ImageUtilities
                    .addToolTipToImage(ImageUtilities.loadImage(IMAGE), "<img src='"
                            + PACKAGE_BADGE + "' width='8' height='8'>"
                            + "&nbsp;Python Package"), 7, 7);

        }
        if (inError) {
            //badge:
            URL errorBadgeIconURL = PythonErrorAnnotator.class.getResource("/" + ERROR_BADGE_URL);
            assert errorBadgeIconURL != null;
            String errorBadgeSingleTP = "<img src=\"" + errorBadgeIconURL + "\">&nbsp;" + getMessage(PythonErrorAnnotator.class, "TP_ErrorBadgeSingle");
            Image errorBadge = loadImage(ERROR_BADGE_URL);
            assert errorBadge != null;
            String errorBadgeFolderTP = "<img src=\"" + errorBadgeIconURL + "\">&nbsp;" + getMessage(PythonErrorAnnotator.class, "TP_ErrorBadgeFolder");
            Image i = mergeImages(icon, singleFile ? assignToolTipToImage(errorBadge, errorBadgeSingleTP) : assignToolTipToImage(errorBadge, errorBadgeFolderTP), 0, 8);
            Iterator<? extends AnnotationProvider> it = Lookup.getDefault().lookupAll(AnnotationProvider.class).iterator();
            boolean found = false;

            while (it.hasNext()) {
                AnnotationProvider p = it.next();
                if (found) {
                    Image res = p.annotateIcon(i, iconType, files);
                    if (res != null) {
                        return res;
                    }
                } else {
                    found = p == this;
                }
            }
            return i;
        }
        return icon;
    }

    @Override
    public String annotateNameHtml(String name, Set files) {
        return null;
    }

    @Override
    public Action[] actions(Set files) {
        return null;
    }

    @Override
    public InterceptionListener getInterceptionListener() {
        return null;
    }

    private synchronized boolean isInError(FileObject file, boolean recursive, boolean forceValue) {
        boolean result = false;
        Project owner = FileOwnerQuery.getOwner(file);
        if (owner == null) {
            return result;

        }
        URL toURL = owner.getProjectDirectory().toURL();
        PythonIndexQuery.queryPerFile(toURL, file, "has_errors");
        try {
            Collection<? extends URL> allFilesInError = ErrorsCache.getAllFilesInError(toURL);
            Stream<? extends URL> filter = allFilesInError.stream().filter(url -> url.toString() != null
                    && StringUtils.startsWith(url.toString(), file.toURL().toString()));
            if (filter.count() > 0) {
                result = true;

            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return result;
    }
}
