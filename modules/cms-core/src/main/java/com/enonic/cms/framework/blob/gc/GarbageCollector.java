/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.blob.gc;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import com.enonic.cms.framework.blob.BlobKey;
import com.enonic.cms.framework.blob.BlobStore;

@Component("blobStoreGarbageCollector")
public final class GarbageCollector
{
    private final static Logger LOG = LoggerFactory.getLogger( GarbageCollector.class );

    private BlobStore store;

    private UsedBlobKeyFinder finder;

    private boolean running;

    @Autowired
    public void setStore( final BlobStore store )
    {
        this.store = store;
    }

    @Autowired
    public void setFinder( final UsedBlobKeyFinder finder )
    {
        this.finder = finder;
    }

    private int deleteUnused()
        throws Exception
    {
        final Set<BlobKey> keys = Sets.newHashSet( this.store.getAllKeys() );
        keys.removeAll( this.finder.findKeys() );

        for ( final BlobKey key : keys )
        {
            this.store.deleteRecord( key );
        }

        return keys.size();
    }

    public void process()
    {
        if ( this.running )
        {
            return;
        }

        this.running = true;

        try
        {
            doProcess();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to run blob store garbage collect", e );
        }
        finally
        {
            this.running = false;
        }
    }

    private synchronized void doProcess()
        throws Exception
    {
        final long now = System.currentTimeMillis();
        LOG.debug( "Starting blob store garbage collect" );

        final int deleted = deleteUnused();
        LOG.debug( "Deleted " + deleted + " blob(s) that was not in use" );

        final long totalTime = System.currentTimeMillis() - now;
        LOG.info( "Garbage collected " + deleted + " blob(s) in " + totalTime + " ms" );
    }
}
