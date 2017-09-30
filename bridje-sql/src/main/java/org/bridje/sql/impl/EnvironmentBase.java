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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridje.sql.Column;
import org.bridje.sql.SQLDialect;
import org.bridje.sql.SQLEnvironment;
import org.bridje.sql.SQLResultParser;
import org.bridje.sql.SQLResultSet;
import org.bridje.sql.SQLStatement;
import org.bridje.sql.Table;
import org.bridje.sql.Query;

abstract class EnvironmentBase implements SQLEnvironment
{
    private static final Logger LOG = Logger.getLogger(EnvironmentBase.class.getName());

    private final SQLDialect dialect;

    public EnvironmentBase(SQLDialect dialect)
    {
        this.dialect = dialect;
    }

    @Override
    public SQLDialect getDialect()
    {
        return dialect;
    }
    
    @Override
    public int update(Query query, Object... parameters) throws SQLException
    {
        return update(query.toStatement(getDialect(), parameters));
    }

    @Override
    public <T> List<T> fetchAll(Query query, SQLResultParser<T> parser, Object... parameters) throws SQLException
    {
        return fetchAll(query.toStatement(getDialect(), parameters), parser);
    }
    
    @Override
    public <T> T fetchOne(Query query, SQLResultParser<T> parser, Object... parameters) throws SQLException
    {
        return fetchOne(query.toStatement(getDialect(), parameters), parser);
    }

    protected int update(Connection cnn, SQLStatement sqlStmt) throws SQLException
    {
        LOG.log(Level.INFO, sqlStmt.getSQL());
        try(PreparedStatement stmt = prepareStatement(cnn, sqlStmt))
        {
            return stmt.executeUpdate();
        }
    }

    protected <T> List<T> fetchAll(Connection cnn, SQLStatement sqlStmt, SQLResultParser<T> parser) throws SQLException
    {
        LOG.log(Level.INFO, sqlStmt.getSQL());
        try(PreparedStatement stmt = prepareStatement(cnn, sqlStmt))
        {
            SQLResultSet rs;
            if(sqlStmt.isWithGeneratedKeys())
            {
                int value = stmt.executeUpdate();
                rs = new SQLResultSetImpl(stmt.getGeneratedKeys(), sqlStmt.getResultFields());
            }
            else
            {
                rs = new SQLResultSetImpl(stmt.executeQuery(), sqlStmt.getResultFields());
            }
            return fetchAll(rs, parser);
        }
    }

    protected <T> T fetchOne(Connection cnn, SQLStatement sqlStmt, SQLResultParser<T> parser) throws SQLException
    {
        LOG.log(Level.INFO, sqlStmt.getSQL());
        try(PreparedStatement stmt = prepareStatement(cnn, sqlStmt))
        {
            SQLResultSet rs;
            if(sqlStmt.isWithGeneratedKeys())
            {
                int value = stmt.executeUpdate();
                rs = new SQLResultSetImpl(stmt.getGeneratedKeys(), sqlStmt.getResultFields());
            }
            else
            {
                rs = new SQLResultSetImpl(stmt.executeQuery(), sqlStmt.getResultFields());
            }
            return fetchOne(rs, parser);
        }
    }
    
    protected PreparedStatement prepareStatement(Connection cnn, SQLStatement sqlStmt) throws SQLException
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

    protected void fixTable(Connection connection, Table table) throws SQLException
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
        fixIndexes(connection, table);
    }

    protected boolean tableExists(DatabaseMetaData metadata, Table table) throws SQLException
    {
        try (ResultSet resultSet = metadata.getTables(null, null, table.getName(), null))
        {
            if (resultSet.next()) return true;
        }
        return false;
    }

    protected boolean columnExists(DatabaseMetaData metadata, Column<?> column) throws SQLException
    {
        try (ResultSet resultSet = metadata.getColumns(null, null, column.getTable().getName(), column.getName()))
        {
            if (resultSet.next()) return true;
        }
        return false;
    }

    protected boolean indexExists(DatabaseMetaData metadata, Table table, Column<?>... columns) throws SQLException
    {
        List<String> columnNames = new ArrayList<>();
        for (Column<?> column : columns)
        {
            columnNames.add(column.getName());
        }
        Map<String, List<String>> idxMap = new HashMap<>();
        try (ResultSet resultSet = metadata.getIndexInfo(null, null, table.getName(), false, true))
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
        idxMap.forEach( (k, v) -> System.out.println(k + " " + Arrays.toString(v.toArray())));
        return idxMap.values()
                    .stream()
                    .filter( v -> Arrays.equals(v.toArray(), columnNames.toArray()))
                    .count() >= 1;
    }

    protected void createTable(Connection connection, Table table) throws SQLException
    {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        dialect.createTable(sb, table);
        for (Column<?> column : table.getColumns())
        {
            dialect.createColumn(sb, params, column, column.isKey());
        }
        dialect.primaryKey(sb, table.getPrimaryKey());
        String sql = sb.toString();
        SQLStatement sqlStmt = new SQLStatementImpl(null, sql, params.toArray(), false);
        update(connection, sqlStmt);
    }

    protected void fixIndexes(Connection connection, Table table) throws SQLException
    {
        /*
        DatabaseMetaData metadata = connection.getMetaData();
        Column<?>[] columns = table.getColumns();
        for (Column<?> column : columns)
        {
            if(column.getIndex() != null && column.getIndex() != ColumnIndexType.NONE)
            {
                if(!indexExists(metadata, column.getTable(), column))
                {
                    StringBuilder sb = new StringBuilder();
                    String idxName = "idx_" + column.getName();
                    dialect.createIndex(sb, idxName, column.getTable(), new Column<?>[]{column}, column.getIndex() == ColumnIndexType.UNIQUE);
                    String sql = sb.toString();
                    SQLStatement sqlStmt = new SQLStatementImpl(null, sql, new Object[0], false);
                    update(connection, sqlStmt);
                }
            }
        }
        */
    }

    protected void fixColumns(Connection connection, Table table) throws SQLException
    {
        DatabaseMetaData metadata = connection.getMetaData();
        List<Object> params = new ArrayList<>();
        Column<?>[] columns = table.getColumns();
        StringBuilder sb = new StringBuilder();
        dialect.alterTable(sb, table);
        int index = 0;
        for (Column<?> column : columns)
        {
            index++;
            if(!columnExists(metadata, column))
            {
                dialect.addColumn(sb, params, column, index == columns.length );
            }
        }
        String sql = sb.toString();
        SQLStatement sqlStmt = new SQLStatementImpl(null, sql, params.toArray(), false);
        update(connection, sqlStmt);
    }

    private <T> T fetchOne(SQLResultSet rs, SQLResultParser<T> parser) throws SQLException
    {
        if(rs.next())
        {
            return parser.parse(rs);
        }
        return null;
    }

    private <T> List<T> fetchAll(SQLResultSet rs, SQLResultParser<T> parser) throws SQLException
    {
        List<T> result = new ArrayList<>();
        while(rs.next())
        {
            result.add(parser.parse(rs));
        }
        return result;
    }
}
