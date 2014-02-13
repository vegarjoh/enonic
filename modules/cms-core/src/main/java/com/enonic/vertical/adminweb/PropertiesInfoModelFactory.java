/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.vertical.adminweb;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import com.enonic.cms.core.tools.DataSourceInfoResolver;
import com.enonic.cms.core.vhost.VirtualHostResolver;

public class PropertiesInfoModelFactory
{
    private DataSourceInfoResolver dataSourceInfoResolver;

    private Properties configurationProperties;

    private Properties virtualHosts;


    public PropertiesInfoModelFactory( DataSourceInfoResolver dataSourceInfoResolver, Properties configurationProperties,
                                       final Properties virtualHosts )
    {
        this.dataSourceInfoResolver = dataSourceInfoResolver;
        this.configurationProperties = configurationProperties;
        this.virtualHosts = virtualHosts;
    }

    public PropertiesInfoModel createSystemPropertiesModel()
    {

        PropertiesInfoModel infoModel = new PropertiesInfoModel();

        try
        {
            infoModel.setSystemProperties( System.getProperties() );
            infoModel.setDatasourceProperties( this.dataSourceInfoResolver.getInfo( false ) );
            infoModel.setConfigurationProperties( getConfigurationProperties() );
            infoModel.setVhostProperties( virtualHosts );
        }
        catch ( Exception e )
        {
            throw new VerticalAdminException( "Not able to create properties-model", e );
        }

        return infoModel;
    }

    private Map<Object, Object> getConfigurationProperties()
    {
        return stripPasswords( this.configurationProperties );
    }


    private Properties stripPasswords( Properties secretProperties )
    {
        Properties publicProperties = new Properties();
        for ( Map.Entry<Object, Object> prop : secretProperties.entrySet() )
        {
            if ( prop.getKey() instanceof String )
            {
                String key = (String) prop.getKey();
                if ( key.matches( ".*[Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd]$" ) )
                {
                    publicProperties.put( key, "****" );
                }
                else
                {
                    publicProperties.put( key, prop.getValue() );
                }
            }
            else
            {
                publicProperties.put( prop.getKey(), prop.getValue() );
            }
        }

        return publicProperties;
    }


}
