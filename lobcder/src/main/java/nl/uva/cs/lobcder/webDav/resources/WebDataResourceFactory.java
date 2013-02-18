package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import java.io.File;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

public class WebDataResourceFactory implements ResourceFactory {

    private JDBCatalogue catalogue;
    private static final boolean debug = true;

    public WebDataResourceFactory(JDBCatalogue catalogue) throws Exception {
        this.catalogue = catalogue;
    }

    @Override
    public Resource getResource(String host, String strPath) {
        
        Path ldri = Path.path(strPath).getStripFirst().getStripFirst();
        try {
            //Gets the root path. If instead we called :'ldri = Path.path(strPath);' we get back '/lobcder-1.0-SNAPSHOT'
            debug("getResource:  strPath: " + strPath + " path: " + Path.path(strPath) + " ldri: " + ldri);
            debug("getResource:  host: " + host + " path: " + ldri);

            LogicalData entry = catalogue.getResourceEntryByLDRI(ldri, null);
            if (entry == null) {
                System.err.println("NULL!!!!!!!!!!!!!");
                return null; 
            }

            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                return new WebDataDirResource(catalogue, entry);
            }
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                return new WebDataFileResource(catalogue, entry);
            }
            return new WebDataResource(catalogue, entry);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
//        return null;
    }

    private void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + ": " + msg);
//        log.debug(msg);
        }
    }
}
