/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.handler.content;

import org.jdom.Document;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.portal.datasource.handler.DataSourceRequest;
import com.enonic.cms.core.portal.datasource.handler.base.ParamsDataSourceHandler;

@Component("ds.GetContentBySectionHandler")
public final class GetContentBySectionHandler
    extends ParamsDataSourceHandler<GetContentBySectionParams>
{
    public GetContentBySectionHandler()
    {
        super( "getContentBySection", GetContentBySectionParams.class );
    }

    @Override
    protected Document handle( final DataSourceRequest req, final GetContentBySectionParams params )
        throws Exception
    {
        return this.dataSourceService.getContentBySection( req, params.menuItemKeys, params.levels, params.query, params.orderBy,
                                                           params.index, params.count, params.includeData, params.childrenLevel,
                                                           params.parentLevel, params.facets ).getAsJDOMDocument();
    }
}
