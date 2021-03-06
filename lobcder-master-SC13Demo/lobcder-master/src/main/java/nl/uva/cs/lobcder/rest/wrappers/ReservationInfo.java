/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
@Data
public class ReservationInfo {

    private String communicationID;
    private int storageHostIndex;
    private String storageHost;
    private String workerDataAccessURL;
    private String hostName;
}
