/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;
import com.enonic.cms.core.structure.menuitem.section.SectionContentKey;

/**
 *
 */
public interface SectionContentDao
    extends EntityDao<SectionContentEntity>
{
    SectionContentEntity findByKey( SectionContentKey key );

    int deleteByContentKey( ContentKey key );

    Integer getCountNamedContentsInSection( MenuItemKey menuItemKey, String contentName );

    long findPublishedContent( ContentKey key );

    long findUnpublishedContent( ContentKey key );
}
