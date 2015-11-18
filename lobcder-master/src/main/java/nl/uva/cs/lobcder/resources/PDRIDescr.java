package nl.uva.cs.lobcder.resources;

import java.math.BigInteger;
import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * User: dvasunin Date: 01.03.13 Time: 13:57 To change this template use File |
 * Settings | File Templates.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
public class PDRIDescr {

    private String name;
    @Getter(AccessLevel.PUBLIC)
    private Long storageSiteId;
    private String resourceUrl;
    private String username;
    private String password;
    private Boolean encrypt;
    private BigInteger key;
    private Long pdriGroupRef;
    private Long id;
    private boolean isCashe;
//    @XmlTransient
//    public Long getStorageSiteId() {
//        return storageSiteId;
//    }
//    public boolean getEncrypt() {
//        return encrypt;
//    }
}
