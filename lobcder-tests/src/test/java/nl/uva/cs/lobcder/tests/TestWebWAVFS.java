/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import nl.uva.cs.lobcder.tests.TestREST.LogicalDataWrapped;
import nl.uva.cs.lobcder.tests.TestREST.PDRIDesc;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.data.StringUtil;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.util.cog.GridProxy;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author S. Koulouzis
 */
public class TestWebWAVFS {

    private static String root;
    private static URI uri;
    private static String username1, password1;
    private static HttpClient client1;
    private static HttpClient client2;
    private static String username2;
    private static String password2;
    private static Properties prop;
    private static Client restClient;
    private static String restURL;
    private static DefaultHttpClient httpclient;
    private static Utils utils;
    private static Boolean quckTest;

    static {
        try {
            InitGlobalVFS();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    private static void InitGlobalVFS() throws MalformedURLException, VlException, Exception {
        try {
            GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        } catch (MalformedURLException ex) {
            fail(ex.getMessage());
            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
        }
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);
        GlobalConfig.setUserHomeLocation(new URL("file:///" + System.getProperty("user.home")));

        // user configuration 
//        GlobalConfig.setUsePersistantUserConfiguration(false);
//        GlobalConfig.setUserHomeLocation(new URL("file:////" + this.tmpVPHuserHome.getAbsolutePath()));
//        Global.setDebug(true);

        VRS.getRegistry().addVRSDriverClass(nl.uva.vlet.vfs.cloud.CloudFSFactory.class);
        Global.init();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {

        String propBasePath = "etc" + File.separator + "test.properties";

        prop = TestSettings.getTestProperties(propBasePath);

        String testURL = prop.getProperty("webdav.test.url");
        //Some problem with the pom.xml. The properties are set but System.getProperty gets null
        if (testURL == null) {
            testURL = "http://localhost:8080/lobcder-1.0-SNAPSHOT/";
        }
        assertTrue(testURL != null);
        if (!testURL.endsWith("/")) {
            testURL = testURL + "/";
        }


        uri = URI.create(testURL);
        root = uri.toASCIIString();
        if (!root.endsWith("/")) {
            root += "/";
        }

        username1 = prop.getProperty(("webdav.test.username1"), "");
        if (username1 == null) {
            username1 = "user1";
        }
        assertTrue(username1 != null);
        password1 = prop.getProperty(("webdav.test.password1"), "");
        if (password1 == null) {
            password1 = "passwd1";
        }
        assertTrue(password1 != null);


        username2 = prop.getProperty(("webdav.test.username2"), "user2");
        assertTrue(username2 != null);
        password2 = prop.getProperty(("webdav.test.password2"), "passwd2");
        assertTrue(password2 != null);

        quckTest = Boolean.valueOf(prop.getProperty(("test.quick"), "true"));

        client1 = new HttpClient();
        HttpClientParams params = new HttpClientParams();
        params.setParameter("http.protocol.handle-redirects", false);
        client1.setParams(params);

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client1);


        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }

        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        client1.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username1, password1));


        httpclient = new DefaultHttpClient();
        org.apache.http.auth.Credentials defaultcreds = new org.apache.http.auth.UsernamePasswordCredentials(username1, password1);
        httpclient.getCredentialsProvider().setCredentials(org.apache.http.auth.AuthScope.ANY, (org.apache.http.auth.Credentials) defaultcreds);
//        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);


        client2 = new HttpClient();

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client2);

        client2.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username2, password2));



        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        restClient = Client.create(clientConfig);
        restClient.addFilter(new com.sun.jersey.api.client.filter.HTTPBasicAuthFilter(username1, password1));
        restURL = prop.getProperty(("rest.test.url"), "http://localhost:8080/lobcder-2.0-SNAPSHOT/rest/");


        utils = new Utils(client1);

    }

    private VFSClient getVFSClient(String vrl, String username, String password) throws VlException {
        VFSClient vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();
        //Bug in sftp: We have to put the username in the url
        ServerInfo info = context.getServerInfoFor(new VRL(vrl), true);
        String authScheme = info.getAuthScheme();

        if (StringUtil.equals(authScheme, ServerInfo.GSI_AUTH)) {
//            copyVomsAndCerts();
            GridProxy gridProxy = null;
            if (gridProxy == null) {
//                context.setProperty("grid.proxy.location", Constants.PROXY_FILE);
                // Default to $HOME/.globus
                context.setProperty("grid.certificate.location", Global.getUserHome() + "/.globus");
                String vo = username;
                context.setProperty("grid.proxy.voName", vo);
                context.setProperty("grid.proxy.lifetime", "200");
//                gridProxy = GridProxy.loadFrom(context, proxyFile);
                gridProxy = context.getGridProxy();
                if (gridProxy.isValid() == false) {
                    gridProxy.setEnableVOMS(true);
                    gridProxy.setDefaultVOName(vo);
                    gridProxy.createWithPassword(password);
                    if (gridProxy.isValid() == false) {
                        throw new VlException("Created Proxy is not Valid!");
                    }
//                    gridProxy.saveProxyTo(Constants.PROXY_FILE);
                }
            }


        }

        if (StringUtil.equals(authScheme, ServerInfo.PASSWORD_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSWORD_OR_PASSPHRASE_AUTH)
                || StringUtil.equals(authScheme, ServerInfo.PASSPHRASE_AUTH)) {
//            String username = storageSite.getCredential().getStorageSiteUsername();
            if (username == null) {
                throw new NullPointerException("Username is null!");
            }
            info.setUsername(username);
//            String password = storageSite.getCredential().getStorageSitePassword();
            if (password == null) {
                throw new NullPointerException("password is null!");
            }
            info.setPassword(password);
        }

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);

//        if(getVrl().getScheme().equals(VRS.SFTP_SCHEME)){
        //patch for bug with ssh driver 
        info.setAttribute("sshKnownHostsFile", System.getProperty("user.home") + "/.ssh/known_hosts");
//        }
//        context.setProperty("chunk.upload", doChunked);
//        info.setAttribute(new VAttribute("chunk.upload", true));
        info.store();

        return vfsClient;
    }

    @Test
    public void testCreateAndDeleteFile() throws IOException, DavException {
        System.err.println("testCreateAndDeleteFile");
        //Make sure it's deleted 
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);

        testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        String testFileURI2 = uri.toASCIIString() + TestSettings.TEST_TXT_FILE_NAME;
        put = new PutMethod(testFileURI2);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);


        utils.deleteResource(testFileURI1, true);
        utils.deleteResource(testFileURI2, true);
    }

    @Test
    public void testSetGetPropertySet() throws IOException, DavException {
        System.err.println("testSetGetPropertySet");
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client1.executeMethod(put);
        assertEquals(HttpStatus.SC_CREATED, status);

        PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
        status = client1.executeMethod(propFind);
        assertEquals(HttpStatus.SC_MULTI_STATUS, status);
        MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multiStatus.getResponses();
        assertEquals(HttpStatus.SC_OK, responses[0].getStatus()[0].getStatusCode());
        DavPropertySet allProp = utils.getProperties(responses[0]);

//        DavPropertyIterator iter = allProp.iterator();
//        while (iter.hasNext()) {
//            DavProperty<?> p = iter.nextProperty();
//            System.out.println("P: " + p.getName() + " " + p.getValue());
//        }

        String isCollStr = (String) allProp.get(DavPropertyName.ISCOLLECTION).getValue();
        Boolean isCollection = Boolean.getBoolean(isCollStr);
        assertFalse(isCollection);
        String lenStr = (String) allProp.get(DavPropertyName.GETCONTENTLENGTH).getValue();
        assertEquals(Long.valueOf(lenStr), Long.valueOf(TestSettings.TEST_DATA.length()));
        String contentType = (String) allProp.get(DavPropertyName.GETCONTENTTYPE).getValue();
        assertEquals("text/plain; charset=UTF-8", contentType);
        utils.deleteResource(testFileURI1, true);
    }

    @Test
    public void testPROPFIND_PUT_PROPFIND_GET_PUT() throws IOException, DavException {
        System.err.println("testPROPFIND_PUT_PROPFIND_GET_PUT");
        //Make sure it's deleted 
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        DeleteMethod del = new DeleteMethod(testFileURI1);
        int status = client1.executeMethod(del);
        assertTrue(status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_FOUND);

        try {
            //PROPFIND file is not there 
            testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
            PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
            status = client1.executeMethod(propFind);
            assertEquals(HttpStatus.SC_NOT_FOUND, status);

            //PUT create an empty file 
            PutMethod put = new PutMethod(testFileURI1);
            put.setRequestEntity(new StringRequestEntity("\n", "text/plain", "UTF-8"));
            status = client1.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            //PROPFIND get proerties 
            propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
            status = client1.executeMethod(propFind);
            assertEquals(HttpStatus.SC_MULTI_STATUS, status);


            MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
            MultiStatusResponse[] responses = multiStatus.getResponses();

            DavPropertySet allProp = utils.getProperties(responses[0]);
//        DavPropertyIterator iter = allProp.iterator();
//        while (iter.hasNext()) {
//            DavProperty<?> p = iter.nextProperty();
//            System.out.println("P: " + p.getName() + " " + p.getValue());
//        }

            String isCollStr = (String) allProp.get(DavPropertyName.ISCOLLECTION).getValue();
            Boolean isCollection = Boolean.getBoolean(isCollStr);
            assertFalse(isCollection);
            String lenStr = (String) allProp.get(DavPropertyName.GETCONTENTLENGTH).getValue();
            assertEquals(Long.valueOf(lenStr), Long.valueOf("\n".length()));
            String contentType = (String) allProp.get(DavPropertyName.GETCONTENTTYPE).getValue();
            assertEquals("text/plain; charset=UTF-8", contentType);


            //GET the file 
            GetMethod get = new GetMethod(testFileURI1);
            status = client1.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals("\n", get.getResponseBodyAsString());

            //PUT
            put = new PutMethod(testFileURI1);
            String content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
            put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
            status = client1.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            get = new GetMethod(testFileURI1);
            status = client1.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals(content, get.getResponseBodyAsString());

            put = new PutMethod(testFileURI1);
            content = get.getResponseBodyAsString() + TestSettings.TEST_DATA;
            put.setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
            status = client1.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            get = new GetMethod(testFileURI1);
            status = client1.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            assertEquals(content, get.getResponseBodyAsString());
        } finally {
            utils.deleteResource(testFileURI1, false);
        }
    }

    @Test
    public void testUploadFileOnRootWithoutAdminRole() throws IOException, DavException {
        System.err.println("testUploadFileOnRootWithoutAdminRole");
        String uname = prop.getProperty(("webdav.test.non.admin.username1"), "nonAdmin");
        assertNotNull(uname);
        String pass = prop.getProperty(("webdav.test.non.admin.password1"), "secret");
        assertNotNull(pass);
        HttpClient client = new HttpClient();

        assertNotNull(uri.getHost());
        assertNotNull(uri.getPort());
        assertNotNull(client);

        client.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(uname, pass));



        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        PutMethod put = new PutMethod(testFileURI1);
        put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
        int status = client.executeMethod(put);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, status);
    }

//    @Test
//    public void testUpDownloadFileWithSpace() throws IOException, DavException {
//        System.err.println("testUpDownloadFileWithSpace");
//        String testFileURI1 = uri.toASCIIString() + "file with spaces";
//        
//        utils.createFile(testFileURI1, true);
//        
//        GetMethod get = new GetMethod(testFileURI1);
//        int status = client1.executeMethod(get);
//        assertEquals(HttpStatus.SC_OK, status);
//        assertEquals(TestSettings.TEST_DATA, get.getResponseBodyAsString());
//
//        utils.deleteResource(testFileURI1, false);
//    }
    @Test
    public void testPutGet() throws VlException, IOException {
        System.err.println("testPutGet");
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        try {
            PutMethod put = new PutMethod(testFileURI1);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            int status = client1.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);


            GetMethod get = new GetMethod(testFileURI1);
            client1.executeMethod(get);
            status = get.getStatusCode();
            assertEquals(HttpStatus.SC_OK, status);
            String content = get.getResponseBodyAsString();

            assertEquals(TestSettings.TEST_DATA.length(), content.length());
            assertTrue(content.equals(TestSettings.TEST_DATA));

        } finally {
            utils.deleteResource(testFileURI1, false);
        }
    }
//

    @Test
    public void testFileConsistency() throws VlException, IOException {
        if (quckTest) {
            return;
        }
        System.err.println("testFileConsistency");
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
        try {
            int from;
            int size;
            if (this.root.contains("localhost")) {
                from = 100;
                size = 200;

            } else {
                from = 10;
                size = 20;
            }

            for (int j = from; j < size; j += 5) {

                File file = new File("/tmp/" + TestSettings.TEST_FILE_NAME1);


                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024 * 1024]; //1MB
                Random r = new Random();

                for (int i = 0; i < j; i++) {
                    r.nextBytes(buffer);
                    fos.write(buffer);
                    if (i % 100 == 0) {
                        System.err.println(i + " of " + j);
                    }
                }
                fos.flush();
                fos.close();

                utils.postFile(file, uri.toASCIIString());

                String localChecksum = utils.getChecksum(file, "SHA1");

                Thread.sleep(40000);

                GetMethod get = new GetMethod(testFileURI1);
                client1.executeMethod(get);
                int status = get.getStatusCode();
                assertEquals(HttpStatus.SC_OK, status);
                InputStream in = get.getResponseBodyAsStream();
                File fromLob = new File("/tmp/fromLob");
                fos = new FileOutputStream(fromLob);

                byte buf[] = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fos.close();
                in.close();



                String fromLobChecksum = utils.getChecksum(fromLob, "SHA1");

                assertEquals(fromLobChecksum, localChecksum);


                System.out.println(j + " of " + size);
            }

        } catch (Exception ex) {
            fail(ex.getMessage());
            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);

        } finally {
            utils.deleteResource(testFileURI1, false);
        }
    }

    @Test
    public void testInconsistency() throws VlException, IOException {
        System.err.println("testInconsistency");
        String testFileURI1 = uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        try {
            PutMethod put = new PutMethod(testFileURI1);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            int status = client1.executeMethod(put);
            assertEquals(HttpStatus.SC_CREATED, status);

            Set<PDRIDesc> pdris = null;
            //Wait for replication 
            utils.waitForReplication(testFileURI1);

            pdris = getPdris(TestSettings.TEST_FILE_NAME1 + ".txt");

            //Delete the physical data 
            deletePhysicalData(pdris);


            GetMethod get = new GetMethod(testFileURI1);
            status = client1.executeMethod(get);
            String cont = get.getResponseBodyAsString();
            //There is no offical status to get from this but  confilct seems apropriate:
            //Status code (409) indicating that the request could not be 
            //completed due to a conflict with the current state of the resource. 
            //Meaning at this time we have no physical data 
            assertEquals(HttpStatus.SC_CONFLICT, status);


        } catch (Exception ex) {
            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        } finally {
            utils.deleteResource(testFileURI1, false);
        }
    }

    @Test
    public void testCopy() throws VlException, IOException {
        System.err.println("testCopy");
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1 + ".txt";
        String testcol = root + "testResourceId/";
        try {


            PutMethod put = new PutMethod(testFileURI1);
            put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
            int status = client1.executeMethod(put);
            assertTrue(status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED);

            GetMethod get = new GetMethod(testFileURI1);
            status = client1.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            String cont = get.getResponseBodyAsString();
            assertEquals(TestSettings.TEST_DATA, cont);

            MkColMethod mkcol = new MkColMethod(testcol);
            status = client1.executeMethod(mkcol);
            assertEquals(HttpStatus.SC_CREATED, status);


            String targetURL = testcol + TestSettings.TEST_FILE_NAME1 + ".txt";
            CopyMethod cp = new CopyMethod(testFileURI1, targetURL, true);
            status = client1.executeMethod(cp);
            assertTrue(status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED);


            get = new GetMethod(targetURL);
            status = client1.executeMethod(get);
            assertEquals(HttpStatus.SC_OK, status);
            cont = get.getResponseBodyAsString();
            System.out.println(cont);
            assertEquals(TestSettings.TEST_DATA, cont);
        } finally {
            utils.deleteResource(testcol, false);
            utils.deleteResource(testFileURI1, false);
        }
    }

    @Test
    public void testUpdateFile() throws VlException, IOException, NoSuchAlgorithmException, DavException, InterruptedException {
        System.err.println("testUpdateFile");
        String testFileURI1 = this.uri.toASCIIString() + TestSettings.TEST_FILE_NAME1;
//        String testcol = root + "testResourceId/";
        try {
            utils.deleteResource(testFileURI1, false);
            File file = utils.createRandomFile("/tmp/" + TestSettings.TEST_FILE_NAME1, 1);
            utils.postFile(file, uri.toASCIIString());
            String firstLocalChecksum = utils.getChecksum(file, "SHA1");
            utils.waitForReplication(testFileURI1);
            File fromLOB = utils.DownloadFile(testFileURI1, "/tmp/FromLOB", true);
            String firstRemoteChecksum = utils.getChecksum(fromLOB, "SHA1");
            assertEquals(firstLocalChecksum, firstRemoteChecksum);

            file = utils.createRandomFile("/tmp/" + TestSettings.TEST_FILE_NAME1, 1);
            utils.postFile(file, uri.toASCIIString());
            String secondLocalChecksum = utils.getChecksum(file, "SHA1");
            utils.waitForReplication(testFileURI1);
            fromLOB = utils.DownloadFile(testFileURI1, "/tmp/FromLOB", true);
            String secondRemoteChecksum = utils.getChecksum(fromLOB, "SHA1");
            assertEquals(secondLocalChecksum, secondRemoteChecksum);

        } finally {
            utils.deleteResource(testFileURI1, false);
        }
    }

    @Test
    public void testFileNames() throws IOException, DavException, InterruptedException {
        if (quckTest) {
            return;
        }
        System.err.println("testFileNames");
        String name = "a'b";
        String testFileURI1 = this.uri.toASCIIString() + name;
//        String name2 = "~!@#$%^&*()_+' '/<>}]]{[";
//        String testFileURI2 = this.uri.toASCIIString() + name2;
        try {
            utils.deleteResource(testFileURI1, false);

            utils.createFile(testFileURI1, true);

            utils.waitForReplication(testFileURI1);
            File file = utils.DownloadFile(testFileURI1, "/tmp/" + name, true);

            assertEquals(name, file.getName());
            utils.deleteResource(testFileURI1, false);


//            utils.createFile(testFileURI2, true);
//            utils.waitForReplication(testFileURI2);
//            file = utils.DownloadFile(testFileURI2, "/tmp/" + name, true);

            assertEquals(name, file.getName());
        } finally {
            utils.deleteResource(testFileURI1, false);
//            utils.deleteResource(testFileURI2, false);
        }
    }

    @Test
    public void testMultiThread() throws IOException, DavException {
        System.err.println("testMultiThread");
        try {
            Thread userThread1 = new UserThread(client1, uri.toASCIIString(), 1);
            userThread1.setName("T1");


            client2.getState().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    new UsernamePasswordCredentials(username1, password1));

            Thread userThread2 = new UserThread(client2, uri.toASCIIString(), 2);
            userThread2.setName("T2");

            userThread1.start();
            userThread2.start();

            userThread1.join();
            userThread2.join();
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
            Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Set<PDRIDesc> getPdris(String testFileURI1) {
        System.out.println("getPdris");
        WebResource webResource = restClient.resource(restURL);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("path", testFileURI1);

        WebResource res = webResource.path("items").path("query").queryParams(params);
        List<LogicalDataWrapped> list = res.accept(MediaType.APPLICATION_XML).
                get(new GenericType<List<LogicalDataWrapped>>() {
        });


        assertNotNull(list);
        assertFalse(list.isEmpty());
        LogicalDataWrapped logicalDataWrapped = null;
        for (LogicalDataWrapped lwd : list) {
            if (lwd.logicalData.type.equals("logical.file") && lwd.path.equals(testFileURI1)) {
                logicalDataWrapped = lwd;
                break;
            }
        }

        assertNotNull(logicalDataWrapped);
        assertFalse(logicalDataWrapped.logicalData.supervised);

        return logicalDataWrapped.pdriList;

    }

    private void deletePhysicalData(Set<PDRIDesc> pdris) throws VlException {
        String endpoint = "";
        for (PDRIDesc p : pdris) {
            VFSClient cli = getVFSClient(p.resourceUrl, p.username, p.password);
            if (p.resourceUrl.startsWith("/")) {
                endpoint = "file:///" + p.resourceUrl;
            } else {
                endpoint = p.resourceUrl;
            }
            VRL vrl = new VRL(endpoint).append("LOBCDER-REPLICA-vTEST").append(p.name);
//            VRL vrl = new VRL(endpoint).append(p.name);
            System.err.println("Deleting: " + vrl);
            cli.openLocation(vrl).delete();
        }
    }

    private static class UserThread extends Thread {

        private final HttpClient client;
        private final String serverLOC;
        private final int num;

        private UserThread(HttpClient client, String serverLOC, int num) {
            this.client = client;
            this.serverLOC = serverLOC;
            this.num = num;
        }

        @Override
        public void run() {
            String testFileURI1 = serverLOC + "testFileName" + getName();
            try {
                PutMethod put = new PutMethod(testFileURI1);
                put.setRequestEntity(new StringRequestEntity(TestSettings.TEST_DATA, "text/plain", "UTF-8"));
                int status = client.executeMethod(put);
                assertEquals("Error wile executing PUT for " + testFileURI1, HttpStatus.SC_CREATED, status);


                PropFindMethod propFind = new PropFindMethod(testFileURI1, DavConstants.PROPFIND_ALL_PROP_INCLUDE, DavConstants.DEPTH_0);
                status = client.executeMethod(propFind);
                assertEquals(HttpStatus.SC_MULTI_STATUS, status);
                MultiStatus multiStatus = propFind.getResponseBodyAsMultiStatus();
                MultiStatusResponse[] responses = multiStatus.getResponses();
                assertEquals(HttpStatus.SC_OK, responses[0].getStatus()[0].getStatusCode());

                DeleteMethod del = new DeleteMethod(testFileURI1);
                status = client.executeMethod(del);
                assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);

            } catch (DavException ex) {
                fail(ex.getMessage());
                Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                fail(ex.getMessage());
                Logger.getLogger(TestWebWAVFS.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
