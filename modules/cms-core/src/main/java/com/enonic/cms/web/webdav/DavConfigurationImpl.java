/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.webdav;

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.util.MimeTypeResolver;

import com.enonic.cms.core.resource.access.ResourceAccessResolver;
import com.enonic.cms.core.security.SecurityService;

@Component
public final class DavConfigurationImpl
    implements DavConfiguration
{
    private File resourceRoot;

    private SecurityService securityService;

    private ResourceAccessResolver resourceAccessResolver;

    private MimeTypeResolver mimeTypeResolver;

    private Pattern excludePattern;

    @Override
    public File getResourceRoot()
    {
        return this.resourceRoot;
    }

    @Value("${cms.resource.path}")
    public void setResourceRoot( final File value )
    {
        this.resourceRoot = value;
    }

    @Value("${cms.webdav.excludePattern}")
    public void setExcludePattern( final String value )
    {
        this.excludePattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public SecurityService getSecurityService()
    {
        return this.securityService;
    }

    @Autowired
    public void setSecurityService( final SecurityService value )
    {
        this.securityService = value;
    }

    @Override
    public ResourceAccessResolver getResourceAccessResolver()
    {
        return resourceAccessResolver;
    }

    @Autowired
    public void setResourceAccessResolver( final ResourceAccessResolver value )
    {
        this.resourceAccessResolver = value;
    }

    @Override
    public MimeTypeResolver getMimeTypeResolver()
    {
        return mimeTypeResolver;
    }

    @Autowired
    public void setMimeTypeResolver( final MimeTypeResolver value )
    {
        this.mimeTypeResolver = value;
    }

    @Override
    public boolean isHidden( final String name )
    {
        return this.excludePattern.matcher(name).matches();
    }
}
