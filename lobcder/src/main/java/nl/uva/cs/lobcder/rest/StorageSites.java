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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
    public List<StorageSiteWrapper> getXml() throws FileNotFoundException, VlException, URISyntaxException, IOException, MalformedURLException, Exception {
        MyPrincipal mp = (MyPrincipal) request.getAttribute("myprincipal");
        if (mp.isAdmin()) {
            try (Connection cn = getCatalogue().getConnection()) {
                List<StorageSiteWrapper> res = queryStorageSites(mp, cn);
                return res;
            } catch (SQLException ex) {
                log.log(Level.SEVERE, null, ex);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return null;

    }

    private List<StorageSiteWrapper> queryStorageSites(@Nonnull MyPrincipal mp, @Nonnull Connection cn) throws SQLException {
        MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        List<String> ids = queryParameters.get("id");
        if (ids !=null && ids.size() >0 && ids.get(0).equals("all")) {
            Collection<StorageSite> sites = getCatalogue().getStorageSites(cn);
            Collection<StorageSite> cachesites = getCatalogue().getCacheStorageSites(cn);
            List<StorageSiteWrapper> sitesWarpper = new ArrayList<>();
            for (StorageSite s : sites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sitesWarpper.add(sw);
            }
            for (StorageSite s : cachesites) {
                StorageSiteWrapper sw = new StorageSiteWrapper();
                CredentialWrapped cw = new CredentialWrapped();
                cw.setStorageSitePassword(s.getCredential().getStorageSitePassword());
                cw.setStorageSiteUsername(s.getCredential().getStorageSiteUsername());
                sw.setCredential(cw);
                sw.setCurrentNum(s.getCurrentNum());
                sw.setEncrypt(s.isEncrypt());
                sw.setQuotaNum(s.getQuotaNum());
                sw.setQuotaSize(s.getQuotaSize());
                sw.setResourceURI(s.getResourceURI());
                sw.setStorageSiteId(s.getStorageSiteId());
                sitesWarpper.add(sw);
            }
            return sitesWarpper;
        }
        return null;
    }
}
