/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.login;

import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SiteKey;

/**
 * Jul 10, 2009
 */
public interface LoginService
{
    String rememberLogin( UserKey userKey, SiteKey siteKey, boolean resetGUID );

    UserKey getRememberedLogin( String guid, SiteKey sitekey );

}
