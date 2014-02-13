/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.enonic.cms.core.content.binary.ContentBinaryDataEntity;

@Repository("contentBinaryDataDao")
public final class ContentBinaryDataEntityDao
    extends AbstractBaseEntityDao<ContentBinaryDataEntity>
    implements ContentBinaryDataDao
{

    @Autowired
    private BinaryDataDao binaryDataDao;


    public ContentBinaryDataEntity findByBinaryKey( Integer binaryKey )
    {
        return findFirstByNamedQuery( ContentBinaryDataEntity.class, "ContentBinaryDataEntity.findByBinaryKey", "key", binaryKey );
    }

    public List<ContentBinaryDataEntity> findAllByBinaryKey( Integer binaryKey )
    {
        return findByNamedQuery( ContentBinaryDataEntity.class, "ContentBinaryDataEntity.findByBinaryKey", "key", binaryKey );
    }

}
