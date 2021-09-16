/*
 * Copyright 2017 Bridje Framework.
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

package org.bridje.sql.impl;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridje.sql.*;

class SchemaFixer
{
    private static final Logger LOG = Logger.getLogger(SchemaFixer.class.getName());

    private final Connection connection;

    private final Schema schema;

    private final SQLDialect dialect;

    public SchemaFixer(Connection connection, SQLDialect dialect, Schema schema)
    {
        this.dialect = dialect;
        this.connection = connection;
        this.schema = schema;
    }

    public void doFix() throws SQLException
    {
        fixTables(connection, schema.getTables());
        fixForeignKeys(connection, schema.getForeignKeys());
        fixIndexes(connection, schema.getIndexes());
    }

    private void fixTables(Connection connection, Table[] tables) throws SQLException
    {
        for (Table table : tables)
        {
            fixTable(connection, table);
        }

        for (Table table : tables)
        {
            fixForeignKeys(connection, table);
        }
    }

    private void fixIndexes(Connection connection, Index[] indexes) throws SQLException
    {
        for (Index index : indexes)
        {
            fixIndex(connection, index);
        }
    }

    private void fixForeignKeys(Connection connection, ForeignKey[] foreignKeys) throws SQLException
    {
        for (ForeignKey fk : foreignKeys)
        {
            fixForeignKey(connection, fk);
        }
    }

    private void fixTable(Connection connection, Table table) throws SQLException
    {
        DatabaseMetaData metaData = connection.getMetaData();
        if(tableExists(metaData, table))
        {
            fixColumns(connection, table);
        }
        else
        {
            createTable(connection, table);
        }
        logMissingColumns(connection, table);
        fixIndexes(connection, table.getIndexes());
    }

    private void fixForeignKeys(Connection connection, Table table) throws SQLException
    {
        fixForeignKeys(connection, table.getForeignKeys());
    }

    private boolean tableExists(DatabaseMetaData metadata, Table table) throws SQLException
    {
        try (ResultSet resultSet = metadata.getTables(null, null, table.getName(), null))
        {
            if (resultSet.next()) return true;
        }
        return false;
    }

    private boolean columnExists(DatabaseMetaData metadata, Column<?, ?> column) throws SQLException
    {
        try (ResultSet resultSet = metadata.getColumns(null, null, column.getTable().getName(), column.getName()))
        {
            if (resultSet.next()) return true;
        }
        return false;
    }

    private boolean isNullable(DatabaseMetaData metadata, Column<?, ?> column) throws SQLException
    {
        try (ResultSet resultSet = metadata.getColumns(null, null, column.getTable().getName(), column.getName()))
        {
            if (resultSet.next())
            {
                String str = resultSet.getString("IS_NULLABLE");
                if(str != null && !str.trim().isEmpty())
                {
                    return "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE"));
                }
            }
        }
        return false;
    }

    private boolean isSameType(DatabaseMetaData metadata, Column<?, ?> column) throws SQLException
    {
        try (ResultSet resultSet = metadata.getColumns(null, null, column.getTable().getName(), column.getName()))
        {
            if (resultSet.next())
            {
                int existingType = resultSet.getInt("DATA_TYPE");
                int columnType = column.getSQLType().getJDBCType().getVendorTypeNumber();
                if(areSimilarType(existingType, columnType, false))
                {
                    if (existingType == Types.LONGVARCHAR ||
                            existingType == Types.VARCHAR)
                    {
                        int length = resultSet.getInt("COLUMN_SIZE");
                        if(length != column.getSQLType().getLength())
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

    private boolean areSimilarType(int type1, int type2, boolean internal)
    {
        if (type1 == type2) return true;
        if (type1 == Types.BOOLEAN ||
                type1 == Types.TINYINT ||
                type1 == Types.SMALLINT ||
                type1 == Types.BIT)
        {
            if (type2 == Types.BOOLEAN ||
                type2 == Types.TINYINT  ||
                type2 == Types.SMALLINT ||
                type2 == Types.BIT)
            {
                return true;
            }
        }
        if (type1 == Types.DOUBLE ||
                type1 == Types.FLOAT ||
                type1 == Types.REAL ||
                type1 == Types.DECIMAL ||
                type1 == Types.NUMERIC)
        {
            if (type2 == Types.DOUBLE ||
                    type2 == Types.FLOAT ||
                    type2 == Types.REAL ||
                    type2 == Types.DECIMAL ||
                    type2 == Types.NUMERIC)
            {
                return true;
            }
        }
        if (type1 == Types.LONGVARCHAR ||
                type1 == Types.VARCHAR)
        {
            if (type2 == Types.LONGVARCHAR ||
                    type2 == Types.VARCHAR)
            {
                return true;
            }
        }
        if (!internal)
            return areSimilarType(type2, type1, true);
        return false;
    }

    private void fixColumns(Connection connection, Table table) throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        for (Column<?, ?> column : table.getColumns())
        {
            if (!columnExists(metadata, column))
            {
                try
                {
                    List<Object> params = new ArrayList<>();
                    String sql = dialect.addColumn(column, params);
                    SQLStatement sqlStmt = new SQLStatementImpl(null, sql, params.toArray(), false);
                    executeStmt(connection, sqlStmt);
                }
                catch (SQLException e)
                {
                    LOG.log(Level.SEVERE, String.format("Could not create column %s on table %s.", column.getName(), table.getName()), e);
                }
            }
            else
            {
                boolean isNullable = isNullable(metadata, column);
                if (isNullable != column.isAllowNull() || !isSameType(metadata, column))
                {
                    if(!isSameType(metadata, column))
                    {
                        LOG.log(Level.INFO, "Changing column {0}", column.getName());
                    }
                    try
                    {
                        List<Object> params = new ArrayList<>();
                        String[] sqls = dialect.changeColumn(column.getName(), column, params);
                        for (String sql : sqls)
                        {
                            SQLStatement sqlStmt = new SQLStatementImpl(null, sql, params.toArray(), false);
                            executeStmt(connection, sqlStmt);
                        }
                    }
                    catch (SQLException e)
                    {
                        LOG.log(Level.SEVERE, String.format("Could not change column %s on table %s.", column.getName(), table.getName()), e);
                    }
                }
            }
        }
    }

    private void createTable(Connection connection, Table table) throws SQLException
    {
        try
        {
            List<Object> params = new ArrayList<>();
            String sql = dialect.createTable(table, params);
            SQLStatement sqlStmt = new SQLStatementImpl(null, sql, params.toArray(), false);
            executeStmt(connection, sqlStmt);
        }
        catch (SQLException e)
        {
            LOG.log(Level.SEVERE, String.format("Could not create table %s.", table.getName()), e);
        }
    }

    private void fixIndex(Connection connection, Index index) throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();

        boolean indexExists = indexExists(metadata, index);
        String sql = null;
        String action = "";
        List<Object> params = new ArrayList<>();
        if (index.mustRemove() && indexExists)
        {
            action = "remove";
            sql = dialect.dropIndex(index, params);
        }
        else if (!index.mustRemove() && !indexExists)
        {
            action = "create";
            sql = dialect.createIndex(index, params);
        }

        if (sql != null)
            try
            {
                SQLStatement sqlStmt = new SQLStatementImpl(null, sql, new Object[0], false);
                executeStmt(connection, sqlStmt);
            }
            catch (SQLException e)
            {
                LOG.log(Level.SEVERE, String.format("Could not %s index %s on table %s.",
                    action, index.getName(), index.getTable().getName()), e);
            }
    }

    private boolean indexExists(DatabaseMetaData metadata, Index index) throws SQLException
    {
        List<String> columnNames = new ArrayList<>();
        for (Column<?, ?> column : index.getColumns())
        {
            columnNames.add(column.getName());
        }
        Map<String, List<String>> idxMap = new HashMap<>();
        try (ResultSet resultSet = metadata.getIndexInfo(null, null, index.getTable().getName(), false, true))
        {
            while (resultSet.next())
            {
                String indexName = resultSet.getString("INDEX_NAME");
                String colName = resultSet.getString("COLUMN_NAME");
                List<String> lst = idxMap.get(indexName);
                if(lst == null)
                {
                    lst = new ArrayList<>();
                    idxMap.put(indexName, lst);
                }
                lst.add(colName);
            }
        }
        return idxMap.values()
                    .stream()
                    .filter( v -> Arrays.equals(v.toArray(), columnNames.toArray()))
                    .count() >= 1;
    }

    private void fixForeignKey(Connection connection, ForeignKey fk) throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        if(!foreignKeyExists(metadata, fk))
        {
            try
            {
                List<Object> params = new ArrayList<>();
                String sql = dialect.createForeignKey(fk, params);
                SQLStatement sqlStmt = new SQLStatementImpl(null, sql, new Object[0], false);
                executeStmt(connection, sqlStmt);
            }
            catch (SQLException e)
            {
                LOG.log(Level.SEVERE, String.format("Could not create foreign key %s on table %s.", fk.getName(), fk.getTable().getName()), e);
            }
        }
    }

    private boolean foreignKeyExists(DatabaseMetaData metadata, ForeignKey fk) throws SQLException
    {
        try (ResultSet resultSet = metadata.getExportedKeys(null, null, fk.getReferences().getName()))
        {
            while (resultSet.next())
            {
                String fkTableName = resultSet.getString("FKTABLE_NAME");
                if(fk.getTable().getName().equals(fkTableName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private PreparedStatement prepareStatement(Connection cnn, SQLStatement sqlStmt) throws SQLException
    {
        PreparedStatement stmt;
        if(sqlStmt.isWithGeneratedKeys())
        {
            stmt = cnn.prepareStatement(sqlStmt.getSQL(), Statement.RETURN_GENERATED_KEYS);
        }
        else
        {
            stmt = cnn.prepareStatement(sqlStmt.getSQL());
        }

        Object[] params = sqlStmt.getParameters();
        for (int i = 0; i < params.length; i++)
        {
            stmt.setObject(i+1, params[i]);
        }
        return stmt;
    }

    private int executeStmt(Connection cnn, SQLStatement sqlStmt) throws SQLException
    {
        LOG.log(Level.INFO, sqlStmt.getSQL());
        try(PreparedStatement stmt = prepareStatement(cnn, sqlStmt))
        {
            return stmt.executeUpdate();
        }
    }

    private void logMissingColumns(Connection connection, Table table) throws SQLException
    {
        List<String> columns = findColumns(connection, table);
        columns.stream()
                .filter( c -> table.getColumn(c) == null )
                .forEach( c -> LOG.log(Level.WARNING, String.format("Column %s no longer exists in table %s.", c, table.getName())) );
    }

    private List<String> findColumns(Connection connection, Table table) throws SQLException
    {
        List<String> columns = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(null, null, table.getName(), null))
        {
            while(resultSet.next())
            {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
