/*
 * Copyright 2016 Bridje Framework.
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

package org.bridje.jdbc.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import org.bridje.ioc.Component;
import org.bridje.ioc.PostConstruct;
import org.bridje.jdbc.JdbcService;
import org.bridje.jdbc.config.DataSourceConfig;
import org.bridje.jdbc.config.JdbcConfig;
import org.bridje.vfs.VFile;
import org.bridje.vfs.VFileInputStream;

@Component
class JdbcServiceImpl implements JdbcService
{
    private static final Logger LOG = Logger.getLogger(JdbcServiceImpl.class.getName());

    private Map<String, DataSourceImpl> dsMap;

    private Map<String, DataSourceImpl> schemaMap;

    private JdbcConfig config;

    @PostConstruct
    public void init()
    {
        try
        {
            dsMap = new ConcurrentHashMap<>();
            schemaMap = new ConcurrentHashMap<>();
            config = loadDefConfig();
            config.getDataSources().forEach(cfg -> dsMap.put(cfg.getName(), new DataSourceImpl(cfg)) );
            config.getSchemas().forEach(cfg -> schemaMap.put(cfg.getName(), dsMap.get(cfg.getDataSource())));
        }
        catch (IOException e)
        {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public DataSource getDataSourceBySchema(String schemaName)
    {
        DataSource result = schemaMap.get(schemaName);
        if(result == null) LOG.log(Level.WARNING, String.format("Could not find the DataSource for the schema %s.", schemaName));
        return result;
    }

    @Override
    public DataSource getDataSource(String name)
    {
        DataSource result = dsMap.get(name);
        if(result == null) LOG.log(Level.WARNING, String.format("The DataSource %s does not exists.", name));
        return result;
    }

    @Override
    public DataSource createDataSource(DataSourceConfig config)
    {
        return new DataSourceImpl(config);
    }

    @Override
    public void closeDataSource(DataSource dataSource) throws SQLException
    {
        if(dataSource instanceof DataSourceImpl)
        {
            ((DataSourceImpl) dataSource).close();
        }
        else
        {
            throw new IllegalArgumentException("Invalid Data Source.");
        }
    }

    @Override
    public void closeAllDataSource()
    {
        for (Map.Entry<String, DataSourceImpl> entry : dsMap.entrySet())
        {
            try
            {
                DataSourceImpl dataSource = entry.getValue();
                dataSource.close();
            }
            catch (SQLException ex)
            {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        dsMap.clear();
    }

    @Override
    public JdbcConfig loadDefConfig() throws IOException
    {
        VFile configFile = new VFile("/etc/jdbc.xml");
        if(configFile.exists())
        {
            try(InputStream is = new VFileInputStream(configFile))
            {
                return JdbcConfig.load(is);
            }
            catch (JAXBException ex)
            {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return new JdbcConfig();
    }

    @Override
    public void reconnectAll(JdbcConfig config)
    {
        if(config != null) this.config = config;
        this.config.getDataSources().forEach(cfg -> dsMap.get(cfg.getName()).reconnect(cfg) );
    }
}
