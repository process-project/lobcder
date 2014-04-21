package nl.uva.cs.lobcder.catalogue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import lombok.Data;
import lombok.extern.java.Log;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: dvasunin Date: 25.02.13 Time: 16:31 To change this template use File |
 * Settings | File Templates.
 */
@Log
class WP4Sweep implements Runnable {

    private final DataSource datasource;
    private final WP4ConnectorI connector;

    public WP4Sweep(DataSource datasource, WP4ConnectorI connector) {
        this.datasource = datasource;
        this.connector = connector;
    }

    @Data
    public static class ResourceMetadata {
      String author = "";
      long localId = 0;
      String globalId;
      String name = "";
      String type = "";
      int views = 0;


      public String getXml() {
          String extension = "";
          int i = name.lastIndexOf('.');
          if (i > 0) {
              extension = name.substring(i + 1);
          }
          return new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                  .append("<resource_metadata>")
                  .append("<file>")
                  .append("<author>").append(author).append("</author>")
                  .append("<category>General Metadata</category>")
                  .append("<description>LOBCDER</description>")
                  .append("<linkedTo/>")
                  .append("<localID>").append(localId).append("</localID>")
                  .append("<name>").append(name).append("</name>")
                  .append("<rating>0</rating>")
                  .append("<relatedResources/>")
                  .append("<semanticAnnotations/>")
                  .append("<status>active</status>")
                  .append("<type>").append(type).append("</type>")
                  .append("<views>").append(views).append("</views>")
                  .append("<fileType>").append(extension).append("</fileType>")
                  .append("<subjectID/>")
                  .append("</file>")
                  .append("</resource_metadata>").toString();
      }
    }

    public static interface WP4ConnectorI {

        public String create(ResourceMetadata resourceMetadata) throws Exception;

        public String create_dev(ResourceMetadata resourceMetadata) throws Exception;

        public void update(ResourceMetadata resourceMetadata) throws Exception;

        public void update_dev(ResourceMetadata resourceMetadata) throws Exception;

        public void delete(String global_id) throws Exception;

        public void delete_dev(String global_id) throws Exception;
    }

    public static class WP4Connector implements WP4ConnectorI {

        private Client client;
        private XPathExpression expression;
        private final String uri;
        private final String uri_dev;


        public WP4Connector(String uri, String uri_dev) {
            try {
                this.uri = uri;
                this.uri_dev = uri_dev;
                client = Client.create();
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                expression = xpath.compile("/message/data[1]/_global_id[1]");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String create(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadata.getXml());
            if (response.getClientResponseStatus() == ClientResponse.Status.OK) {
                Node uidNode = (Node) expression.evaluate(new InputSource(response.getEntityInputStream()), XPathConstants.NODE);
                String result = uidNode.getTextContent();
                log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author,resourceMetadata.name, resourceMetadata.type, result});
                return result;
            } else {
                throw new Exception(response.getClientResponseStatus().toString());
            }
        }

        @Override
        public String create_dev(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri_dev);
            ClientResponse response = webResource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, resourceMetadata.getXml());
            if (response.getClientResponseStatus() == ClientResponse.Status.OK) {
                Node uidNode = (Node) expression.evaluate(new InputSource(response.getEntityInputStream()), XPathConstants.NODE);
                String result = uidNode.getTextContent();
                log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author,resourceMetadata.name, resourceMetadata.type, result});
                return result;
            } else {
                throw new Exception(response.getClientResponseStatus().toString());
            }
        }

        @Override
        public void update(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri);
            ClientResponse response = webResource.path(resourceMetadata.getGlobalId()).type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata.getXml());
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new Exception(response.getClientResponseStatus().toString());
            }
            log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, resourceMetadata.globalId});
        }

        @Override
        public void update_dev(ResourceMetadata resourceMetadata) throws Exception {
            WebResource webResource = client.resource(uri_dev);
            ClientResponse response = webResource.path(resourceMetadata.getGlobalId()).type(MediaType.APPLICATION_XML).put(ClientResponse.class, resourceMetadata.getXml());
            if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                throw new Exception(response.getClientResponseStatus().toString());
            }
            log.log(Level.FINE, "Send metadata to uri: {0} author: {1} name: {2} type: {3} global_id: {4}", new Object[]{uri, resourceMetadata.author, resourceMetadata.name, resourceMetadata.type, resourceMetadata.globalId});
        }

        @Override
        public void delete(String global_id) throws Exception {
            WebResource webResource = client.resource(uri);
            webResource.path(global_id).type(MediaType.APPLICATION_XML).delete();
            log.log(Level.FINE, "Deleting metadata. global_id: {0}", global_id);
        }

        @Override
        public void delete_dev(String global_id) throws Exception {
            WebResource webResource = client.resource(uri_dev);
            webResource.path(global_id).type(MediaType.APPLICATION_XML).delete();
            log.log(Level.FINE, "Deleting metadata. global_id: {0}", global_id);
        }
    }

    private void create(Connection connection, WP4ConnectorI wp4Connector) throws SQLException, UnknownHostException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY)) {
            try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_create = FALSE, global_id = ?, global_id_dev = ? WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT uid, ownerId, datatype, ldName, id FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_create=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setLocalId(rs.getLong(1));
                    rm.setAuthor(rs.getString(2));
                    rm.setType(rs.getString(3));
                    rm.setName(rs.getString(4));
                    rm.setViews(0);
                    try {
                        String gid = wp4Connector.create(rm);
                        String gid_dev = wp4Connector.create_dev(rm);
                        s2.setString(1, gid);
                        s2.setString(2, gid_dev);
                        s2.setLong(3, rs.getLong(5));
                        s2.executeUpdate();
                    } catch (Exception e) {
                        if (e instanceof com.sun.jersey.api.client.ClientHandlerException && e.getMessage().contains("java.net.UnknownHostException")) {
                            throw new java.net.UnknownHostException(e.getMessage());
                        } else {
                            WP4Sweep.log.log(Level.SEVERE, null, e);
                        }
                    }
                }
            }
        }
    }

    private void update(Connection connection, WP4ConnectorI wp4Connector) throws SQLException, UnknownHostException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY)) {
            try (PreparedStatement s2 = connection.prepareStatement("UPDATE wp4_table SET need_update = FALSE WHERE id = ?")) {
                ResultSet rs = s1.executeQuery("SELECT ownerId, ldName, global_id, views, id, global_id_dev FROM ldata_table JOIN wp4_table ON uid=local_id WHERE need_update=TRUE");
                while (rs.next()) {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setAuthor(rs.getString(1));
                    rm.setName(rs.getString(2));
                    rm.setGlobalId(rs.getString(3));
                    rm.setViews(rs.getInt(4));
                    try {
                        wp4Connector.update(rm);
                        rm.setGlobalId(rs.getString(6));
                        wp4Connector.update_dev(rm);
                        s2.setLong(1, rs.getLong(5));
                        s2.executeUpdate();
                    } catch (Exception e) {
                        if (e instanceof com.sun.jersey.api.client.ClientHandlerException && e.getMessage().contains("java.net.UnknownHostException")) {
                            throw new java.net.UnknownHostException(e.getMessage());
                        } else {
                            WP4Sweep.log.log(Level.SEVERE, null, e);
                        }
                    }
                }
            }
        }
    }

    private void delete(Connection connection, WP4ConnectorI wp4Connector) throws SQLException, UnknownHostException {
        try (Statement s1 = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_UPDATABLE)) {
            ResultSet rs = s1.executeQuery("SELECT global_id, id, global_id_dev FROM wp4_table WHERE local_id IS NULL");
            while (rs.next()) {
                try {
                    String global_id = rs.getString(1);
                    String global_id_dev = rs.getString(3);
                    if (global_id != null) {
                        wp4Connector.delete(global_id);
                    }
                    if(global_id_dev != null) {
                        wp4Connector.delete(global_id_dev);
                    }
                    rs.deleteRow();
                } catch (Exception e) {
                    if (e instanceof com.sun.jersey.api.client.ClientHandlerException && e.getMessage().contains("java.net.UnknownHostException")) {
                        throw new java.net.UnknownHostException(e.getMessage());
                    } else {
                        WP4Sweep.log.log(Level.SEVERE, null, e);
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(true);
            create(connection, connector);
            update(connection, connector);
            delete(connection, connector);
        } catch (Exception ex) {
            if (ex instanceof UnknownHostException) {
//                    unknownHostExceptionCounter++;
//                    if (unknownHostExceptionCounter >= Constants.RECONNECT_NTRY) {
//                        runSweeper = false;
//                    }
            } else {
                Logger.getLogger(WP4Sweep.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


    }
}
