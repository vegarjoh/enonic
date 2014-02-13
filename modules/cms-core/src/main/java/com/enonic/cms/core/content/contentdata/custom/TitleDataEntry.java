/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.contentdata.custom;

/**
 * For a field to be used as a title, it must be a single line text string.  All implementing classes have data that are or can be converted
 * to a single line string, and thus be used as the title of the content.
 */
public interface TitleDataEntry
    extends DataEntry
{
    /**
     * @return Returns the value as a single line string.
     */
    public String getValueAsTitle();
}
