/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.framework.xml.StringSource;

import com.enonic.cms.core.resource.ResourceFile;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.resource.ResourceService;
import com.enonic.cms.core.xslt.XsltResourceHelper;

/**
 * This class resolves the stylesheets and also other resources. It should be renamed to reflect that it also resolves other files that
 * stylesheets.
 */
public final class StyleSheetURIResolver
        implements URIResolver
{

    private static final Logger LOG = LoggerFactory.getLogger( StyleSheetURIResolver.class );

    /**
     * Resource service.
     */
    private final ResourceService resourceService;

    /**
     * Construct the url resolver.
     */
    public StyleSheetURIResolver( ResourceService resourceService )
    {
        this.resourceService = resourceService;
    }

    /**
     * Resolve the reference.
     */
    public Source resolve( String href, String base )
            throws TransformerException
    {
        final ResourceKey resourceKey = ResourceKey.from( XsltResourceHelper.resolveRelativePath( href, base ) );
        final ResourceFile resource = this.resourceService.getResourceFile( resourceKey );

        if ( resource == null )
        {
            final String message =
                    "Failed to resolve resource, did not find it: " + resourceKey.toString() + " (" + href + ")";
            LOG.error( message );
            throw new TransformerException( message );
        }

        final String resourceData = resource.getDataAsString();

        if ( resourceData == null )
        {
            final String message =
                    "Failed to resolve resource, resource data was null: " + resourceKey.toString() + " (" + href + ")";
            LOG.error( message );
            throw new TransformerException( message );
        }

        return new StringSource( resourceData, resourceKey.toString() );
    }
}
