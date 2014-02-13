/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.portal.ClientError;

public class ContentNameMismatchClientError
    extends ClientError
{
    private ContentKey contentKey;

    private String requestedContentName;

    public ContentNameMismatchClientError( int statusCode, String message, Throwable cause, ContentKey contentKey,
                                           String requestedContentName )
    {
        super( statusCode, message, cause );
        this.contentKey = contentKey;
        this.requestedContentName = requestedContentName;
    }

    public ContentKey getContentKey()
    {
        return contentKey;
    }

    public String getRequestedContentName()
    {
        return requestedContentName;
    }
}
