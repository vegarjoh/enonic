/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.link;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.binary.BinaryDataKey;

public interface ContentKeyResolver
{
    ContentKey resolvFromBinaryKey( BinaryDataKey binaryDataKey );
}
