/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;


public class RelatedContentEntityEqualsTest
    extends AbstractEqualsTest
{
    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        RelatedContentEntity instance = new RelatedContentEntity();
        instance.setKey( new RelatedContentKey( new ContentVersionKey( 1 ), new ContentKey( 1 ) ) );
        return instance;
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        RelatedContentEntity instance1 = new RelatedContentEntity();
        instance1.setKey( new RelatedContentKey( new ContentVersionKey( 1 ), new ContentKey( 2 ) ) );

        RelatedContentEntity instance2 = new RelatedContentEntity();
        instance1.setKey( new RelatedContentKey( new ContentVersionKey( 2 ), new ContentKey( 1 ) ) );

        return new Object[]{instance1, instance2};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        RelatedContentEntity instance = new RelatedContentEntity();
        instance.setKey( new RelatedContentKey( new ContentVersionKey( 1 ), new ContentKey( 1 ) ) );
        return instance;
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        RelatedContentEntity instance = new RelatedContentEntity();
        instance.setKey( new RelatedContentKey( new ContentVersionKey( 1 ), new ContentKey( 1 ) ) );
        return instance;
    }
}
