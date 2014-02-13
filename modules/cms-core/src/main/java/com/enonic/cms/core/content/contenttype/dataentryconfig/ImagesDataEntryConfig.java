/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.contenttype.dataentryconfig;


public class ImagesDataEntryConfig
    extends AbstractBaseDataEntryConfig
{
    public ImagesDataEntryConfig( String name, boolean required, String displayName, String xpath )
    {
        super( name, required, DataEntryConfigType.IMAGES, displayName, xpath );
    }
}