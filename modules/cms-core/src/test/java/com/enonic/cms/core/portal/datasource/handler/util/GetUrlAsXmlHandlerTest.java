/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler.util;

import org.junit.Test;
import org.mockito.Mockito;

import com.enonic.cms.core.http.HTTPService;
import com.enonic.cms.core.portal.datasource.DataSourceException;
import com.enonic.cms.core.portal.datasource.handler.AbstractDataSourceHandlerTest;

public class GetUrlAsXmlHandlerTest
    extends AbstractDataSourceHandlerTest<GetUrlAsXmlHandler>
{
    private HTTPService httpService;

    public GetUrlAsXmlHandlerTest()
    {
        super( GetUrlAsXmlHandler.class );
    }

    @Override
    protected void initTest()
        throws Exception
    {
        this.httpService = Mockito.mock( HTTPService.class );
        Mockito.when( this.httpService.getURLAsBytes( Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt() ) ).thenReturn(
            "<dummy/>".getBytes() );
        this.handler.setHttpService( this.httpService );
    }

    @Test(expected = DataSourceException.class)
    public void testUrlNotSet()
        throws Exception
    {
        this.handler.handle( this.request );
    }

    @Test
    public void testDefaultParams()
        throws Exception
    {
        this.request.addParam( "url", "http://www.enonic.com" );
        testHandle( "getUrlAsXml_result" );
        Mockito.verify( this.httpService, Mockito.times( 1 ) ).getURLAsBytes( "http://www.enonic.com", 5000, -1 );
    }

    @Test
    public void testSetParams()
        throws Exception
    {
        this.request.addParam( "url", "http://www.enonic.com" );
        this.request.addParam( "timeout", "1000" );
        this.request.addParam( "readTimeout", "1000" );
        testHandle( "getUrlAsXml_result" );
        Mockito.verify( this.httpService, Mockito.times( 1 ) ).getURLAsBytes( "http://www.enonic.com", 1000, 1000 );
    }

    @Test(expected = DataSourceException.class)
    public void testIllegalTimeout()
        throws Exception
    {
        this.request.addParam( "url", "http://www.enonic.com" );
        this.request.addParam( "timeout", "abc" );
        this.handler.handle( this.request );
    }

    @Test
    public void testUrlYieldsNull()
        throws Exception
    {
        Mockito.when( this.httpService.getURLAsBytes( Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt() ) ).thenReturn( null );

        this.request.addParam( "url", "http://www.enonic.com" );
        testHandle( "getUrlAsXmlUrlYieldsNull_result" );
        Mockito.verify( this.httpService, Mockito.times( 1 ) ).getURLAsBytes( "http://www.enonic.com", 5000, -1 );
    }


}
