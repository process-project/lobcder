/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.webDav.resources;

import com.bradmcevoy.http.CustomProperty;

/**
 *
 * @author S. koulouzis
 */
class DataDistProperty implements CustomProperty{
    private String formattedValue="formattedValue";

    @Override
    public Class getValueClass() {
        return DataDistProperty.class;
    }

    @Override
    public Object getTypedValue() {
        return formattedValue;
    }

    @Override
    public String getFormattedValue() {
        return formattedValue;
    }

    @Override
    public void setFormattedValue(String value) {
        formattedValue = value;
    }
    
}
