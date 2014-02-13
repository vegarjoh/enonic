/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler;

import java.net.URL;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.mockito.Mockito;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.preview.PreviewContext;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.portal.datasource.service.DataSourceService;

import static org.junit.Assert.*;

public abstract class AbstractDataSourceHandlerTest<T extends DataSourceHandler>
{
    private final Class<T> type;

    protected T handler;

    protected DataSourceRequest request;

    protected DataSourceService dataSourceService;

    protected XMLDocument dummyDoc;

    public AbstractDataSourceHandlerTest( final Class<T> type )
    {
        this.type = type;
    }

    @Before
    public final void startUp()
        throws Exception
    {
        this.handler = this.type.newInstance();
        this.request = new DataSourceRequest();
        this.request.setName( this.handler.getName() );
        final UserEntity user = new UserEntity();
        user.setKey( new UserKey( User.ANONYMOUS_UID ) );
        user.setName( User.ANONYMOUS_UID );
        user.setDisplayName( User.ANONYMOUS_UID );
        this.request.setUser( user );
        this.request.setPreviewContext( PreviewContext.NO_PREVIEW );

        this.dummyDoc = XMLDocumentFactory.create( "<dummy/>" );
        this.dataSourceService = Mockito.mock( DataSourceService.class );

        initTest();
    }

    protected abstract void initTest()
        throws Exception;

    protected final void testHandle( final String resultName )
        throws Exception
    {
        final Document actualDoc = this.handler.handle( this.request );
        assertNotNull( actualDoc );

        final Document expectedDoc = readDoc( resultName + ".xml" );
        assertXml( expectedDoc, actualDoc );
    }

    private void assertXml( final Document expected, final Document actual )
        throws Exception
    {
        final String expectedStr = toString( expected );
        final String actualStr = toString( actual );
        assertEquals( actualStr, expectedStr );
    }

    private Document readDoc( final String name )
        throws Exception
    {
        final URL url = getClass().getResource( name );
        assertNotNull( "Document [" + name + "] not found", url );

        final SAXBuilder builder = new SAXBuilder();
        return builder.build( url );
    }

    private String toString( final Document doc )
        throws Exception
    {
        final XMLOutputter out = new XMLOutputter();
        out.setFormat( Format.getPrettyFormat() );
        return out.outputString( doc );
    }
}
