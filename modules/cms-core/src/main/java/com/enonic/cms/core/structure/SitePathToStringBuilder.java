/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.structure;

import com.enonic.cms.core.Path;
import com.enonic.cms.core.RequestParameters;
import com.enonic.cms.core.RequestParametersToStringBuilder;

public class SitePathToStringBuilder
{
    private boolean htmlEscapeParameterAmps = false;

    public String toString( SitePath sitePath )
    {
        StringBuffer s = new StringBuffer();
        s.append( "/" );
        s.append( sitePath.getSiteKey() );
        Path lp = sitePath.getLocalPath();
        if ( lp.isRelative() )
        {
            s.append( "/" );
        }
        Path localPath = sitePath.getLocalPath();
        s.append( localPath.getPathWithoutFragmentAsString() );
        if ( sitePath.hasParams() )
        {
            s.append( "?" ).append( paramsToString( sitePath.getRequestParameters() ) );
        }
        if ( localPath.hasFragment() )
        {
            s.append( "#" ).append( localPath.getFragment() );
        }

        return s.toString();
    }

    private String paramsToString( RequestParameters params )
    {
        RequestParametersToStringBuilder rptoStringBuilder = new RequestParametersToStringBuilder();
        rptoStringBuilder.setHtmlEscapeParameterAmps( htmlEscapeParameterAmps );
        return rptoStringBuilder.toString( params );
    }

    public void setHtmlEscapeParameterAmps( boolean htmlEscapeParameterAmps )
    {
        this.htmlEscapeParameterAmps = htmlEscapeParameterAmps;
    }
}
