/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.predictors;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.Vertex;
import static nl.uva.cs.lobcder.predictors.DBMapPredictor.type;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 * PP is a simple predictor which predicts successors to pairs of files. For
 * example, if the sequence BCD is observed, then the next time the sequence BC
 * is observed, D will be predicted as the next file to be requested.
 *
 * @author S. Koulouzis
 */
public class PredecessorPosition extends DBMapPredictor {

    List<String> stateList = new ArrayList<>();
    List<String> keyList = new ArrayList<>();
//    Map<String, LobState> stateMap = new HashMap<>();
    static Integer len;

    public PredecessorPosition() throws NamingException, IOException, SQLException {
        super();
        deleteAll();
        len = PropertiesHelper.PredecessorPositionLen();
    }

    @Override
    public void stop() {
        //Nothing to stop
    }

    @Override
    public Vertex getNextState(Vertex currentState) {

        String currentID;
        switch (type) {
            case state:
                currentID = currentState.getID();
                break;
            case resource:
                currentID = currentState.getResourceName();
                break;
            case method:
                currentID = currentState.getMethod().code;
                break;
            default:
                currentID = currentState.getID();
                break;
        }

        stateList.add(currentID);
        if (stateList.size() >= len) {
            try {
                String key = "";
                for (int i = 0; i < len; i++) {
                    key += stateList.get(i);
                }
                stateList.remove(0);
                return getSuccessor(key);
                //            return stateMap.get(key);
            } catch (SQLException ex) {
                Logger.getLogger(PredecessorPosition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(Vertex prevState, Vertex currentState) {

        String prevID = null;
        String currentID = null;
        switch (type) {
            case state:
                prevID = prevState.getID();
                currentID = currentState.getID();
                break;
            case resource:
                prevID = prevState.getResourceName();
                currentID = currentState.getResourceName();
                break;
            case method:
                prevID = prevState.getMethod().code;
                currentID = currentState.getMethod().code;
                break;
            default:
                prevID = prevState.getID();
                currentID = currentState.getID();
                break;
        }
        keyList.add(prevID);
        if (keyList.size() >= len) {
            try {
                String key = "";
                for (int i = 0; i < len; i++) {
                    key += keyList.get(i);
                }
                putSuccessor(key, currentID, false);
                //            if (!stateMap.containsKey(key)) {
                //                stateMap.put(key, currentState);
                //            }
                keyList.remove(0);
            } catch (SQLException | UnknownHostException ex) {
                Logger.getLogger(PredecessorPosition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
