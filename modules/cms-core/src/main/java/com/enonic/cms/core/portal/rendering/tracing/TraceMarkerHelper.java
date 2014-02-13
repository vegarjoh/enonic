/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering.tracing;

import org.apache.commons.lang.StringUtils;

import com.enonic.cms.core.portal.rendering.RenderedWindowResult;

/**
 * This class implements the render trace helper.
 */
public final class TraceMarkerHelper
{
    /**
     * This method modifies the markup with extra trace code.
     */
    public static String writePageMarker( RenderTraceInfo traceInfo, String markup, String outputMethod )
    {
        if ( outputMethod == null )
        {
            return markup;
        }

        outputMethod = outputMethod.toLowerCase();
        boolean usePageMarker = outputMethod.contains( "html" );

        if ( ( traceInfo != null ) && usePageMarker )
        {
            StringBuffer buffer = new StringBuffer( markup );
            int pos = buffer.indexOf( "</head>" );
            if ( pos > -1 )
            {
                String href = "__info__?type=css&key=" + traceInfo.getKey();
                href = makeXmlCompliantIfNeeded( href, outputMethod );
                buffer.insert( pos, writeCssInclude( href ) );
            }

            pos = buffer.indexOf( "</body>" );
            if ( pos > -1 )
            {
                String href = "__info__?type=javascript&key=" + traceInfo.getKey();
                href = makeXmlCompliantIfNeeded( href, outputMethod );
                buffer.insert( pos, writeJavaScriptInclude( href ) );
            }

            return buffer.toString();
        }
        else
        {
            return markup;
        }
    }

    private static String makeXmlCompliantIfNeeded( String url, String outputMethod )
    {

        if ( "xhtml".equals( outputMethod ) || "xml".equals( outputMethod ) )
        {
            return url.replaceAll( "&", "&amp;" );
        }

        return url;
    }

    /**
     * Return css tag.
     */
    private static String writeCssInclude( String href )
    {
        return "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + href + "\"/>";
    }

    /**
     * Return javascript tag.
     */
    private static String writeJavaScriptInclude( String href )
    {
        return "<script charset=\"UTF-8\" type=\"text/javascript\" src=\"" + href + "\">//</script>";
    }

    public static void wrapResultWithPortletMarker( RenderedWindowResult result, PagePortletTraceInfo info )
    {
        StringBuffer str = new StringBuffer();
        String key = RenderTrace.getCurrentRenderTraceInfo().getKey();
        str.append( "<div id=\"marker-" ).append( key ).append( "-" ).append( info.getKey() ).append( "\">" );
        str.append( result.getContent() );
        str.append( "</div>" );

        result.setContent( str.toString() );
    }

    public static String unwrapResultWithPortletMarker( RenderedWindowResult result )
    {
        String content = result.getContent();
        String contentUnwrapped = StringUtils.substringBetween( content, "<div id=\"marker-", "</div>" );
        if ( contentUnwrapped != null )
        {
            return StringUtils.substringAfter( contentUnwrapped, ">" );
        }
        else
        {
            return content;
        }
    }

}
