/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.resolver.mock;

import com.enonic.cms.framework.xml.XMLBuilder;
import com.enonic.cms.framework.xml.XMLDocument;

import com.enonic.cms.core.resolver.ResolverHttpRequestInput;
import com.enonic.cms.core.resolver.ResolverHttpRequestInputXMLCreator;

/**
 * Created by rmy - Date: Aug 24, 2009
 */
public class ResolverHttpRequestInputXMLCreatorMock
    extends ResolverHttpRequestInputXMLCreator
{

    public XMLDocument buildResolverInputXML( ResolverHttpRequestInput resolverHttpRequestInput )
    {
        XMLBuilder xmlDoc = new XMLBuilder();

        xmlDoc.startElement( REQUEST_ROOT_ELEMENT_NAME );

        xmlDoc.endElement();

        return xmlDoc.getDocument();
    }

}
