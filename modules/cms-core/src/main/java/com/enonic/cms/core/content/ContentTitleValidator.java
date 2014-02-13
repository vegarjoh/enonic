/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.TitleDataEntryNotFoundException;


public class ContentTitleValidator
{

    public static final int CONTENT_TITLE_MAX_LENGTH = 255;

    public static void validate( ContentData contentData )
        throws IllegalArgumentException
    {
        String contentTitle = getContentTitle( contentData );
        if ( ( contentTitle != null ) && ( contentTitle.length() > CONTENT_TITLE_MAX_LENGTH ) )
        {
            throw new IllegalArgumentException(
                "Content title is too long: " + contentTitle.length() + " . Maximum length is " + CONTENT_TITLE_MAX_LENGTH +
                    " characters." );
        }
    }

    private static String getContentTitle( ContentData contentData )
    {
        if ( contentData == null )
        {
            return null;
        }
        try
        {
            return contentData.getTitle();
        }
        catch ( TitleDataEntryNotFoundException e )
        {
            return null;
        }
    }

}
