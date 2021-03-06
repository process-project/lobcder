/*
 * Copyright 2015 alogo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.uva.cs.lobcder.rest.wrappers;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import nl.uva.cs.lobcder.resources.PDRIDescr;

/**
 *
 * @author S. Koulouzis
 */
@XmlRootElement
public class PDRIDescrWrapperList {
     private List<PDRIDescr> pdris;

    /**
     * @return the pdris
     */
    public List<PDRIDescr> getPdris() {
        return pdris;
    }

    /**
     * @param pdris the pdris to set
     */
    public void setPdris(List<PDRIDescr> pdris) {
        this.pdris = pdris;
    }
}
