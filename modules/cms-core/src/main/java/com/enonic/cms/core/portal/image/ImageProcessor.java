/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.image;

import java.awt.image.BufferedImage;

import com.enonic.cms.framework.util.ImageHelper;

import com.enonic.cms.core.image.ImageRequest;
import com.enonic.cms.core.image.ImageResponse;
import com.enonic.cms.core.image.filter.BuilderContext;
import com.enonic.cms.core.image.filter.ImageFilter;
import com.enonic.cms.core.image.filter.ImageFilterBuilder;

public final class ImageProcessor
{
    private final ImageFilterBuilder imageFilterBuilder;

    public ImageProcessor()
    {
        this.imageFilterBuilder = new ImageFilterBuilder();
    }

    public ImageResponse process( ImageRequest req, BufferedImage image )
        throws Exception
    {
        image = getFilter( req ).filter( image );
        return createResponse( req, image );
    }

    private ImageFilter getFilter( ImageRequest req )
    {
        BuilderContext context = new BuilderContext();
        context.setBackgroundColor( req.getParams().getBackgroundColor() );
        return this.imageFilterBuilder.build( context, req.getParams().getFilter() );
    }

    private ImageResponse createResponse( ImageRequest req, BufferedImage image )
        throws Exception
    {
        String format = req.getFormat();
        if ( !ImageHelper.supportsAlphaChannel( format ) )
        {
            image = ImageHelper.removeAlphaChannel( image, req.getParams().getBackgroundColor() );
        }

        byte[] data = ImageHelper.writeImage( image, format, req.getParams().getQuality(), true );
        return new ImageResponse( req.getName(), data, format );
    }
}
