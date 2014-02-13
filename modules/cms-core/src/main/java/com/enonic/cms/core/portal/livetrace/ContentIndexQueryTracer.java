/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.livetrace;

import java.util.Collection;
import java.util.Iterator;

import com.enonic.cms.core.content.index.ContentIndexQuery;
import com.enonic.cms.core.time.TimeService;

public class ContentIndexQueryTracer
{
    public static ContentIndexQueryTrace startTracing( final LivePortalTraceService livePortalTraceService )
    {
        if ( livePortalTraceService.tracingEnabled() )
        {
            return livePortalTraceService.startContentIndexQueryTracing();
        }
        else
        {
            return null;
        }
    }

    public static void stopTracing( ContentIndexQueryTrace trace, final LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }

    public static void traceQuery( ContentIndexQuery query, int index, int count, String translatedQuery, ContentIndexQueryTrace trace )
    {
        if ( trace != null )
        {
            trace.setIndex( index );
            trace.setCount( count );

            trace.setQuery( query.getQuery() );
            trace.setTranslatedQuery( translatedQuery );
            trace.setContentFilter( collectionToString( query.getContentFilter() ) );
            trace.setSectionFilter( collectionToString( query.getSectionFilter() ) );
            trace.setCategoryFilter( collectionToString( query.getCategoryFilter() ) );
            trace.setContentTypeFilter( collectionToString( query.getContentTypeFilter() ) );
            trace.setCategoryAccessTypeFilter( collectionToString( query.getCategoryAccessTypeFilter() ) );
            trace.setSecurityFilter( securityFilterToString( query.getSecurityFilter() ) );
        }
    }

    public static void traceMatchCount( final int matchCount, final ContentIndexQueryTrace trace )
    {
        if ( trace != null )
        {
            trace.setMatchCount( matchCount );
        }
    }

    private static String securityFilterToString( final Collection collection )
    {
        if ( collection == null || collection.size() == 0 )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < collection.size(); i++ )
        {
            sb.append( "*" );
            if ( i < collection.size() - 1 )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    private static String collectionToString( final Collection collection )
    {
        if ( collection == null || collection.size() == 0 )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator it = collection.iterator();
        while ( it.hasNext() )
        {
            sb.append( it.next() );

            if ( it.hasNext() )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    public static void traceElasticSearchStartTime( final ContentIndexQueryTrace trace, final TimeService timeService )
    {
        if ( trace != null )
        {
            trace.setElasticSearchStartTime( timeService.getNowAsDateTime() );
        }
    }

    public static void traceElasticSearchFinishedTime( final ContentIndexQueryTrace trace, final TimeService timeService )
    {
        if ( trace != null )
        {
            trace.setElasticSearchStopTime( timeService.getNowAsDateTime() );
        }
    }
}
