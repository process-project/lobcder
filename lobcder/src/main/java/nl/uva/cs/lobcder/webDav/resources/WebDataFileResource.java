/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import nl.uva.cs.lobcder.util.Constants;
import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.*;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.PartialGetHelper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.CatalogueException;
import nl.uva.cs.lobcder.catalogue.IDLCatalogue;
import nl.uva.cs.lobcder.resources.ILogicalData;
import nl.uva.cs.lobcder.resources.IStorageSite;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.Metadata;
import nl.uva.cs.lobcder.util.MMTypeTools;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author S. Koulouzis
 */
public class WebDataFileResource implements
        com.bradmcevoy.http.FileResource {

    private final IDLCatalogue catalogue;
    private ILogicalData entry;
    private static final boolean debug = true;
    private String user;

    public WebDataFileResource(IDLCatalogue catalogue, ILogicalData logicalData) throws CatalogueException, Exception {
        this.catalogue = catalogue;
        this.entry = logicalData;
        if (!logicalData.getType().equals(Constants.LOGICAL_FILE)) {
            throw new Exception("The logical data has the wonrg type: " + logicalData.getType());
        }
        initMetadata();
    }

    @Override
    public void copyTo(CollectionResource collectionResource, String name) throws ConflictException {
        try {
            debug("copyTo.");
            debug("\t toCollection: " + collectionResource.getName());
            debug("\t name: " + name);
            Path toCollectionLDRI = Path.path(collectionResource.getName());
            Path newLDRI = Path.path(toCollectionLDRI, name);

            LogicalData newFolderEntry = new LogicalData(newLDRI, Constants.LOGICAL_FOLDER);
            newFolderEntry.getMetadata().setModifiedDate(System.currentTimeMillis());
            catalogue.registerResourceEntry(newFolderEntry);
        } catch (CatalogueException ex) {
            throw new ConflictException(this, ex.toString());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
        try {
            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites != null && !sites.isEmpty()) {
                for (IStorageSite s : sites) {
                    s.deleteVNode(entry.getPDRI());
                }
            }
            catalogue.unregisterResourceEntry(entry);
        } catch (CatalogueException ex) {
            throw new BadRequestException(this, ex.toString());
        } catch (VlException ex) {
            throw new BadRequestException(this, ex.toString());
        }
    }

    @Override
    public Long getContentLength() {
        Metadata meta = entry.getMetadata();
        if (meta != null) {
            return meta.getLength();
        }
        return null;
    }

    @Override
    public String getContentType(String accepts) {
        debug("getContentType. accepts: " + accepts);

        String type = "";
        ArrayList<String> fileContentTypes = null;
        if (entry.getMetadata() != null) {
            fileContentTypes = entry.getMetadata().getContentTypes();
        }

        if (accepts != null && fileContentTypes != null && !fileContentTypes.isEmpty()) {
            String[] acceptsTypes = accepts.split(",");
            Collection<String> acceptsList = new ArrayList<String>();
            acceptsList.addAll(Arrays.asList(acceptsTypes));

            for (String fileContentType : fileContentTypes) {
                type = MMTypeTools.bestMatch(acceptsList, fileContentType);
                debug("\t type: " + type);
                if (!StringUtil.isEmpty(type)) {
                    return type;
                }
            }
        } else {
            String regex = "(^.*?\\[|\\]\\s*$)";
            type = fileContentTypes.toString().replaceAll(regex, "");
            return type;
        }
        return null;
    }

    /**
     * Specifies a lifetime for the information returned by this header. A
     * client MUST discard any information related to this header after the
     * specified amount of time.
     *
     * @param auth
     * @return
     */
    @Override
    public Long getMaxAgeSeconds(Auth auth) {
        return null;
    }

    @Override
    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException,
            NotAuthorizedException, BadRequestException {
        InputStream in = null;
        debug("sendContent.");
        debug("\t range: " + range);
        debug("\t params: " + params);
        debug("\t contentType: " + contentType);

        try {

            VFile vFile;
            if (!entry.hasPhysicalData()) {
                vFile = (VFile) entry.createPhysicalData();
            } else {
                vFile = (VFile) entry.getVFSNode();
            }
            if (vFile == null) {
                throw new IOException("Could not locate physical data source for " + entry.getLDRI());
            }

            in = vFile.getInputStream();

            if (range != null) {
                debug("sendContent: ranged content: " + vFile.getVRL());
                PartialGetHelper.writeRange(in, range, out);
            } else {
                debug("sendContent: send whole file to " + vFile.getVRL());
                IOUtils.copy(in, out);
            }

        } catch (VlException ex) {
            throw new IOException(ex);
        } finally {
            out.flush();
            out.close();
            if (in != null) {
                in.close();
            }
        }

    }

    @Override
    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException, NotAuthorizedException, BadRequestException {
        debug("moveTo.");
        debug("\t name: " + name);
        Path parent;
        Path tmpPath;

        debug("\t rDestgetName: " + rDest.getName() + " name: " + name);

        Path dirPath = ((WebDataDirResource) rDest).getPath();
        debug("\t rDestgetUniqueId: " + rDest.getUniqueId());

        Path newPath = Path.path(dirPath, name);
        parent = entry.getLDRI().getParent();
        if (newPath.isRelative() && parent != null) {
            tmpPath = Path.path(parent, name);
            newPath = tmpPath;
        }
        try {
            debug("\t rename: " + entry.getLDRI() + " to " + newPath);
            catalogue.renameEntry(entry.getLDRI(), newPath);
            ILogicalData newLogicData = catalogue.getResourceEntryByLDRI(newPath);
            entry = newLogicData;

            WebDataDirResource dir = (WebDataDirResource) rDest;
            dir.setLogicalData(catalogue.getResourceEntryByLDRI(dirPath));


        } catch (Exception ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex.getMessage().contains("resource exists")) {
                throw new ConflictException(rDest, ex.getMessage());
            }
        }
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException {

        //Maybe we can do more smart things here with deltas. So if we update a file send only the diff
        debug("processForm.");
        debug("\t parameters: " + parameters);
        debug("\t files: " + files);
        Collection<FileItem> values = files.values();
        VFSNode node;
        OutputStream out;
        InputStream in;
        Metadata meta;
//        try {
//            for (FileItem i : values) {
//
//                debug("\t getContentType: " + i.getContentType());
//                debug("\t getFieldName: " + i.getFieldName());
//                debug("\t getName: " + i.getName());
//                debug("\t getSize: " + i.getSize());
//                
//                if (!logicalData.hasPhysicalData()) {
//                    node = logicalData.createPhysicalData();
//                    out = ((VFile)node).getOutputStream();
//                    in = i.getInputStream();
//                    IOUtils.copy(in, out);
////                     PartialGetHelper.writeRange(in, range, out);
//                    in.close();
//                    out.flush();
//                    out.close();
//                    meta = logicalData.getMetadata();
//                    meta.setLength(i.getSize());
//                    meta.addContentType(i.getContentType());
//                    meta.setModifiedDate(System.currentTimeMillis());
//                    logicalData.setMetadata(meta);
//                    
//                }else{
//                    throw new BadRequestException(this);
//                }
//            }
//        } catch (IOException ex) {
//            throw new BadRequestException(this);
//        } catch (VlException ex) {
//            throw new BadRequestException(this);
//        } finally {
//        }
        return null;
    }

    protected void debug(String msg) {
        if (debug) {
            System.err.println(this.getClass().getSimpleName() + "." + entry.getLDRI() + ": " + msg);
        }

//        log.debug(msg);
    }

    @Override
    public String getUniqueId() {
        return String.valueOf(entry.getUID());
    }

    @Override
    public String getName() {
        return entry.getLDRI().getName();
    }

    @Override
    public Object authenticate(String user, String password) {

        debug("authenticate.\n"
                + "\t user: " + user
                + "\t password: " + password);
        return user;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        String absPath = null;
        String absURL = null;
        String acceptHeader = null;
        String fromAddress = null;
        String remoteAddr = null;
        String cnonce = null;
        String nc = null;
        String nonce = null;
        String password = null;
        String qop = null;
        String relm = null;
        String responseDigest = null;
        String uri = null;
        String user = null;
        Object tag = null;
        if (request != null) {
            absPath = request.getAbsolutePath();
            absURL = request.getAbsoluteUrl();
            acceptHeader = request.getAcceptHeader();
            fromAddress = request.getFromAddress();
            remoteAddr = request.getRemoteAddr();
        }
        if (auth != null) {
            cnonce = auth.getCnonce();
            nc = auth.getNc();
            nonce = auth.getNonce();
            password = auth.getPassword();
            qop = auth.getQop();
            relm = auth.getRealm();
            responseDigest = auth.getResponseDigest();
            uri = auth.getUri();
            user = auth.getUser();
            tag = auth.getTag();
        }
        debug("authorise. \n"
                + "\t request.getAbsolutePath(): " + absPath + "\n"
                + "\t request.getAbsoluteUrl(): " + absURL + "\n"
                + "\t request.getAcceptHeader(): " + acceptHeader + "\n"
                + "\t request.getFromAddress(): " + fromAddress + "\n"
                + "\t request.getRemoteAddr(): " + remoteAddr + "\n"
                + "\t auth.getCnonce(): " + cnonce + "\n"
                + "\t auth.getNc(): " + nc + "\n"
                + "\t auth.getNonce(): " + nonce + "\n"
                + "\t auth.getPassword(): " + password + "\n"
                + "\t auth.getQop(): " + qop + "\n"
                + "\t auth.getRealm(): " + relm + "\n"
                + "\t auth.getResponseDigest(): " + responseDigest + "\n"
                + "\t auth.getUri(): " + uri + "\n"
                + "\t auth.getUser(): " + user + "\n"
                + "\t auth.getTag(): " + tag);

        if (canUseResource(user)) {
            this.user = user;
            Collection<IStorageSite> sites = entry.getStorageSites();
            if (sites == null || sites.isEmpty()) {
                try {
                    sites = (Collection<IStorageSite>) catalogue.getSitesByUname(user);
                    if (sites == null || sites.isEmpty()) {
                        debug("\t StorageSites for " + this.getName() + " are empty!");
                        throw new RuntimeException("User " + user + " has StorageSites for " + this.getName());
                    }
                    entry.setStorageSites(sites);
                } catch (CatalogueException ex) {
                    throw new RuntimeException(ex.getMessage());
                }
            }
        }
        return true;
    }

    @Override
    public String getRealm() {
        return "realm";
    }

    @Override
    public Date getModifiedDate() {
        debug("getModifiedDate.");
        if (entry.getMetadata() != null && entry.getMetadata().getModifiedDate() != null) {
            return new Date(entry.getMetadata().getModifiedDate());
        }
        return null;
    }

    @Override
    public String checkRedirect(Request request) {
        debug("checkRedirect.");
        switch (request.getMethod()) {
            case GET:
                if (entry.isRedirectAllowed()) {
                    //Replica selection algorithm 
                    return null;
                }
                return null;

            default:
                return null;
        }
    }

    @Override
    public Date getCreateDate() {
        debug("getCreateDate.");
        if (entry.getMetadata() != null && entry.getMetadata().getCreateDate() != null) {
            return new Date(entry.getMetadata().getCreateDate());
        }
        return null;
    }

    Path getPath() {
        return this.entry.getLDRI();
    }

    private void initMetadata() {

        Metadata meta = this.entry.getMetadata();
        Long createDate = meta.getCreateDate();
        if (createDate == null) {
            meta.setCreateDate(System.currentTimeMillis());
            entry.setMetadata(meta);
        }
        Long modifiedDate = meta.getModifiedDate();
        if (modifiedDate == null) {
            meta.setModifiedDate(System.currentTimeMillis());
            entry.setMetadata(meta);
        }
    }

    Collection<IStorageSite> getStorageSites() {
        return this.entry.getStorageSites();
    }

    private boolean canUseResource(String user) {
        return true;
    }
}
