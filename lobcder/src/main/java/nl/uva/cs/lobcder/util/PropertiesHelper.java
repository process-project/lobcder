/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.webDav.resources.WebDataFileResource;

/**
 *
 * @author S. Koulouzis
 */
public class PropertiesHelper {

    public static List<String> getWorkers() {
        ArrayList<String> workers = new ArrayList<>();
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/workers");
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            workers = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                workers.add(line);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return workers;
    }

    public static List<String> getNonRedirectableUserAgents() {
        ArrayList<String> workers = new ArrayList<>();
        BufferedReader br = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("/user-agents");
            br = new BufferedReader(new InputStreamReader(in));
            String line;
            workers = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                workers.add(line);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(WebDataFileResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return workers;
    }

    public static String getWorkerToken() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/lobcder.properties");
        Properties properties = new Properties();
        properties.load(in);
        return properties.getProperty("worker.token");
    }

    public static boolean doAggressiveReplication() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/lobcder.properties");
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("replication.aggressive"));
    }

    public static boolean doRedirectGets() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/lobcder.properties");
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty("get.redirect"));
    }

    public static boolean doRemoteAuth() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("/lobcder.properties");
        Properties properties = new Properties();
        properties.load(in);
        return Boolean.valueOf(properties.getProperty(" auth.useremote"));

    }
}