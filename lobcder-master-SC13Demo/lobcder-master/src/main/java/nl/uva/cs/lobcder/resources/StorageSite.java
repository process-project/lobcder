/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.resources;

import lombok.Data;

/**
 *
 * @author S. Koulouzis
 */
@Data
public class StorageSite implements Cloneable {

    @Override
    public Object clone() {
        StorageSite clone = new StorageSite();
        clone.setStorageSiteId(storageSiteId);
        clone.setCredential(credential);
        clone.setCurrentNum(new Long(currentNum));
        clone.setCurrentSize(new Long(currentSize));
        clone.setQuotaNum(new Long(quotaNum));
        clone.setQuotaSize(new Long(quotaSize));
        clone.setResourceURI(resourceURI);
        clone.setEncrypt(encrypt);
        clone.setCache(cache);
        return clone;
    }
    private Long storageSiteId;
    private Long quotaSize;
    private Long quotaNum;
    private Long currentSize;
    private Long currentNum;
    private String resourceURI;
    private Credential credential;
    private boolean encrypt;
    private boolean cache;
}
