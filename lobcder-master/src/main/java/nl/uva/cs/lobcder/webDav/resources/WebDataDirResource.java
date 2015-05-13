/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.resource.*;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.auth.Permissions;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.PDRI;
import nl.uva.cs.lobcder.rest.wrappers.Stats;
import nl.uva.cs.lobcder.util.Constants;
import org.apache.commons.io.FilenameUtils;
import org.rendersnake.HtmlCanvas;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.rendersnake.HtmlAttributesFactory.*;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class WebDataDirResource extends WebDataResource implements FolderResource,
        CollectionResource, DeletableCollectionResource, LockingCollectionResource, PostableResource {

    private int attempts = 0;
    private Map<String, String> mimeTypeMap = new HashMap<>();

    public WebDataDirResource(@Nonnull LogicalData logicalData, Path path, @Nonnull JDBCatalogue catalogue, @Nonnull List<AuthI> authList) {
        super(logicalData, path, catalogue, authList);
//        WebDataDirResource.log.log(Level.FINE, "Init. WebDataDirResource:  {0}", getPath());
        mimeTypeMap.put("mp4", "video/mp4");
        mimeTypeMap.put("pdf", "application/pdf");
        mimeTypeMap.put("tex", "application/x-tex");
        mimeTypeMap.put("log", "text/plain");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("aux", "text/plain");
        mimeTypeMap.put("bbl", "text/plain");
        mimeTypeMap.put("blg", "text/plain");
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        if (auth == null) {
            attempts = 0;
            return false;
        }
        try {
            switch (method) {
                case MKCOL:
                    String msg = "From: " + fromAddress + " User: " + getPrincipal().getUserId() + " Method: " + method;
                    WebDataDirResource.log.log(Level.INFO, msg);
                    attempts = 0;
                    return getPrincipal().canWrite(getPermissions());
                default:
                    attempts = 0;
                    return super.authorise(request, method, auth);
            }
        } catch (Throwable th) {
            if (th instanceof java.sql.SQLException && attempts <= Constants.RECONNECT_NTRY) {
                attempts++;
                authorise(request, method, auth);

            } else {
                WebDataDirResource.log.log(Level.FINER, "Exception in authorize for a resource " + getPath(), th);
                attempts = 0;
                return false;
            }

        }
        return false;
    }

    @Override
    public CollectionResource createCollection(String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
        WebDataDirResource.log.log(Level.FINE, "createCollection {0} in {1}", new Object[]{newName, getPath()});

        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Path newCollectionPath = Path.path(getPath(), newName);

                Long newFolderEntryId = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                if (newFolderEntryId != null) {
                    throw new ConflictException(this, newName);
                } else {// collection does not exists, create a new one
                    LogicalData newFolderEntry = new LogicalData(); //newCollectionPath, Constants.LOGICAL_FOLDER,
                    newFolderEntry.setType(Constants.LOGICAL_FOLDER);
                    newFolderEntry.setParentRef(getLogicalData().getUid());
                    newFolderEntry.setName(newName);
                    newFolderEntry.setCreateDate(System.currentTimeMillis());
                    newFolderEntry.setModifiedDate(System.currentTimeMillis());
                    newFolderEntry.setLastAccessDate(System.currentTimeMillis());
                    newFolderEntry.setTtlSec(getLogicalData().getTtlSec());
                    newFolderEntry.setOwner(getPrincipal().getUserId());
                    getCatalogue().setPermissions(
                            getCatalogue().registerDirLogicalData(newFolderEntry, connection).getUid(),
                            new Permissions(getPrincipal(), getPermissions()), connection);
                    newFolderEntry = inheritProperties(newFolderEntry, connection);
                    connection.commit();
                    connection.close();
                    WebDataDirResource res = new WebDataDirResource(newFolderEntry, newCollectionPath, getCatalogue(), authList);
                    return res;
                }
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public Resource child(String childName) throws NotAuthorizedException {
        WebDataDirResource.log.log(Level.FINE, "child({0}) for {1}", new Object[]{childName, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                LogicalData childLD = getCatalogue().getLogicalDataByParentRefAndName(getLogicalData().getUid(), childName, connection);
                connection.commit();
                connection.close();
                if (childLD != null) {
                    if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                        return new WebDataDirResource(childLD, Path.path(getPath(), childName), getCatalogue(), authList);
                    } else {
                        return new WebDataFileResource(childLD, Path.path(getPath(), childName), getCatalogue(), authList);
                    }
                } else {
                    return null;
                }
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                return null;
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            return null;
        }
    }

    @Override
    public List<? extends Resource> getChildren() throws NotAuthorizedException {
//        WebDataDirResource.log.log(Level.FINE, "getChildren() for {0}", getPath());
        try {
            try (Connection connection = getCatalogue().getConnection()) {
                try {
                    List<Resource> children = new ArrayList<>();
                    Collection<LogicalData> childrenLD = getCatalogue().getChildrenByParentRef(getLogicalData().getUid(), connection);
                    if (childrenLD != null) {
                        for (LogicalData childLD : childrenLD) {
                            if (childLD.getType().equals(Constants.LOGICAL_FOLDER)) {
                                children.add(new WebDataDirResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
                            } else {
                                children.add(new WebDataFileResource(childLD, Path.path(getPath(), childLD.getName()), getCatalogue(), authList));
                            }
                        }
                    }
                    getCatalogue().addViewForRes(getLogicalData().getUid(), connection);
                    connection.commit();
                    connection.close();
                    return children;
                } catch (Exception e) {
                    if (connection != null && !connection.isClosed()) {
                        connection.rollback();
                        connection.close();
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            WebDataDirResource.log.log(Level.SEVERE, null, e);
            return null;
        }
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException,
            ConflictException, NotAuthorizedException, BadRequestException, InternalError {
        WebDataDirResource.log.log(Level.FINE, "createNew. for {0}\n\t newName:\t{1}\n\t length:\t{2}\n\t contentType:\t{3}", new Object[]{getPath(), newName, length, contentType});
        LogicalData fileLogicalData;
//        List<PDRIDescr> pdriDescrList;
        WebDataFileResource resource;
        PDRI pdri;
        double start = System.currentTimeMillis();
        try (Connection connection = getCatalogue().getConnection()) {
            try {
//                Long uid = getCatalogue().getLogicalDataUidByParentRefAndName(getLogicalData().getUid(), newName, connection);
                Path newPath = Path.path(getPath(), newName);
                fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
                if (contentType == null || contentType.equals("application/octet-stream")) {
                    contentType = mimeTypeMap.get(FilenameUtils.getExtension(newName));
                }
                if (fileLogicalData != null) {  // Resource exists, update
//                    throw new ConflictException(this, newName);
                    Permissions p = getCatalogue().getPermissions(fileLogicalData.getUid(), fileLogicalData.getOwner(), connection);
                    if (!getPrincipal().canWrite(p)) {
                        throw new NotAuthorizedException(this);
                    }
                    fileLogicalData.setLength(length);
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.setLastAccessDate(fileLogicalData.getModifiedDate());
                    if (contentType == null) {
                        contentType = mimeTypeMap.get(FilenameUtils.getExtension(newName));
                    }
                    fileLogicalData.addContentType(contentType);
                    //Create new
                    pdri = createPDRI(fileLogicalData.getLength(), newName, connection);
                    pdri.setLength(length);
                    pdri.putData(inputStream);
                    fileLogicalData = getCatalogue().updateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    //fileLogicalData = inheritProperties(fileLogicalData, connection);
                    connection.commit();
                    connection.close();
//                    String md5 = pdri.getStringChecksum();
//                    if (md5 != null) {
//                        fileLogicalData.setChecksum(md5);
//                    }
                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
//                    return new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
                } else { // Resource does not exists, create a new one
                    // new need write prmissions for current collection
                    fileLogicalData = new LogicalData();
                    fileLogicalData.setName(newName);
                    fileLogicalData.setParentRef(getLogicalData().getUid());
                    fileLogicalData.setType(Constants.LOGICAL_FILE);
                    fileLogicalData.setOwner(getPrincipal().getUserId());
                    fileLogicalData.setLength(length);
                    fileLogicalData.setCreateDate(System.currentTimeMillis());
                    fileLogicalData.setModifiedDate(System.currentTimeMillis());
                    fileLogicalData.setLastAccessDate(System.currentTimeMillis());
                    fileLogicalData.setTtlSec(getLogicalData().getTtlSec());
                    fileLogicalData.addContentType(contentType);

                    pdri = createPDRI(length, newName, connection);
                    pdri.setLength(length);
                    pdri.putData(inputStream);
//                    String md5 = pdri.getStringChecksum();
//                    if (md5 != null) {
//                        fileLogicalData.setChecksum(md5);
//                    }
                    fileLogicalData = getCatalogue().associateLogicalDataAndPdri(fileLogicalData, pdri, connection);
                    getCatalogue().setPermissions(fileLogicalData.getUid(), new Permissions(getPrincipal(), getPermissions()), connection);
                    //fileLogicalData = inheritProperties(fileLogicalData, connection);
                    setPreferencesOn(fileLogicalData.getUid(), getLogicalData().getUid(), connection);
                    List<String> pref = getLogicalData().getDataLocationPreferences();
                    fileLogicalData.setDataLocationPreferences(pref);
                    connection.commit();
                    connection.close();
                    resource = new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
//                    return new WebDataFileResource(fileLogicalData, Path.path(getPath(), newName), getCatalogue(), authList);
                }
            } catch (NoSuchAlgorithmException ex) {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                WebDataDirResource.log.log(Level.SEVERE, null, ex);
                throw new InternalError(ex.getMessage());
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException | IOException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
//            throw new InternalError(e1.getMessage());
        }
        double elapsed = System.currentTimeMillis() - start;
        double speed = ((resource.getLogicalData().getLength() * 8.0) * 1000.0) / (elapsed * 1000.0);
        String msg = null;
        try {

            Stats stats = new Stats();
            stats.setSource(fromAddress);
            stats.setDestination(pdri.getHost());
            stats.setSpeed(speed);
            stats.setSize(resource.getLogicalData().getLength());
            if (!pdri.isCahce()) {
                getCatalogue().setSpeed(stats);
            }
            msg = "Source: " + fromAddress + " Destination: " + new URI(pdri.getURI()).getScheme() + "://" + pdri.getHost() + " Rx_Speed: " + speed + " Kbites/sec Rx_Size: " + (resource.getLogicalData().getLength()) + " bytes Elapsed_Time: " + elapsed + " ms";
        } catch (URISyntaxException | SQLException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        WebDataDirResource.log.log(Level.INFO, msg);
        return resource;
    }

    @Override
    public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.log(Level.FINE, "copyTo({0}, ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions newParentPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                if (!getPrincipal().canWrite(newParentPerm)) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().copyFolder(getLogicalData(), toWDDR.getLogicalData(), name, getPrincipal(), connection);
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
//        WebDataDirResource.log.log(Level.FINE, "delete() for {0}", getPath());
        if (getPath().isRoot()) {
            throw new ConflictException(this, "Cannot delete root");
        }
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                getCatalogue().remove(getLogicalData(), getPrincipal(), connection);
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        WebDataDirResource.log.log(Level.FINE, "sendContent({0}) for {1}", new Object[]{contentType, getPath()});

        try (Connection connection = getCatalogue().getConnection()) {
            try (PrintStream ps = new PrintStream(out)) {
                HtmlCanvas html = new HtmlCanvas();
                html
                        //                    .a(href("otherpage.html")).content("Other Page")
                        .table(border("1"))
                        .tr()
                        .th().content("Name")
                        .th().content("Size")
                        .th().content("Modification Date")
                        .th().content("Creation Date")
                        .th().content("Owner")
                        .th().content("Content Type")
                        .th().content("Type")
                        .th().content("Is Supervised")
                        .th().content("Uid");
                String ref;

                for (LogicalData ld : getCatalogue().getChildrenByParentRef(getLogicalData().getUid(), connection)) {
                    if (ld.isFolder()) {
                        ref = "../dav" + getPath() + "/" + ld.getName();
//                        if (ld.getUid() != 1) {
//                        } else {
//                        }
                    } else {
                        ref = "../dav" + getPath() + "/" + ld.getName();
                    }
                    html._tr()
                            .tr()
                            .td()
                            .a(href(ref))
                            .img(src("").alt(ld.getName()))
                            ._a()
                            ._td()
                            .td().content(String.valueOf(ld.getLength()))
                            .td().content(new Date(ld.getModifiedDate()).toString())
                            .td().content(new Date(ld.getCreateDate()).toString())
                            .td().content(ld.getOwner())
                            .td().content(ld.getContentTypesAsString())
                            .td().content(ld.getType())
                            .td().content(ld.getSupervised().toString())
                            .td().content(ld.getUid().toString());
                }
                html._tr()
                        ._table();
                ps.println(html.toHtml());
                getCatalogue().addViewForRes(getLogicalData().getUid(), connection);
                connection.commit();
                connection.close();
            } catch (Exception e) {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw e;
            }
        } catch (Exception e) {
            WebDataDirResource.log.log(Level.SEVERE, null, e);
            throw new BadRequestException(this);
        }
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
//        WebDataDirResource.log.log(Level.FINE, "getMaxAgeSeconds() for {0}", getPath());
        return null;
    }

    @Override
    public String getContentType(String accepts) {
//        WebDataDirResource.log.log(Level.FINE, "getContentType({0}) for {1}", new Object[]{accepts, getPath()});
        return "text/html";
    }

    @Override
    public Long getContentLength() {
//        WebDataDirResource.log.log(Level.FINE, "getContentLength() for {0}", getPath());
        return null;
    }

    @Override
    public void moveTo(CollectionResource toCollection, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
        WebDataDirResource toWDDR = (WebDataDirResource) toCollection;
        WebDataDirResource.log.log(Level.FINE, "moveTo({0}, ''{1}'') for {2}", new Object[]{toWDDR.getPath(), name, getPath()});
        try (Connection connection = getCatalogue().getConnection()) {
            try {
                Permissions destPerm = getCatalogue().getPermissions(toWDDR.getLogicalData().getUid(), toWDDR.getLogicalData().getOwner(), connection);
                LogicalData parentLD = getCatalogue().getLogicalDataByUid(getLogicalData().getParentRef());
                Permissions parentPerm = getCatalogue().getPermissions(parentLD.getUid(), parentLD.getOwner());
                if (!(getPrincipal().canWrite(destPerm) && getPrincipal().canWrite(parentPerm))) {
                    throw new NotAuthorizedException(this);
                }
                getCatalogue().moveEntry(getLogicalData(), toWDDR.getLogicalData(), name, connection);
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                WebDataDirResource.log.log(Level.SEVERE, null, e);
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                    connection.close();
                }
                throw new BadRequestException(this, e.getMessage());
            }
        } catch (SQLException e1) {
            WebDataDirResource.log.log(Level.SEVERE, null, e1);
            throw new BadRequestException(this, e1.getMessage());
        }
    }

    @Override
    public Date getCreateDate() {
        Date date = new Date(getLogicalData().getCreateDate());
//        WebDataDirResource.log.log(Level.FINE, "getCreateDate() for {0} date: " + date, getPath());
        return new Date(getLogicalData().getCreateDate());
    }

    @Override
    public boolean isLockedOutRecursive(Request rqst) {
        return false;
    }

    /**
     * This means to just lock the name Not to create the resource.
     *
     * @param name
     * @param timeout
     * @param lockInfo
     * @return
     * @throws NotAuthorizedException
     */
    @Override
    public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException {
        try (Connection connection = getCatalogue().getConnection()) {
            Path newPath = Path.path(getPath(), name);
            //If the resource exists 
            LogicalData fileLogicalData = getCatalogue().getLogicalDataByPath(newPath, connection);
            if (fileLogicalData != null) {
                throw new PreConditionFailedException(new WebDataFileResource(fileLogicalData, Path.path(getPath(), name), getCatalogue(), authList));
            }
            LockToken lockToken = new LockToken(UUID.randomUUID().toString(), lockInfo, timeout);
            return lockToken;

        } catch (SQLException | PreConditionFailedException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
            if (ex instanceof PreConditionFailedException) {
                throw new RuntimeException(ex);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataDirResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException,
            ConflictException {
        //        Set<String> keys = parameters.keySet();
        //        for (String s : keys) {
        //            WebDataDirResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, parameters.get(s)});
        //        }
        Set<String> keys = files.keySet();
        for (String s : keys) {
            WebDataDirResource.log.log(Level.INFO, "{0} : {1}", new Object[]{s, files.get(s).getFieldName()});
            try {
                //         public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException,
                createNew(files.get(s).getFieldName(), files.get(s).getInputStream(), files.get(s).getSize(), files.get(s).getContentType());
            } catch (IOException ex) {
                throw new BadRequestException(this, ex.getMessage());
            }

        }
        return null;
    }

    private LogicalData inheritProperties(LogicalData newLogicalData, Connection connection) throws SQLException {
        String value = (String) getProperty(Constants.DATA_LOC_PREF_NAME);
        if (value != null) {
            List<String> list = property2List(value);
            getCatalogue().setLocationPreferences(connection, newLogicalData.getUid(), list, getPrincipal().isAdmin());
            list = property2List(getDataLocationPreferencesString());
            newLogicalData.setDataLocationPreferences(list);
        } else {
            //trigger 
        }
        return newLogicalData;
    }

    private void setPreferencesOn(Long uidTo, Long uidFrom, Connection connection) throws SQLException {
        try (PreparedStatement psDel = connection.prepareStatement("DELETE FROM pref_table WHERE ld_uid = ?");
                PreparedStatement psIns = connection.prepareStatement("INSERT "
                + "INTO pref_table (ld_uid, storageSiteRef) "
                + "SELECT ?, storageSiteRef FROM pref_table WHERE ld_uid=?")) {
            psDel.setLong(1, uidTo);
            psIns.setLong(1, uidTo);
            psIns.setLong(2, uidFrom);
            psDel.executeUpdate();
            psIns.executeUpdate();
        }
    }

    private Connection checkConnection(Connection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
