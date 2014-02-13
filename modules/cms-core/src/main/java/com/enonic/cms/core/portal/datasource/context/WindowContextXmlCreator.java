/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.datasource.context;

import java.util.Map;

import org.jdom.Element;

import com.enonic.cms.core.structure.TemplateParameter;
import com.enonic.cms.core.structure.page.Window;
import com.enonic.cms.core.structure.portlet.PortletEntity;

/**
 * May 15, 2009
 */
final class WindowContextXmlCreator
{

    public Element createPortletWindowElement( Window window, boolean isRenderedInline, Element portletDocumentEl )
    {
        PortletEntity portlet = window.getPortlet();

        Element windowEl = new Element( "window" );
        windowEl.setAttribute( "key", window.getKey().toString() );
        windowEl.setAttribute( "is-rendered-inline", Boolean.toString( isRenderedInline ) );
        windowEl.setAttribute( "region", window.getRegion().getName() );
        windowEl.addContent( new Element( "name" ).setText( portlet.getName() ) );
        windowEl.addContent( createPortletElement( portlet, portletDocumentEl ) );

        return windowEl;
    }

    private Element createPortletElement( PortletEntity portlet, Element portletDocumentEl )
    {
        Element portletEl = new Element( "portlet" );

        portletEl.setAttribute( "key", portlet.getPortletKey().toString() );
        portletEl.addContent( new Element( "name" ).setText( portlet.getName() ) );

        Map<String, TemplateParameter> templateParameters = portlet.getTemplateParameters();
        Element parametersEl = new Element( "parameters" );
        for ( TemplateParameter templateParameter : templateParameters.values() )
        {
            Element parameterEl = new Element( "parameter" );

            Element nameEl = new Element( "name" );
            Element valueEl = new Element( "value" );

            nameEl.setText( templateParameter.getName() );
            valueEl.setText( templateParameter.getValue() );

            parameterEl.addContent( nameEl );
            parameterEl.addContent( valueEl );

            parametersEl.addContent( parameterEl );
        }
        portletEl.addContent( parametersEl );

        if ( portletDocumentEl != null )
        {
            portletEl.addContent( portletDocumentEl );
        }

        return portletEl;
    }

}