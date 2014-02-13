/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

public abstract class BlobRecord
{
    private final BlobKey key;

    public BlobRecord( final BlobKey key )
    {
        this.key = key;
    }

    public final BlobKey getKey()
    {
        return this.key;
    }

    public abstract long getLength();

    public abstract InputStream getStream()
        throws BlobStoreException;

    public final byte[] getAsBytes()
        throws BlobStoreException
    {
        final InputStream stream = getStream();

        try
        {
            return ByteStreams.toByteArray( stream );
        }
        catch ( IOException e )
        {
            throw new BlobStoreException( "Failed to get bytes from blob [" + this.key + "]", e );
        }
        finally
        {
            Closeables.closeQuietly( stream );
        }
    }

    public abstract File getAsFile();

    public final String toString()
    {
        return this.key.toString();
    }

    public final int hashCode()
    {
        return this.key.hashCode();
    }

    public final boolean equals( final Object object )
    {
        return ( object instanceof BlobRecord ) && this.key.equals( ( (BlobRecord) object ).key );
    }
}
