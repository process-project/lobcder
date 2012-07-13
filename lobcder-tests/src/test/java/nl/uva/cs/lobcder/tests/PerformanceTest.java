/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.tests;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nl.uva.vlet.Global;
import nl.uva.vlet.GlobalConfig;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VDir;
import nl.uva.vlet.vfs.VFSClient;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;
import nl.uva.vlet.vrl.VRL;
import nl.uva.vlet.vrs.ServerInfo;
import nl.uva.vlet.vrs.VRS;
import nl.uva.vlet.vrs.VRSContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author S. koulouzis
 */
public class PerformanceTest {

    private static VDir testDriverRemoteDir;
    private static VDir localTempDir;
    private static URI uri;
    private static String lobcderRoot;
    private static String[] usernames;
    private static String password;
    private static HttpClient client;
    private static String lobcdrTestPath;
    private static VFSClient vfsClient;
    private static File dataSetFolderBase;
    private static ArrayList<File> datasets;
    
    
    public static final int CREATE_DATASET = 1;
    public static final int FILE_SIZE_IN_KB = 50;
    public static final int STEP_SIZE_DATASET = 4;
    
    
    
    public static final int MIN_SIZE_DATASET = 10;//640;
    public static final int MAX_SIZE_DATASET = 1200;

    @BeforeClass
    public static void setUpClass() throws Exception {
        String propBasePath = System.getProperty("user.home") + File.separator
                + "workspace" + File.separator + "lobcder-tests"
                + File.separator + "etc" + File.separator + "test.proprties";
        Properties prop = TestSettings.getTestProperties(propBasePath);


        initBackendDriver(prop);

        initURL(prop);

        initUsers(prop);

        initClients();

        initLobcderTestDir();

        initDatasets();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        testDriverRemoteDir.delete();

        DeleteMethod del = new DeleteMethod(lobcdrTestPath);
//        debug("Deleteing: "+lobcdrTestPath);
        int status = client.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);


        del = new DeleteMethod(lobcderRoot);
//        debug("Deleteing: "+lobcderRoot);
        status = client.executeMethod(del);
        assertTrue("status: " + status, status == HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT);
    }

    private static void initUsers(Properties prop) {
        usernames = prop.getProperty("lobcder.scale.usernames").split(",");
        password = prop.getProperty("lobcder.scale.password");
    }

    private static void initClients() {
        ProtocolSocketFactory socketFactory =
                new EasySSLProtocolSocketFactory();

        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }
        Protocol https = new Protocol("https", socketFactory, port);
        Protocol.registerProtocol("https", https);

        client = new HttpClient();

        client.getState().setCredentials(
                new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(usernames[0], password));


    }

    private static void initURL(Properties prop) throws FileNotFoundException {
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
        lobcderRoot = uri.toASCIIString();
        if (!lobcderRoot.endsWith("/")) {
            lobcderRoot += "/";
        }

        lobcdrTestPath = uri.toString() + "/deleteMe";
    }

    private static void initDatasets() throws IOException, InterruptedException {
        //Init a synthetic datasets from 50MB to 4GB
        String dirPath = System.getProperty("java.io.tmpdir") + "/testDatasets";
        dataSetFolderBase = new File(dirPath);
        if (!dataSetFolderBase.exists()) {
            if (!dataSetFolderBase.mkdirs()) {
                throw new IOException("Faild to create tmp dir");
            }
        }

        datasets = new ArrayList<File>();
        createFiles(32, MIN_SIZE_DATASET, MAX_SIZE_DATASET, dataSetFolderBase);
    }

    private static void createFiles(int threads, int start, int end, File dataSetFolderBase) throws IOException, InterruptedException {
        ExecutorService execSvc = Executors.newFixedThreadPool(threads);
        long startTime = System.currentTimeMillis();
        for (int i = start; i < end; i *= STEP_SIZE_DATASET) {
            String datasetName = String.valueOf(i);
            String datasetPath = dataSetFolderBase.getAbsolutePath() + "/" + datasetName;
            File dataset = new File(datasetPath);
            if (!dataset.exists()) {
                if (!dataset.mkdirs()) {
                    throw new IOException("Faild to create tmp dir");
                }
            }
            datasets.add(dataset);
            for (int j = 0; j < i; j++) {
                ScaleTest u = new ScaleTest(CREATE_DATASET);
                u.setDataset(dataset);
                u.setFileID(j);
                execSvc.execute(u);
            }
        }
        execSvc.shutdown();
        execSvc.awaitTermination(30, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();
    }

    @Test
    public void benchmarkTest() throws FileNotFoundException, IOException, InterruptedException {
        benchmarkUpload();
    }

    private void benchmarkUpload() throws IOException {

        //Create the folder stucture 
        File dataset = datasets.get(0);
        String[] parts = dataset.getAbsolutePath().split("/");
        String testDatasetPath = lobcderRoot;
        for (int i = 1; i < parts.length - 1; i++) {
            if (testDatasetPath.endsWith("/")) {
                testDatasetPath += parts[i];
            } else {
                testDatasetPath += "/" + parts[i];
            }
//                System.out.println("MkCol: " + testDatasetPath);
            MkColMethod mkcol = new MkColMethod(testDatasetPath);
            client.executeMethod(mkcol);
            assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
        }


        for (File d : datasets) {
            File[] files = d.listFiles();
            String datasetName = files[0].getParentFile().getName();
            String path1 = testDatasetPath + "/" + datasetName;
            debug("Mkol: " + path1);
            MkColMethod mkcol = new MkColMethod(path1);
            client.executeMethod(mkcol);
            
            for (File f : files) {
                String path2 = path1 + "/" + f.getName();
                PutMethod put = new PutMethod(path2);
                RequestEntity requestEntity = new InputStreamRequestEntity(
                        new FileInputStream(f));
                put.setRequestEntity(requestEntity);
                double putStart = System.currentTimeMillis();
                client.executeMethod(put);
                double putEnd = System.currentTimeMillis();
                double individualFileUploadSpeedkBpSec = (f.length() / 1024.0) / ((putEnd - putStart) / 1000.0);
                debug("uploadSpeed: " +individualFileUploadSpeedkBpSec);
            }
        }
    }

//    @Test
//    public void testSpeed() throws VlException, IOException {
//
//        //First test backend speed
//        VFile[] localFiles = new VFile[5];
//
//        VFile localFile = localTempDir.createFile("test1MBUpload");
//        int len = 1024 * 100;
//        Random generator = new Random();
//        byte buffer[] = new byte[len];
//        generator.nextBytes(buffer);
//        localFile.streamWrite(buffer, 0, buffer.length);
//
//        long start_time;
//        long total_millis;
//        
//        double sum = 0;
//        double mean;
//        double N = 3;
//        VFile remoteFile = null;
//        
//        debug("---------- Test upload backend same file ------------");
//        for (int i = 0; i < N; i++) {
//            start_time = System.currentTimeMillis();
//            remoteFile = localFile.copyTo(testDriverRemoteDir);
//            total_millis = System.currentTimeMillis() - start_time;
//            assertTrue(remoteFile.exists());
//            double backendUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("backend upload speed=" + backendUpSpeed
//                    + "KB/s");
//            sum += backendUpSpeed;
//        }
//        mean = sum / N;
//        debug("mean backend upload speed=" + mean
//                + "KB/s");
//
//        
//        byte[] buf = new byte[(int) localFile.getLength()];
//        sum = 0;
//        debug("---------- Test download backend same file ------------");
//        for (int i = 0; i < N; i++) {
//            start_time = System.currentTimeMillis();
//            InputStream is = remoteFile.getInputStream();
//            File downLoadedFile = new File(localTempDir.getPath() + "/downloadFile");
//            FileOutputStream os = new FileOutputStream(downLoadedFile);
//            int read;
//            while ((read = is.read(buf)) != -1) {
//                os.write(buf, 0, read);
//            }
//            total_millis = System.currentTimeMillis() - start_time;
//            assertTrue(localFile.exists());
//            double backendDownSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("backend download speed=" + backendDownSpeed
//                    + "KB/s");
//            sum += backendDownSpeed;
//        }
//        mean = sum / N;
//        debug("mean backend download speed=" + mean
//                + "KB/s");
//
//        //Now test with LOBCDER
//        String lobcderFilePath = lobcdrTestPath + localFile.getName();
//        PutMethod put = new PutMethod(lobcderFilePath);
//        RequestEntity requestEntity = new InputStreamRequestEntity(localFile.getInputStream());
//        put.setRequestEntity(requestEntity);
//
//        sum = 0;
//        start_time = 0;
//        double lobcderUpSpeed;
//        debug("---------- Test upload lobcder same file ------------");
//        for (int i = 0; i < N; i++) {
//            start_time = System.currentTimeMillis();
//            int status = client.executeMethod(put);
//            total_millis = System.currentTimeMillis() - start_time;
//            assertEquals(HttpStatus.SC_CREATED, status);
//            lobcderUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("lobcder upload speed=" + lobcderUpSpeed
//                    + "KB/s");
//            sum += lobcderUpSpeed;
//        }
//        mean = sum / N;
//
//        debug("mean lobcder upload speed=" + mean
//                + "KB/s");
//
//        sum = 0;
//        start_time = 0;
//        GetMethod get = new GetMethod(lobcderFilePath);
//        double lobcderDownSpeed;
//        debug("---------- Test download lobcder same file ------------");
//        for (int i = 0; i < N; i++) {
//            start_time = System.currentTimeMillis();
//            int status = client.executeMethod(get);
//            InputStream is = get.getResponseBodyAsStream();
//            File downLoadedFile = new File(localTempDir.getPath() + "/downloadFile");
//            FileOutputStream os = new FileOutputStream(downLoadedFile);
//            int read;
//            while ((read = is.read(buf)) != -1) {
//                os.write(buf, 0, read);
//            }
//
//            total_millis = System.currentTimeMillis() - start_time;
//            assertEquals(HttpStatus.SC_OK, status);
//            lobcderDownSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("lobcder download speed=" + lobcderDownSpeed
//                    + "KB/s");
//            sum += lobcderDownSpeed;
//        }
//        mean = sum / N;
//
//        debug("mean lobcder download speed=" + mean
//                + "KB/s");
//    }
//
//    @Test
//    public void testSpeedFromFolder() throws VlException, FileNotFoundException, IOException {
//        String datasetPath = "/tmp/testDatasets/10";
//        VDir localDatasetDir = vfsClient.openDir(new VRL("file:" + datasetPath));
//        VFSNode[] nodes = localDatasetDir.list();
//        double sum = 0;
//        
//        debug("---------- Test upload backend dataset------------");
//        for (VFSNode n : nodes) {
//            long start_time = System.currentTimeMillis();
//            VFSNode remoteFile = n.copyTo(testDriverRemoteDir);
//            long total_millis = System.currentTimeMillis() - start_time;
//            double len = ((VFile) remoteFile).getLength();
//            double backendUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("backend upload speed=" + backendUpSpeed
//                    + "KB/s");
//            sum += backendUpSpeed;
//        }
//        double mean = sum / nodes.length;
//        debug("mean backend download speed=" + mean
//                + "KB/s");
//
//
//
//        File datasetDir = new File(datasetPath);
//        File[] files = datasetDir.listFiles();
//        debug("---------- Test upload lobcder dataset------------");
//        sum = 0;
//        for (File f : files) {
//            String putPath = lobcdrTestPath + f.getName();
//            PutMethod put = new PutMethod(putPath);
//            RequestEntity requestEntity = new InputStreamRequestEntity(new FileInputStream(f));
//            put.setRequestEntity(requestEntity);
//            long start_time = System.currentTimeMillis();
//            int status = client.executeMethod(put);
//            long total_millis = System.currentTimeMillis() - start_time;
//            assertEquals(HttpStatus.SC_CREATED, status);
//            double len = f.length();
//            double lobcderUpSpeed = (len / 1024.0) / (total_millis / 1000.0);
//            debug("lobcder upload speed=" + lobcderUpSpeed
//                    + "KB/s");
//            sum += lobcderUpSpeed;
//        }
//
//        mean = sum / datasetDir.list().length;
//
//        debug("mean backend upload speed=" + mean
//                + "KB/s");
//    }
    private static void debug(String msg) {
        System.err.println("debug: " + msg);
    }

    private static void initBackendDriver(Properties prop) throws MalformedURLException, Exception {
        GlobalConfig.setBaseLocation(new URL("http://dummy/url"));
        // runtime configuration
        GlobalConfig.setHasUI(false);
        GlobalConfig.setIsApplet(true);
        GlobalConfig.setPassiveMode(true);
        GlobalConfig.setIsService(true);
        GlobalConfig.setInitURLStreamFactory(false);
        GlobalConfig.setAllowUserInteraction(false);

        VRS.getRegistry().addVRSDriverClass(
                nl.uva.vlet.vfs.cloud.CloudFSFactory.class);

        Global.init();

        vfsClient = new VFSClient();
        VRSContext context = vfsClient.getVRSContext();

        VRL vrl = new VRL(prop.getProperty(TestSettings.BACKEND_ENDPOINT));
        ServerInfo info = context.getServerInfoFor(vrl, true);


        info.setUsername(prop.getProperty(TestSettings.BACKEND_USERNAME));
        info.setPassword(prop.getProperty(TestSettings.BACKEND_PASSWORD));

        info.setAttribute(ServerInfo.ATTR_DEFAULT_YES_NO_ANSWER, true);
        info.store();

        testDriverRemoteDir = vfsClient.createDir(vrl.append("deleteMe"), true);
        localTempDir = vfsClient.createDir(new VRL("file:/tmp/testDir"), true);
    }

    private static void initLobcderTestDir() throws IOException {
        MkColMethod mkcol = new MkColMethod(lobcdrTestPath);
        client.executeMethod(mkcol);
        assertTrue("status: " + mkcol.getStatusCode(), mkcol.getStatusCode() == HttpStatus.SC_CREATED || mkcol.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    private static class ScaleTest implements Runnable {

        private final int op;
        private File dataset;
        private int fileID;

        private ScaleTest(int op) {
            this.op = op;
        }

        @Override
        public void run() {
            try {
                switch (op) {
                    case CREATE_DATASET:
                        createDataset(FILE_SIZE_IN_KB);
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void setDataset(File dataset) {
            this.dataset = dataset;
        }

        private void setFileID(int j) {
            this.fileID = j;
        }

        private void createDataset(int sizeInk) throws FileNotFoundException, IOException {
            byte[] data = new byte[1024 * sizeInk];//1MB
            Random r = new Random();
            File f = new File(dataset.getAbsolutePath() + "/file" + this.fileID + ".dat");
            if (!f.exists() || f.length() != (1024 * sizeInk)) {
//                debug("Writing: " + f.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(f);
                r.nextBytes(data);
                fos.write(data);
                fos.flush();
                fos.close();
            }
        }
    }
}
