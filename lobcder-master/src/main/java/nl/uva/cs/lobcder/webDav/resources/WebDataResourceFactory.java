package nl.uva.cs.lobcder.webDav.resources;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.resource.Resource;
import java.io.UnsupportedEncodingException;

import nl.uva.cs.lobcder.auth.AuthI;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.Constants;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.uva.cs.lobcder.catalogue.TokensDeleteSweep;

public class WebDataResourceFactory implements ResourceFactory {

    private JDBCatalogue catalogue;
    private List<AuthI> authList;
    private int attempts = 0;

    @Override
    public Resource getResource(String host, String strPath) {

        if (strPath.equals("/login.html")) {
            return null;
        }

        Path ldri = Path.path(strPath);
        String first;
        do {
            first = ldri.getFirst();
            ldri = ldri.getStripFirst();
        } while (!first.equals("dav"));
//        String escaedPath = StringEscapeUtils.escapeSql(strPath);
//        strPath = StringEscapeUtils.escapeHtml(escaedPath);
//        strPath  = strPath.replaceAll("'", "\'");
//       strPath =  strPath.replaceAll ("#", "\\#" );
//                ,    '%', '\%' )
//                ,    '_', '\_' )
//                ,    '[', '\[' )

//        try (Connection cn = catalogue.getConnection()) {
        try {
            Logger.getLogger(WebDataResourceFactory.class.getName()).log(Level.FINER, "getResource:  strPath: {0} path: {1} ldri: {2}" + "\n" + "\tgetResource:  host: {3} path: {4}", new Object[]{strPath, Path.path(strPath), ldri, host, ldri});
//            LogicalData entry = catalogue.getLogicalDataByPath(ldri, cn);
            LogicalData entry = getCatalogue().getLogicalDataByPath(ldri);
            if (entry == null) {
                return null;
            }

            if (entry.getType().equals(Constants.LOGICAL_FOLDER)) {
                return new WebDataDirResource(entry, ldri, getCatalogue(), getAuthList());
            }
            if (entry.getType().equals(Constants.LOGICAL_FILE)) {
                return new WebDataFileResource(entry, ldri, getCatalogue(), getAuthList());
            }
            attempts = 0;
//            return null;
        } catch (SQLException ex) {
            if (attempts <= Constants.RECONNECT_NTRY) {
                attempts++;
                getResource(host, strPath);
            } else {
                Logger.getLogger(WebDataResourceFactory.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebDataResourceFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @return the catalogue
     */
    public JDBCatalogue getCatalogue() {
        return catalogue;
    }

    /**
     * @param catalogue the catalogue to set
     */
    public void setCatalogue(JDBCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    /**
     * @return the authList
     */
    public List<AuthI> getAuthList() {
        return authList;
    }

    /**
     * @param authList the authList to set
     */
    public void setAuthList(List<AuthI> authList) {
        this.authList = authList;
    }
}
