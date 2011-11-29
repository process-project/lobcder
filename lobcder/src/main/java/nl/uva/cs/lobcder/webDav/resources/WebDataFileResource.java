/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.resources.LogicalFile;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource implements
        com.bradmcevoy.http.FileResource {

    private final IDLCatalogue catalogue;
    private final ILogicalData logicalData;

    public WebDataFileResource(IDLCatalogue catalogue, ILogicalData logicalData) {
        this.catalogue = catalogue;
        this.logicalData = logicalData;
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + collectionResource.getName());
            debug("\t name: " + name);
            Path toCollectionLDRI = Path.path(collectionResource.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);
            LogicalFile newFolderEntry = new LogicalFile(newLDRI);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);

        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void delete() {
        try {
            catalogue.unregisterResourceEntry(logicalData);
        } catch (Exception ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Long getContentLength() {
        Metadata meta = logicalData.getMetadata();
        if (meta != null) {
            return meta.getLength();
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);
        if (accepts != null) {
            String[] acceptsTypes = accepts.split(",");
            if (logicalData.getMetadata() != null) {
                ArrayList<String> mimeTypes = logicalData.getMetadata().getMimeTypes();
                for (String accessType : acceptsTypes) {
                    for (String mimeType : mimeTypes) {
                        if (accessType.equals(mimeType)) {
                            return mimeType;
                        }
                    }
                }
                return mimeTypes.get(0);
            }
        }

        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException {
        debug("sendContent.");
        debug("\t range: " + range);
        debug("\t params: " + params);
        debug("\t contentType: " + contentType);
    }

    @Override
    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("moveTo.");
        debug("\t name: " + name);
        Path parent;
        Path tmpPath;


        debug("\t rDestgetName: " + rDest.getName() + " name: " + name);
//        if (rDest == null || rDest.getName() == null) {
//            debug("----------------Have to throw forbidden ");
//            throw new ForbiddenException(this);
//        }

        debug("\t rDestgetUniqueId: " + rDest.getUniqueId());
        Path newPath = Path.path(Path.path(rDest.getName()), name);
        if (newPath.isRelative()) {
            parent = logicalData.getLDRI().getParent();
            tmpPath = Path.path(parent, name);
            newPath = tmpPath;
        }
        try {
            debug("\t rename: " + logicalData.getLDRI() + " to " + newPath);
            catalogue.renameEntry(logicalData.getLDRI(), newPath);
        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
        }
    }

    @Override
    public String processForm(Map<String, String> arg0,
            Map<String, FileItem> arg1) throws BadRequestException,
            NotAuthorizedException {
        debug("processForm.");
        debug("\t arg0: " + arg0);
        debug("\t arg1: " + arg1);
        return null;
    }

    protected void debug(String msg) {
        System.err.println(this.getClass().getSimpleName() + "." + logicalData.getLDRI() + ": " + msg);
//        log.debug(msg);
    }

    @Override
    public String getUniqueId() {
        return logicalData.getUID();
    }

    @Override
    public String getName() {
        return logicalData.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        debug("authenticate.");
        debug("\t user: " + user);
        debug("\t password: " + password);
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        debug("authorise.");
        return true;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getModifiedDate() != null) {
            return new Date(logicalData.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (logicalData.isRedirectAllowed()) {
                    try {
                        //Replica selection algorithm 
                        return logicalData.getStorageSites().get(0).getStorageLocation().toURL().toString();
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return null;

            default:
                return null;
        }
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        if (logicalData.getMetadata() != null && logicalData.getMetadata().getCreateDate() != null) {
            return new Date(logicalData.getMetadata().getCreateDate());
        }
        return null;
    }
}
