/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import com.enonic.cms.store.dao.SectionContentDao;

public class ContentPublishedResolver
{
    private SectionContentDao sectionContentDao;

    public ContentPublishedResolver()
    {
    }

    public ContentPublishedResolver( SectionContentDao sectionContentDao )
    {
        this.sectionContentDao = sectionContentDao;
    }

    /**
     * Compute if the content has been added to one (or more) sections, and if it is published or not in these sections.
     * @param content observed content
     * @return
     * - 'published' when content is in one or more sections and published in at least one of them
     * - 'unpublished' when content is in one or more sections, but not published in any of them
     * - 'none' when the content is not in any section
     */
    public String computePublishingInSection( final ContentEntity content )
    {
        if ( sectionContentDao != null )
        {
            long count = sectionContentDao.findPublishedContent( content.getKey() );

            if ( count > 0 )
            {
                return "published";
            }

            count = sectionContentDao.findUnpublishedContent( content.getKey() );

            return ( count > 0 ) ? "unpublished" : "none";
        }

        return "";
    }
}
