/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.upgrade.task.datasource.method;

import org.jdom.Element;

final class GetSubMenuConverter
    extends DataSourceMethodConverter
{
    public GetSubMenuConverter()
    {
        super( "getSubMenu" );
    }

    @Override
    public Element convert( final String[] params )
    {
        if ( !checkMinMax( params, 2, 4 ) )
        {
            return null;
        }

        return method().params( params, "menuItemKey", "tagItem", "levels" ).build();
    }
}
