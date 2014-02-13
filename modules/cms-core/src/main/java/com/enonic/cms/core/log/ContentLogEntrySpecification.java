/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.log;


public class ContentLogEntrySpecification
    extends LogEntrySpecification
{

    private boolean allowDeletedContent = false;


    public void setAllowDeletedContent( boolean value )
    {
        this.allowDeletedContent = value;

    }

    public boolean isAllowDeletedContent()
    {
        return allowDeletedContent;
    }


}