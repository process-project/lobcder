/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import javax.xml.namespace.QName;

/**
 *
 * @author S. Koulouzis
 */
public class Constants {

    //public static final String LOBCDER_CONF_DIR = System.getProperty("user.home") + "/.lobcder-test/";
    public static final Long CACHE_STORAGE_SITE_ID = Long.valueOf(1);
    public static final String VPH_USERNAMES = "vph.users";
    public static final String STORAGE_SITE_USERNAME = "storage.site.username";
    public static final String STORAGE_SITE_PASSWORD = "storage.site.password";
    public static final String STORAGE_SITE_GRID_PROXY = "storage.site.grid.proxy";
    public static final String STORAGE_SITE_ENDPOINT = "storage.site.endpoint";
    public static final String LOGICAL_DATA = "logical.data";
    public static final String LOGICAL_FILE = "logical.file";
    public static final String LOGICAL_FOLDER = "logical.folder";
    public static final String LOBCDER_STORAGE_PREFIX = "lobcder.storage.prefix";
    public static final String STORAGE_SITES_PROP_FILES = "lobcder.storage.site.prop.files";
    public static final QName DATA_DIST_PROP_NAME = new QName("custom:", "data-distribution");
    public static final QName DRI_SUPERVISED_PROP_NAME = new QName("custom:", "dri-supervised");
    public static final QName DRI_CHECKSUM_PROP_NAME = new QName("custom:", "dri-checksum-MD5");
    public static final QName DRI_STATUS_PROP_NANE = new QName("custom:", "dri-status");
    public static final QName DRI_LAST_VALIDATION_DATE_PROP_NAME = new QName("custom:", "dri-last-validation-date-ms");
    public static final QName DESCRIPTION_PROP_NAME = new QName("custom:", "description");
    public static final QName DAV_CURRENT_USER_PRIVILAGE_SET_PROP_NAME = new QName("current-user-privilege-set");
    public static final QName DAV_ACL_PROP_NAME = new QName("DAV:", "acl");
    public static final QName DAV_LOCK_PROP_NAME = new QName("DAV:", "lockscope");
    public static final QName DATA_LOC_PREF_NAME = new QName("custom:", "data-location-preference");
    public static final QName ENCRYPT_PROP_NAME = new QName("custom:", "encrypt");
    public static final QName AVAIL_STORAGE_SITES_PROP_NAME = new QName("custom:", "avail-storage-sites");
    public static final QName TTL = new QName("custom:", "ttl");
    public static final QName REPLICATION_QUEUE = new QName("custom:", "replication-queue");
    public static final QName REPLICATION_QUEUE_LEN = new QName("custom:", "replication-queue-len");
    public static final QName REPLICATION_QUEUE_SIZE = new QName("custom:", "replication-queue-size");
    public static final QName[] PROP_NAMES = new QName[]{DATA_DIST_PROP_NAME, DRI_SUPERVISED_PROP_NAME,
        DRI_CHECKSUM_PROP_NAME, DRI_LAST_VALIDATION_DATE_PROP_NAME, DRI_STATUS_PROP_NANE, DESCRIPTION_PROP_NAME,
        DATA_LOC_PREF_NAME, ENCRYPT_PROP_NAME, AVAIL_STORAGE_SITES_PROP_NAME, DAV_LOCK_PROP_NAME, TTL,
        REPLICATION_QUEUE, REPLICATION_QUEUE_LEN, REPLICATION_QUEUE_SIZE};
    public static final int BUF_SIZE = 2 * 1024 * 1024;
    public static Long LOCK_TIME = new Long(60000);
    public static final int RECONNECT_NTRY = 4;
    public static final String RANGE_HEADER_NAME = "range";
    public static final String CERT_LOCATION = System.getProperty("user.home") + "/.globus/certificates/";
    public static final String PROXY_FILE = "/tmp/myProxy";
    public static String[] BUFFERED_TYPES = new String[]{"audio/mpeg", "video/mp4"};
    public static int CACHE_SIZE = 600;
}
