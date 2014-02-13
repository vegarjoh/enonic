/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.binary;

import com.enonic.cms.core.content.ContentKey;


public class AttachmentNativeLinkKeyWithLabel
    extends AttachmentNativeLinkKey
{
    private String label = "source";

    public AttachmentNativeLinkKeyWithLabel( ContentKey contentKey, String label )
    {
        super( contentKey );
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public String asUrlRepresentation()
    {
        String key = getContentKey() + "/label/" + getLabel();
        if ( getExtension() != null )
        {
            return key + "." + getExtension();
        }
        return key;
    }

}
