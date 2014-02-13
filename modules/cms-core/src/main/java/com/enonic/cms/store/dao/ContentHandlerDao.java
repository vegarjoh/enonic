/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import com.enonic.cms.core.content.contenttype.ContentHandlerEntity;
import com.enonic.cms.core.content.contenttype.ContentHandlerKey;


public interface ContentHandlerDao
    extends EntityDao<ContentHandlerEntity>
{
    ContentHandlerEntity findByKey( ContentHandlerKey key );

    List<ContentHandlerEntity> findAll();
}