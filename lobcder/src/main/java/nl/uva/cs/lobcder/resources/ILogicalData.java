/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import com.bradmcevoy.common.Path;
import java.util.ArrayList;
import nl.uva.vlet.exception.VlException;
import nl.uva.vlet.vfs.VFSNode;
import nl.uva.vlet.vfs.VFile;

/**
 *
 * @author S. Koulouzis
 */
public interface ILogicalData {

    public Path getLDRI();
    
    public ArrayList<Path> getChildren();

    public void addChildren(ArrayList<Path> children);

    public void addChild(Path child);

    public ArrayList<StorageSite> getStorageSites();

    public void setStorageSites(ArrayList<StorageSite> storageResources);

    public Metadata getMetadata();

    public void setMetadata(Metadata metadata);

    public String getUID();

    public boolean hasChildren();

    public void removeChild(Path childPath);

    public Path getChild(Path path);

    public void setLDRI(Path path);

    public boolean isRedirectAllowed();

    public VFSNode getVFSNode() throws VlException;

    public boolean hasPhysicalData()throws VlException;

    public VFSNode createPhysicalData()throws VlException;
    
}