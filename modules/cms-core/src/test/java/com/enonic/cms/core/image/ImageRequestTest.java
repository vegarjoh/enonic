/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.image;

import org.junit.Assert;
import org.junit.Test;

public class ImageRequestTest
{
    @Test
    public void testFormatOverride()
    {
        ImageRequest req = new ImageRequest();
        req.setFormat( "jpeg" );
        Assert.assertEquals( "jpeg", req.getFormat() );

        req.getParams().setFormat( "png" );
        Assert.assertEquals( "png", req.getFormat() );
    }
}
