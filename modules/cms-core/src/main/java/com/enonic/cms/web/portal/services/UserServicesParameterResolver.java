/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.structure.SitePath;

/**
 *
 */
public class UserServicesParameterResolver
{

    public static String resolveHandlerFromSitePath( SitePath userServicesSitePath )
    {
        Path localPath = userServicesSitePath.getLocalPath();

        if ( localPath.getPathElementsCount() < 2 )
        {
            return null;
        }

        return localPath.getPathElement( 1 );
    }


    public static String resolveOperationFromSitePath( SitePath userServicesSitePath )
    {
        Path localPath = userServicesSitePath.getLocalPath();

        if ( localPath.getPathElementsCount() < 3 )
        {
            return null;
        }

        return localPath.getPathElement( 2 );
    }
}
