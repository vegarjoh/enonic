/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.image;

public final class ImageProcessorException
    extends RuntimeException
{
    public ImageProcessorException( String message, Exception e )
    {
        super( message, e );
    }
}
