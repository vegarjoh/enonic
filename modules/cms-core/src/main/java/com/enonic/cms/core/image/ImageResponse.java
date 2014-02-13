/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class ImageResponse
{
    private final String name;

    private final byte[] data;

    private final String format;

    private boolean imageNotFound = false;

    public static ImageResponse notFound()
    {
        ImageResponse response = new ImageResponse();
        response.imageNotFound = true;
        return response;
    }

    private ImageResponse()
    {
        this( null, null, null );
    }

    public String getName()
    {
        return this.name;
    }

    public boolean isImageNotFound()
    {
        return imageNotFound;
    }

    public ImageResponse( String name, byte[] data, String format )
    {
        this.name = name;
        this.data = data;
        this.format = format;
    }

    public String getMimeType()
    {
        return "image/" + this.format;
    }

    public int getSize()
    {
        return this.data.length;
    }

    public byte[] getData()
    {
        return this.data;
    }

    public InputStream getDataAsStream()
    {
        return new ByteArrayInputStream( this.data );
    }
}
