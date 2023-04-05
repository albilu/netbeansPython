package org.netbeans.modules.python.indexing;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.netbeans.modules.parsing.spi.indexing.support.IndexingSupport;
import org.netbeans.modules.python.PythonUtility;
import org.openide.filesystems.FileObject;

/**
 *
 * @author albilu
 */
@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = CustomIndexerFactory.class)
public class PythonCustomIndexerFactory extends CustomIndexerFactory {

    public static final Logger LOG = Logger.getLogger(PythonCustomIndexerFactory.class.getName());

    @Override
    public CustomIndexer createIndexer() {
        return new PythonCustomIndexer();
    }

    @Override
    public boolean supportsEmbeddedIndexers() {
        return false;
    }

    @Override
    public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
        FileObject fo = context.getRoot();
        Project owner = fo != null ? FileOwnerQuery.getOwner(fo) : null;
        if (owner != null && owner.getClass()
                .getName().equals("org.netbeans.modules.python.PythonProject")) {
            try {
                IndexingSupport is = IndexingSupport.getInstance(context);
                for (Indexable indexable : deleted) {
                    is.removeDocuments(indexable);
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.log(Level.INFO, () -> "removed " + indexable.getRelativePath());
                    }
                }
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, null, ioe);
            }
        }
    }

    @Override
    public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
        FileObject fo = context.getRoot();
        Project owner = fo != null ? FileOwnerQuery.getOwner(fo) : null;
        if (owner != null && owner.getClass()
                .getName().equals("org.netbeans.modules.python.PythonProject")) {
            try {
                IndexingSupport is = IndexingSupport.getInstance(context);
                for (Indexable indexable : dirty) {
                    is.markDirtyDocuments(indexable);
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.log(Level.INFO, () -> "updated " + indexable.getRelativePath());
                    }
                }
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, null, ioe);
            }
        }
    }

    @Override
    public String getIndexerName() {
        return "PythonIndexer";
    }

    @Override
    public int getIndexVersion() {
        return 0;
    }

}
