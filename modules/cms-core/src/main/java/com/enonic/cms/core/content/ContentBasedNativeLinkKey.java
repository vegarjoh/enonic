/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import com.enonic.cms.core.link.NativeLinkKey;

/**
 * Feb 15, 2010
 */
public interface ContentBasedNativeLinkKey
    extends NativeLinkKey
{
    ContentKey getContentKey();
}
