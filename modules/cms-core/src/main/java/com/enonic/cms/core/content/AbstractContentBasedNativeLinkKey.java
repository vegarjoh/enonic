/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import com.enonic.cms.core.link.AbstractNativeLinkKey;

/**
 * Feb 15, 2010
 */
public abstract class AbstractContentBasedNativeLinkKey
    extends AbstractNativeLinkKey
    implements ContentBasedNativeLinkKey

{
    private ContentKey contentKey = null;

    protected AbstractContentBasedNativeLinkKey( ContentKey contentKey )
    {
        this.contentKey = contentKey;
    }

    protected AbstractContentBasedNativeLinkKey()
    {

    }

    public ContentKey getContentKey()
    {
        return contentKey;
    }


    public void setContentKey( ContentKey contentKey )
    {
        this.contentKey = contentKey;
    }
}
