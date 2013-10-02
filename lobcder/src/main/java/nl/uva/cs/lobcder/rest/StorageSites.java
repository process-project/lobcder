/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.auth.MyPrincipal;
import nl.uva.cs.lobcder.resources.StorageSite;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import nl.uva.vlet.exception.VlException;

/**
 *
 * @author S. Koulouzis
 */
@Log
@Path("storage_sites/")
public class StorageSites extends CatalogueHelper {

    @Context
    UriInfo info;
    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse servletResponse;

    public StorageSites() throws NamingException {
    }

    @Path("query/")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public StorageSiteWrapperList getXml() throws FileNotFoundException, VlException, URISyntaxException, IOException, MalformedURLException, Exception {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection cn = getCatalogue().getConnection()) {
                List<StorageSiteWrapper> res = queryStorageSites(mp, cn);
                StorageSiteWrapperList sswl = new StorageSiteWrapperList();
                sswl.setSites(res);
                return sswl;
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    @Path("set/")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public void set(JAXBElement<StorageSiteWrapperList> jbSites) throws SQLException {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection connection = getCatalogue().getConnection()) {
                StorageSiteWrapperList sites = jbSites.getValue();
                List<StorageSiteWrapper> sswl = sites.sites;
                for (StorageSiteWrapper ssw : sswl) {
                    log.log(Level.FINE, "sites: {0}", ssw.getResourceURI());
                }
                //                for(JAXBElement<StorageSiteWrapper> s : sites){
                //                    StorageSiteWrapper ssw = s.getValue();
                //                     log.log(Level.FINE, "sites: {0}", ssw.getResourceURI());
                //                }

            }
        }

    }

    private List<StorageSiteWrapper> queryStorageSites(@Nonnull MyPrincipal mp, @Nonnull Connection cn) throws SQLException {
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        List<String> ids = queryParameters.get("id");
        if (ids != null && ids.size() > 0 && ids.get(0).equals("all")) {
            Collection<StorageSite> sites = getCatalogue().getStorageSites(cn);
            Collection<StorageSite> cachesites = getCatalogue().getCacheStorageSites(cn);
            List<StorageSiteWrapper> sitesWarpper = new ArrayList<>();
            for (StorageSite s : sites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
//                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSitePassword("************");
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sw.setCache(false);
                sitesWarpper.add(sw);
            }
            for (StorageSite s : cachesites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
//                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSitePassword("************");
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sw.setCache(true);
                sitesWarpper.add(sw);
            }
            return sitesWarpper;
        }
        return null;
    }
}
