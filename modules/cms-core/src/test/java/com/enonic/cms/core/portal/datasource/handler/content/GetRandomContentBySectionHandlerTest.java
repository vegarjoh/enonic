/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler.content;

import org.junit.Test;
import org.mockito.Mockito;

import com.enonic.cms.core.portal.datasource.handler.AbstractDataSourceHandlerTest;
import com.enonic.cms.core.portal.datasource.handler.DataSourceRequest;
import com.enonic.cms.core.portal.datasource.handler.StoreMatcher;
import com.enonic.cms.core.preview.PreviewContext;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

public class GetRandomContentBySectionHandlerTest
    extends AbstractDataSourceHandlerTest<GetRandomContentBySectionHandler>
{
    public GetRandomContentBySectionHandlerTest()
    {
        super( GetRandomContentBySectionHandler.class );
    }

    @Override
    protected void initTest()
        throws Exception
    {
        this.handler.setDataSourceService( this.dataSourceService );

        this.request.addParam( "menuItemKeys", "10,20,30" );
        this.request.addParam( "query", "title STARTS WITH 'Content '" );
        this.request.addParam( "levels", "2" );
        this.request.addParam( "count", "10" );
        this.request.addParam( "includeData", "true" );
        this.request.addParam( "childrenLevel", "1" );
        this.request.addParam( "parentLevel", "0" );
    }

    @Test
    public void testHandler_get_content_by_query()
        throws Exception
    {
        Mockito.when(
            this.dataSourceService.getRandomContentBySection(
                argThat( new StoreMatcher<DataSourceRequest>() {
                    protected void store( DataSourceRequest value ) {
                        assertEquals( request.getUser(), value.getUser() );
                        assertEquals( PreviewContext.NO_PREVIEW, value.getPreviewContext() );
                    }
                } ),
                eq( new int[] { 10, 20, 30 } ),
                eq( 2 ),
                eq( "title STARTS WITH 'Content '" ),
                eq( 10 ),
                eq( true ),
                eq( 1 ),
                eq( 0 ) ) )
            .thenReturn( this.dummyDoc );

        this.handler.handle( this.request );

        Mockito.verify( this.dataSourceService, Mockito.times( 1 ) ).getRandomContentBySection(
            eq( request ),
            eq( new int[] { 10, 20, 30 } ),
            eq( 2 ),
            eq( "title STARTS WITH 'Content '" ),
            eq( 10 ),
            eq( true ),
            eq( 1 ),
            eq( 0 ) );
    }
}
