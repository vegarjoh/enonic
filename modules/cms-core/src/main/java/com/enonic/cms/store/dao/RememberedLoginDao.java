/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import com.enonic.cms.core.security.RememberedLoginEntity;
import com.enonic.cms.core.security.RememberedLoginKey;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SiteKey;


public interface RememberedLoginDao
    extends EntityDao<RememberedLoginEntity>
{
    RememberedLoginEntity findByKey( RememberedLoginKey key );

    RememberedLoginEntity findByGuidAndSite( String guid, SiteKey siteKey );

    RememberedLoginEntity findByUserKeyAndSiteKey( UserKey userKey, SiteKey siteKey );

    void removeUsage( UserKey user );

}