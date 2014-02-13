/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import org.springframework.stereotype.Repository;

import com.enonic.cms.core.security.RememberedLoginEntity;
import com.enonic.cms.core.security.RememberedLoginKey;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SiteKey;

@Repository("rememberedLoginDao")
public final class RememberedLoginEntityDao
    extends AbstractBaseEntityDao<RememberedLoginEntity>
    implements RememberedLoginDao
{

    public RememberedLoginEntity findByKey( RememberedLoginKey key )
    {
        return get( RememberedLoginEntity.class, key );
    }

    public RememberedLoginEntity findByGuidAndSite( String guid, SiteKey siteKey )
    {
        return findSingleByNamedQuery( RememberedLoginEntity.class, "RememberedLoginEntity.findByGuidAndSite",
                                       new String[]{"guid", "siteKey"}, new Object[]{guid, siteKey} );
    }

    public RememberedLoginEntity findByUserKeyAndSiteKey( UserKey userKey, SiteKey siteKey )
    {
        return findSingleByNamedQuery( RememberedLoginEntity.class, "RememberedLoginEntity.findByUserKeyAndSiteKey",
                                       new String[]{"userKey", "siteKey"}, new Object[]{userKey, siteKey} );
    }

    @Override
    public void removeUsage( UserKey user )
    {
        for ( RememberedLoginEntity rememberedLogin : findByNamedQuery( RememberedLoginEntity.class, "RememberedLoginEntity.findByUser",
                                                                        "userKey", user ) )
        {
            delete( rememberedLogin );
        }
    }
}