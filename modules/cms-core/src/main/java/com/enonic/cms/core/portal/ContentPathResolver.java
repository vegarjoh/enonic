/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.content.ContentKey;

public class ContentPathResolver
{
    private final static Pattern CONTENT_PATH_PATTERN = Pattern.compile( "^(.*/+)?(.*)--(\\d+)(/_window/.*)?$" );

    private final static Pattern OLD_STYLE_CONTENT_PATH_PATTERN = Pattern.compile( "[0-9]+\\.cms(/_window/.*)?$" );

    public static ContentPath resolveContentPath( final Path localPath )
    {
        String localPathWithoutFragment = localPath.getPathWithoutFragmentAsString();

        Matcher matcher = CONTENT_PATH_PATTERN.matcher( localPathWithoutFragment );

        if ( !matcher.matches() )
        {
            return tryResolveOldTypeContentPath( localPath );
        }

        final Path menuItemPath = resolveMenuItemPath( localPath );

        String contentTitle = matcher.group( 2 );
        ContentKey contentKey = new ContentKey( matcher.group( 3 ) );

        return new ContentPath( contentKey, contentTitle, menuItemPath );
    }

    private static ContentPath tryResolveOldTypeContentPath( final Path localPath )
    {
        Matcher cmsMatcher = OLD_STYLE_CONTENT_PATH_PATTERN.matcher( localPath.getPathAsString() );

        if ( !cmsMatcher.find() )
        {
            return null;
        }

        final Path menuItemPath = resolveMenuItemPath( localPath );

        String contentTitleAndKey = resolveContentTitleAndKeyPart( localPath );

        int dotPosition = contentTitleAndKey.lastIndexOf( '.' );
        ContentKey contentKey = resolveContentKey( contentTitleAndKey, dotPosition );

        String contentTitle = resolveContentTitle( contentTitleAndKey, dotPosition );

        if ( contentKey == null )
        {
            return null;
        }

        ContentPath oldStyleContentPath = new ContentPath( contentKey, contentTitle, menuItemPath );
        oldStyleContentPath.setOldStyleContentPath( true );

        return oldStyleContentPath;
    }

    private static Path resolveMenuItemPath( final Path localPath )
    {
        final int numberOfPathElements = localPath.getPathElementsCount();
        if ( numberOfPathElements == 1 )
        {
            return Path.ROOT;
        }

        int index = localPath.getPathAsString().contains( "/" + WindowReference.WINDOW_PATH_PREFIX + "/" ) ? 3 : 1;
        return new Path( "/" + localPath.subPath( 0, numberOfPathElements - index ) );
    }


    private static String resolveContentTitleAndKeyPart( final Path localPath )
    {
        String cmsPathElement = localPath.getPathElement( localPath.getPathElementsCount() - 1 );
        return cmsPathElement.substring( 0, cmsPathElement.lastIndexOf( '.' ) );
    }

    private static String resolveContentTitle( String contentNameAndKey, int dotPosition )
    {
        if ( dotPosition < 0 )
        {
            return null;
        }
        return contentNameAndKey.substring( 0, dotPosition );
    }

    private static ContentKey resolveContentKey( String contentNameAndKey, int dotPosition )
    {
        String contentKeyString;

        if ( dotPosition < 0 )
        {
            contentKeyString = contentNameAndKey;
        }
        else
        {
            contentKeyString = contentNameAndKey.substring( dotPosition + 1 );
        }

        return StringUtils.isNumeric( contentKeyString ) ? new ContentKey( contentKeyString ) : null;
    }

}
