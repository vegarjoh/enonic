/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.xslt.XsltProcessorErrors;
import com.enonic.cms.core.xslt.XsltProcessorException;

/**
 * Apr 28, 2009
 */
public class PortletXsltViewTransformationException
    extends RuntimeException
{
    public PortletXsltViewTransformationException( String message, XsltProcessorException exception )
    {
        super( message, exception );
    }
}