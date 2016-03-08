/*
 * Copyright 2014 S. Koulouzis.
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
package nl.uva.cs.lobcder.predictors;

import io.milton.common.Path;
import io.milton.http.Request;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.naming.NamingException;
import nl.uva.cs.lobcder.optimization.Vertex;
import nl.uva.cs.lobcder.util.MyDataSource;
import nl.uva.cs.lobcder.resources.LogicalData;
import nl.uva.cs.lobcder.util.PropertiesHelper;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.method;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.resource;
import static nl.uva.cs.lobcder.util.PropertiesHelper.PREDICTION_TYPE.state;

/**
 *
 * Uses the DB as its hashmap
 *
 * @author S. Koulouzis
 */
public class DBMapPredictor extends MyDataSource implements Predictor {

    public static PropertiesHelper.PREDICTION_TYPE type;

    public DBMapPredictor() throws NamingException, IOException {
        type = PropertiesHelper.getPredictionType();
    }

    protected void putSuccessor(String key, String ID, boolean replace) throws SQLException, UnknownHostException {
        try (Connection connection = getConnection()) {
            Long uid = null;
            try (PreparedStatement ps = connection.prepareStatement("select uid "
                    + "from successor_table where keyVal = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    uid = rs.getLong(1);
                }
            }

            if (replace && uid != null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "UPDATE successor_table SET `lobStateID` = ? WHERE uid = ?", Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, ID);
                    preparedStatement.setLong(2, uid);
                    preparedStatement.execute();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                }
            } else if (uid == null) {
                String query = "INSERT INTO successor_table (keyVal, lobStateID) VALUES (?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        query, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, key);
                    preparedStatement.setString(2, ID);
                    preparedStatement.execute();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                }
            }
            connection.commit();
            connection.close();
        }
    }

    protected Integer getOoccurrences(String key) throws SQLException {
        Integer value = null;
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("select occurrences from "
                    + "occurrences_table where keyVal= ? ")) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    value = rs.getInt(1);
                }
            }
            connection.commit();
        }
        return value;
    }

    protected void putOoccurrences(String key, Integer occurrences) throws SQLException {
        try (Connection connection = getConnection()) {
            Integer value = null;
            Long uid = null;
            try (PreparedStatement ps = connection.prepareStatement("select uid,occurrences from "
                    + "occurrences_table where keyVal= ? ")) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    uid = rs.getLong(1);
                    value = rs.getInt(2);
                }
            }
            if (value == null) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO occurrences_table(keyVal, occurrences)"
                        + " VALUES (?, ? )", Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, key);
                    preparedStatement.setInt(2, occurrences);
                    preparedStatement.executeUpdate();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "UPDATE occurrences_table SET `occurrences` = ? WHERE uid = ?",
                        Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setInt(1, occurrences);
                    preparedStatement.setLong(2, uid);
                    preparedStatement.executeUpdate();
                    ResultSet rs = preparedStatement.getGeneratedKeys();
                    rs.next();
                }
            }

            connection.commit();
        }

    }

    protected Vertex getSuccessor(String key) throws SQLException {
        Vertex ls = null;
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("select "
                    + "lobStateID from successor_table WHERE keyVal = ? ")) {
                ps.setString(1, key);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String id = rs.getString(1);
                    Request.Method method;
                    String resource;
                    switch (type) {
                        case state:
                            method = Request.Method.valueOf(id.split(",")[0]);
                            resource = id.split(",")[1];
                            break;
                        case resource:
                            resource = id;
                            method = null;
                            break;
                        case method:
                            resource = "";
                            method = Request.Method.valueOf(id);
                            break;
                        default:
                            method = Request.Method.valueOf(id.split(",")[0]);
                            resource = id.split(",")[1];
                            break;
                    }
                    ls = new Vertex(method, resource);
                }
            }
            connection.commit();
        }
        return ls;
    }

    protected void deleteAll() throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM successor_table")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM occurrences_table")) {
                ps.executeUpdate();
            }

            connection.commit();
        }
    }

    public LogicalData getLogicalDataByPath(Path logicalResourceName, @Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT uid FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
            long parent = 1;
            String parts[] = logicalResourceName.getParts();
            if (parts.length == 0) {
                parts = new String[]{""};
            }
            for (int i = 0; i != parts.length; ++i) {
                String p = parts[i];
                if (i == (parts.length - 1)) {
                    try (PreparedStatement preparedStatement1 = connection.prepareStatement(
                            "SELECT uid, ownerId, datatype, createDate, modifiedDate, ldLength, "
                            + "contentTypesStr, pdriGroupRef, isSupervised, checksum, lastValidationDate, "
                            + "lockTokenID, lockScope, lockType, lockedByUser, lockDepth, lockTimeout, "
                            + "description, locationPreference, status "
                            + "FROM ldata_table WHERE ldata_table.parentRef = ? AND ldata_table.ldName = ?")) {
                        preparedStatement1.setLong(1, parent);
                        preparedStatement1.setString(2, p);
                        ResultSet rs = preparedStatement1.executeQuery();
                        if (rs.next()) {
                            res = new LogicalData();
                            res.setUid(rs.getLong(1));
                            res.setParentRef(parent);
                            res.setOwner(rs.getString(2));
                            res.setType(rs.getString(3));
                            res.setName(p);
                            res.setCreateDate(rs.getTimestamp(4).getTime());
                            res.setModifiedDate(rs.getTimestamp(5).getTime());
                            res.setLength(rs.getLong(6));
                            res.setContentTypesAsString(rs.getString(7));
                            res.setPdriGroupId(rs.getLong(8));
                            res.setSupervised(rs.getBoolean(9));
                            res.setChecksum(rs.getString(10));
                            res.setLastValidationDate(rs.getLong(11));
                            res.setLockTokenID(rs.getString(12));
                            res.setLockScope(rs.getString(13));
                            res.setLockType(rs.getString(14));
                            res.setLockedByUser(rs.getString(15));
                            res.setLockDepth(rs.getString(16));
                            res.setLockTimeout(rs.getLong(17));
                            res.setDescription(rs.getString(18));
//                            res.setDataLocationPreference(rs.getString(19));
                            res.setStatus(rs.getString(20));
                            return res;
                        } else {
                            return null;
                        }
                    }
                } else {
                    preparedStatement.setLong(1, parent);
                    preparedStatement.setString(2, p);
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        parent = rs.getLong(1);
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    public LogicalData getLogicalDataByUid(Long UID, @Nonnull Connection connection) throws SQLException {
        LogicalData res = null;
        if (res != null) {
            return res;
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT parentRef, ownerId, datatype, ldName, "
                + "createDate, modifiedDate, ldLength, contentTypesStr, pdriGroupRef, "
                + "isSupervised, checksum, lastValidationDate, lockTokenID, lockScope, "
                + "lockType, lockedByUser, lockDepth, lockTimeout, description, locationPreference, status "
                + "FROM ldata_table WHERE ldata_table.uid = ?")) {
            ps.setLong(1, UID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = new LogicalData();
                res.setUid(UID);
                res.setParentRef(rs.getLong(1));
                res.setOwner(rs.getString(2));
                res.setType(rs.getString(3));
                res.setName(rs.getString(4));
                res.setCreateDate(rs.getTimestamp(5).getTime());
                res.setModifiedDate(rs.getTimestamp(6).getTime());
                res.setLength(rs.getLong(7));
                res.setContentTypesAsString(rs.getString(8));
                res.setPdriGroupId(rs.getLong(9));
                res.setSupervised(rs.getBoolean(10));
                res.setChecksum(rs.getString(11));
                res.setLastValidationDate(rs.getLong(12));
                res.setLockTokenID(rs.getString(13));
                res.setLockScope(rs.getString(14));
                res.setLockType(rs.getString(15));
                res.setLockedByUser(rs.getString(16));
                res.setLockDepth(rs.getString(17));
                res.setLockTimeout(rs.getLong(18));
                res.setDescription(rs.getString(19));
//                res.setDataLocationPreference(rs.getString(20));
                res.setStatus(rs.getString(21));
                return res;
            } else {
                return null;
            }
        }
    }

    public String getPathforLogicalData(LogicalData ld, @Nonnull Connection connection) throws SQLException {
        String res = null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ldName, parentRef FROM ldata_table WHERE uid = ?")) {
            PathInfo pi = new PathInfo(ld.getName(), ld.getParentRef());
            List<PathInfo> pil = new ArrayList<>();
            getPathforLogicalData(pi, pil, ps);
            res = "";
            Collections.reverse(pil);
            for (PathInfo pi1 : pil) {
                res = res + "/" + pi1.getName();
            }
            return res;
        }
    }

    private void getPathforLogicalData(PathInfo pi, List<PathInfo> pil, PreparedStatement ps) throws SQLException {
        pil.add(pi);
        if (pi != null && pi.getParentRef() != 1) {
            ps.setLong(1, pi.getParentRef());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pi = new PathInfo(rs.getString(1), rs.getLong(2));
                    getPathforLogicalData(pi, pil, ps);
                }
            }
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public Vertex getNextState(Vertex currentState) {
        return null;
    }

    @Override
    public void setPreviousStateForCurrent(Vertex prevState, Vertex currentState) {
    }

    public class PathInfo {

        private String name;
        private Long parentRef;

        private PathInfo(String name, Long parentRef) {
            this.name = name;
            this.parentRef = parentRef;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the parentRef
         */
        public Long getParentRef() {
            return parentRef;
        }

        /**
         * @param parentRef the parentRef to set
         */
        public void setParentRef(Long parentRef) {
            this.parentRef = parentRef;
        }
    }
}
