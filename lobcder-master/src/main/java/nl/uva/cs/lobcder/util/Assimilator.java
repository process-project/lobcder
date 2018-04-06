/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

//import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import io.milton.common.Path;
import nl.uva.cs.lobcder.resources.Credential;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.VRS;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.vlet.data.VAttribute;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrs.VComposite;
import nl.uva.vlet.vrs.VNode;
import nl.uva.vlet.vrs.VRSClient;

/**
 *
 * @author S. Koulouzis
 */
public class Assimilator {

    static final String dbName = "lobcderDB2";
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/" + dbName;
    //  Database credentials
    static final String USER = "lobcder";
    static final String PASS = "RoomC3156";
    private final Connection conn;
    private static String importingOwner;

    public Assimilator() throws ClassNotFoundException, SQLException {
        //STEP 2: Register JDBC driver
        Class.forName("com.mysql.jdbc.Driver");

        //STEP 3: Open a connection
        System.out.println("Connecting to database...");
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.setAutoCommit(false);
    }

    private long addCredentials(Connection connection, String username, String password) throws SQLException {
        long id;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                + "credential_table (username, password) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            id = rs.getLong(1);
            System.out.println("ID: " + id);
            connection.commit();
        }
//        }
        return id;
    }

    private Connection getConnection() throws SQLException {
        return conn;
    }

    private long addStorageSite(Connection connection, StorageSite site, long credentialRef, boolean isCache) throws SQLException {
        long ssID;
        String resourceUri;
        if (!site.getResourceURI().endsWith("/")) {
            resourceUri = site.getResourceURI() + "/";
        } else {
            resourceUri = site.getResourceURI();
        }
//        try (Connection connection = getConnection()) {

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO "
                + "storage_site_table (resourceUri, credentialRef, currentNum, "
                + "currentSize, quotaNum, quotaSize, isCache, extra, encrypt, private, removing, readOnly) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, resourceUri);
            ps.setLong(2, credentialRef);
            ps.setLong(3, site.getCurrentNum());
            ps.setLong(4, site.getCurrentSize());
            ps.setLong(5, site.getQuotaNum());
            ps.setLong(6, site.getQuotaSize());
            ps.setBoolean(7, isCache);
            ps.setString(8, "");
            ps.setBoolean(9, site.isEncrypt());
            ps.setBoolean(10, site.isPrivateSite());
            ps.setBoolean(11, site.isRemoving());
            ps.setBoolean(12, site.isReadOnly());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            ssID = rs.getLong(1);

            connection.commit();
        }
        return ssID;
    }

    private long addPdrigroupTable(Connection connection) throws SQLException {
        long pdriGroupID;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO pdrigroup_table (refCount) VALUES(1)", Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            pdriGroupID = rs.getLong(1);
//                connection.commit();
            System.out.println("pdriGroupID: " + pdriGroupID);
        }
//        }
        return pdriGroupID;
    }

    private long addPDRI(Connection connection, String fileName, long storageSiteRef, long pdriGroupRef, boolean isEncrypted, BigInteger encryptionKey) throws SQLException {
        long pdriID;
//        try (Connection connection = getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO pdri_table "
                + "(fileName, storageSiteRef, pdriGroupRef, isEncrypted, encryptionKey) "
                + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fileName);
            ps.setLong(2, storageSiteRef);
            ps.setLong(3, pdriGroupRef);
            ps.setBoolean(4, isEncrypted);
            ps.setLong(5, encryptionKey.longValue());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            pdriID = rs.getLong(1);
//                connection.commit();
            System.out.println("pdriID: " + pdriID);
        }
//        }
        return pdriID;
    }

    private LogicalData addLogicalData(Connection connection, LogicalData entry) throws SQLException {

//        try (Connection connection = getConnection()) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO ldata_table(parentRef, ownerId, datatype, "
                + "createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, "
                + "checksum, lastValidationDate, lockTokenId, "
                + "lockScope, lockType, lockedByUser, lockDepth, "
                + "lockTimeout, description, locationPreference, "
                + "ldName) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, entry.getType());
            preparedStatement.setDate(4, new java.sql.Date(entry.getCreateDate()));
            preparedStatement.setDate(5, new java.sql.Date(entry.getModifiedDate()));
            preparedStatement.setLong(6, entry.getLength());
            preparedStatement.setString(7, entry.getContentTypesAsString());
            preparedStatement.setLong(8, entry.getPdriGroupId());
            preparedStatement.setBoolean(9, entry.getSupervised());
            preparedStatement.setString(10, entry.getChecksum());
            preparedStatement.setLong(11, entry.getLastValidationDate());
            preparedStatement.setString(12, entry.getLockTokenID());
            preparedStatement.setString(13, entry.getLockScope());
            preparedStatement.setString(14, entry.getLockType());
            preparedStatement.setString(15, entry.getLockedByUser());
            preparedStatement.setString(16, entry.getLockDepth());
            preparedStatement.setLong(17, entry.getLockTimeout());
            preparedStatement.setString(18, entry.getDescription());
//            preparedStatement.setString(19, entry.getDataLocationPreference());
            preparedStatement.setString(20, entry.getName());
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
            return entry;
        } catch (SQLException ex) {
            if (ex.getMessage().contains("Duplicate entry")) {
                System.err.println(entry.getName() + " already exists!");
            } else {
                throw ex;
            }
        }
        return null;
//        }
    }

    private long getStorageSiteID(Connection connection, String ssURI) throws SQLException {
        long ssID = -1;
        String query = "select storageSiteId from storage_site_table where resourceUri = '"
                + ssURI + "' and isCache = false";
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                query)) {
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                ssID = rs.getLong(1);
            }
        }
        if (ssID == -1) {
            if (!ssURI.endsWith("/")) {
                ssURI += "/";
                query = "select storageSiteId from storage_site_table where resourceUri = '"
                        + ssURI + "' and isCache = false";
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        query)) {
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        ssID = rs.getLong(1);
                    }
                }
            }
        }
        return ssID;
    }

    private void assimilate(List<StorageSite> sites) throws SQLException,
            MalformedURLException, VlException, NoSuchAlgorithmException, Exception {
        Connection c = getConnection();
        StorageSiteClient ssClient;
        for (StorageSite site : sites) {
            String username = site.getCredential().getStorageSiteUsername();
            String password = site.getCredential().getStorageSitePassword();
            String ssURI = site.getResourceURI();

            long ssID = getStorageSiteID(c, ssURI);
            if (ssID == -1) {
                long credentialsID = addCredentials(c, username, password);
                ssID = addStorageSite(c, site, credentialsID, false);
            }
            URI importedURI = new URI(site.getResourceURI());
            String importedFolderName = "ImportedFrom-" + importedURI.getScheme() + "-" + importedURI.getHost();

            //Add imported folder
            LogicalData imported = getLogicalDataByPath(Path.path(importedFolderName), c);
            Long importedUid;
            if (imported == null) {
                importedUid = addRegisteredFolder(importedFolderName, c);
            } else {
                importedUid = imported.getUid();
            }

//
            ssClient = new StorageSiteClient(username, password, ssURI);


            VNode dir = ssClient.getStorageSiteClient().getNode(new VRL(ssURI));


//                VFSClient client = (VFSClient) ssClient.getStorageSiteClient();
//                VDir dir = client.openDir(new VRL(ssURI));
            //build folders first 
            nl.uva.vlet.vdriver.vrs.http.HTTPNode k = (nl.uva.vlet.vdriver.vrs.http.HTTPNode) dir;
//            k.getInputStream()
                    
            add((VComposite) dir, dir.getPath(), c, ssID, false);
            VNode[] nodes = ((VComposite) dir).getNodes();
            for (VNode n : nodes) {
                if (!n.isComposite()) {
                    VFile f = (VFile) n;
                    String fileName = n.getName();
                    VRL currentPath = new VRL(f.getPath().replaceFirst(dir.getPath(), ""));
                    LogicalData parent = getLogicalDataByPath(Path.path(currentPath.getPath()), c);
                    Long parentRef;

                    if (parent != null) {
                        parentRef = parent.getUid();
                    } else {
                        parentRef = importedUid;
                    }

                    LogicalData registered = getLogicalDataByParentRefAndName(parentRef, fileName, c);
                    System.err.println(currentPath);
                    if (registered == null) {
                        addFile(c, f, parentRef, ssID);
                    }
                }
            }



        }
        c.commit();
        c.close();
    }

    public LogicalData getLogicalDataByParentRefAndName(Long parentRef, String name, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                + "description, locationPreference "
                + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            preparedStatement.setLong(1, parentRef);
            preparedStatement.setString(2, name);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                LogicalData res = new LogicalData();
                res.setUid(rs.getLong(1));
                res.setParentRef(parentRef);
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(name);
                res.setCreateDate(rs.getTimestamp(4).getTime());
                res.setModifiedDate(rs.getTimestamp(5).getTime());
                res.setLength(rs.getLong(6));
                res.setContentTypesAsString(rs.getString(7));
                res.setPdriGroupId(rs.getLong(8));
                res.setSupervised(rs.getBoolean(9));
                res.setChecksum(rs.getString(10));
                res.setLastValidationDate(rs.getLong(11));
                res.setLockTokenID(rs.getString(12));
                res.setLockScope(rs.getString(13));
                res.setLockType(rs.getString(14));
                res.setLockedByUser(rs.getString(15));
                res.setLockDepth(rs.getString(16));
                res.setLockTimeout(rs.getLong(17));
                res.setDescription(rs.getString(18));
//                res.setDataLocationPreference(rs.getString(19));
                return res;
            } else {
                return null;
            }

        }
    }

    public LogicalData registerDirLogicalData(LogicalData entry, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO ldata_table(parentRef, ownerId, datatype, ldName, createDate, modifiedDate)"
                + " VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, entry.getParentRef());
            preparedStatement.setString(2, entry.getOwner());
            preparedStatement.setString(3, Constants.LOGICAL_FOLDER);
            preparedStatement.setString(4, entry.getName());
            preparedStatement.setDate(5, new java.sql.Date(entry.getCreateDate()));
            preparedStatement.setDate(6, new java.sql.Date(entry.getModifiedDate()));
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            entry.setUid(rs.getLong(1));
            return entry;
        }
    }

    public void add(VComposite dir, String base, Connection connection, long ssid, boolean addFiles) throws MalformedURLException, VlException, SQLException, NoSuchAlgorithmException {
        VNode[] nodes = dir.getNodes();
        for (VNode f : nodes) {
            VRL currentPath = new VRL(f.getPath().replaceFirst(base, ""));
            LogicalData register = getLogicalDataByPath(Path.path(currentPath.getPath()), connection);
            LogicalData parent = getLogicalDataByPath(Path.path(currentPath.getPath()).getParent(), connection);
            Long parentRef = new Long(1);
            if (parent == null) {
                parentRef = new Long(1);
            } else {
                parentRef = parent.getUid();
            }

            if (f.isComposite()) {
                if (register == null) {
                    LogicalData entry = new LogicalData();
                    if (f instanceof VDir) {
                        VDir d = (VDir) f;
                        entry.setCreateDate(d.getModificationTime());
                        entry.setModifiedDate(d.getModificationTime());
                    } else {
                        entry.setCreateDate(System.currentTimeMillis());
                        entry.setModifiedDate(System.currentTimeMillis());
                    }

                    entry.setName(f.getName());
                    entry.setOwner(importingOwner);
                    entry.setParentRef(parentRef);

                    register = registerDirLogicalData(entry, connection);
                }
                add((VComposite) f, base, connection, ssid, addFiles);
            } else if (addFiles) {
                if (register == null) {
                    System.err.println(f.getVRL());
                    addFile(connection, (VFile) f, parentRef, ssid);
                }
            }
        }
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
                if (i == (parts.length - 1)) {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(
                            "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                            + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                            + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                            + "description, locationPreference "
                            + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            LogicalData res = new LogicalData();
                            res.setUid(rs.getLong(1));
                            res.setParentRef(parent);
                            res.setOwner(rs.getString(2));
                            res.setType(rs.getString(3));
                            res.setName(p);
                            res.setCreateDate(rs.getTimestamp(4).getTime());
                            res.setModifiedDate(rs.getTimestamp(5).getTime());
                            res.setLength(rs.getLong(6));
                            res.setContentTypesAsString(rs.getString(7));
                            res.setPdriGroupId(rs.getLong(8));
                            res.setSupervised(rs.getBoolean(9));
                            res.setChecksum(rs.getString(10));
                            res.setLastValidationDate(rs.getLong(11));
                            res.setLockTokenID(rs.getString(12));
                            res.setLockScope(rs.getString(13));
                            res.setLockType(rs.getString(14));
                            res.setLockedByUser(rs.getString(15));
                            res.setLockDepth(rs.getString(16));
                            res.setLockTimeout(rs.getLong(17));
                            res.setDescription(rs.getString(18));
//                            res.setDataLocationPreference(rs.getString(19));
                            return res;
                        } else {
                            return null;
                        }
                    }
                } else {
                    preparedStatement.setLong(1, parent);
                    preparedStatement.setString(2, p);
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        parent = rs.getLong(1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    public static void main(String args[]) throws SQLException, MalformedURLException, VlException, NoSuchAlgorithmException, Exception {
        try {
            importingOwner = "admin";
            List<StorageSite> sites = new ArrayList<>();

            Credential credential = new Credential();
            credential.setStorageSiteUsername("fake");
            credential.setStorageSitePassword("fake");

//            StorageSite ss1 = new StorageSite();
//            ss1.setCredential(credential);
//            ss1.setResourceURI("swift://somewhere:8443/auth/v1.0/uploadContainer/");
//            ss1.setCurrentNum(Long.valueOf("-1"));
//            ss1.setCurrentSize(Long.valueOf("-1"));
//            ss1.setEncrypt(false);
//            ss1.setQuotaNum(Long.valueOf("-1"));
//            ss1.setQuotaSize(Long.valueOf("-1"));
//            sites.add(ss1);
//
//
//            StorageSite ss2 = new StorageSite();
//            ss2.setCredential(credential);
//            ss2.setResourceURI("file:///" + System.getProperty("user.home") + "/Downloads");
//            ss2.setCurrentNum(Long.valueOf("-1"));
//            ss2.setCurrentSize(Long.valueOf("-1"));
//            ss2.setEncrypt(false);
//            ss2.setQuotaNum(Long.valueOf("-1"));
//            ss2.setQuotaSize(Long.valueOf("-1"));
//            sites.add(ss2);


//            StorageSite ss3 = new StorageSite();
//            ss3.setCredential(credential);
//            ss3.setResourceURI("file:///" + System.getProperty("user.home") + "/Downloads/lobcderUsageData");
////            ss3.setResourceURI("srm://tbn18.nikhef.nl:8446/dpm/nikhef.nl/home/biomed/lobcder");
//            ss3.setCurrentNum(Long.valueOf("-1"));
//            ss3.setCurrentSize(Long.valueOf("-1"));
//            ss3.setEncrypt(false);
//            ss3.setQuotaNum(Long.valueOf("-1"));
//            ss3.setQuotaSize(Long.valueOf("-1"));
//            sites.add(ss3);


            StorageSite ss4 = new StorageSite();
            ss4.setCredential(credential);
            ss4.setResourceURI("http://data.sdss3.org/sas/dr12/apogee/spectro/redux/r5/apo1m/calibration/56357/plots/");
            ss4.setCurrentNum(Long.valueOf("-1"));
            ss4.setCurrentSize(Long.valueOf("-1"));
            ss4.setEncrypt(false);
            ss4.setQuotaNum(Long.valueOf("-1"));
            ss4.setQuotaSize(Long.valueOf("-1"));
            ss4.setPrivateSite(false);
            ss4.setReadOnly(true);

            sites.add(ss4);

            Assimilator a = new Assimilator();
            a.assimilate(sites);


        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(Assimilator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            VRS.exit();
            System.exit(0);
        }
    }

    private void addFile(Connection connection, VFile f, Long parentRef, long ssID) throws SQLException, VlException, NoSuchAlgorithmException {
        long pdriGroupID = addPdrigroupTable(connection);
        LogicalData entry = new LogicalData();
        entry.setContentTypesAsString(f.getMimeType());
        entry.setCreateDate(f.getModificationTime());
        entry.setLength(f.getLength());
        entry.setModifiedDate(f.getModificationTime());
        entry.setName(f.getName());
        entry.setOwner("admin");

        entry.setParentRef(parentRef);
        entry.setType(Constants.LOGICAL_FILE);
        entry.setPdriGroupId(pdriGroupID);
//                        if(f instanceof VChecksum){
//                            String chs = ((VChecksum) f).getChecksum(VChecksum.MD5);
//                            entry.setChecksum();
//                        }
        long pdriID = addPDRI(connection, f.getName(), ssID, pdriGroupID, false, DesEncrypter.generateKey());
        addLogicalData(connection, entry);
    }

    private Long addRegisteredFolder(String importedFolderName, Connection connection) throws SQLException {
        LogicalData entry = new LogicalData();
        entry.setCreateDate(System.currentTimeMillis());
        entry.setModifiedDate(System.currentTimeMillis());
        entry.setName(importedFolderName);
        entry.setOwner("admin");
        entry.setParentRef(new Long(1));
        LogicalData register = registerDirLogicalData(entry, connection);
        return register.getUid();
    }
}
