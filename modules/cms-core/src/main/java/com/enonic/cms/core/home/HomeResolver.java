/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.home;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Strings;

public final class HomeResolver
{
    private final Properties systemProperties;

    public HomeResolver()
    {
        this.systemProperties = new Properties();
    }

    public void addSystemProperties( final Properties props )
    {
        this.systemProperties.putAll( props );
    }

    public void addSystemProperties( final Map<String, String> map )
    {
        this.systemProperties.putAll( map );
    }

    public HomeDir resolve()
    {
        final File dir = validatePath( resolvePath() );
        return new HomeDir( dir );
    }

    private String resolvePath()
    {
        String path = this.systemProperties.getProperty( "cms.home" );
        if ( !Strings.isNullOrEmpty( path ) )
        {
            return path;
        }

        path = this.systemProperties.getProperty( "CMS_HOME" );
        if ( !Strings.isNullOrEmpty( path ) )
        {
            return path;
        }

        throw new IllegalArgumentException(
            "Home directory not set. Please set either [cms.home] system property or [CMS_HOME] environment variable." );
    }

    private File validatePath( final String path )
    {
        final File dir = new File( path ).getAbsoluteFile();
        if ( !dir.exists() || !dir.isDirectory() )
        {
            throw new IllegalArgumentException( "Invalid home directory: [" + path + "] is not a directory" );
        }

        return dir;
    }
}
