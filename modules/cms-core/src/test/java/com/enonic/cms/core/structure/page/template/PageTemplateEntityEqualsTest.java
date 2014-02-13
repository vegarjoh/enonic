/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure.page.template;

import org.junit.Test;

import com.enonic.cms.core.AbstractEqualsTest;

/**
 * Aug 26, 2010
 */
public class PageTemplateEntityEqualsTest
    extends AbstractEqualsTest
{

    @Test
    public void testEquals()
    {
        assertEqualsContract();
    }

    public Object getObjectX()
    {
        return createPageTemplate( 1 );
    }

    public Object[] getObjectsThatNotEqualsX()
    {
        return new Object[]{createPageTemplate( 2 )};
    }

    public Object getObjectThatEqualsXButNotTheSame()
    {
        return createPageTemplate( 1 );
    }

    public Object getObjectThatEqualsXButNotTheSame2()
    {
        return createPageTemplate( 1 );
    }

    private PageTemplateEntity createPageTemplate( int key )
    {
        PageTemplateEntity pageTemplateEntity = new PageTemplateEntity();
        pageTemplateEntity.setKey( key );
        return pageTemplateEntity;
    }
}