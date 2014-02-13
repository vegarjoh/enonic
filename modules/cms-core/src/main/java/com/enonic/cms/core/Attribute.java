/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

/**
 * These are the attributes available on the ServletRequest on a site request.
 */
public class Attribute
{
    public static final String ORIGINAL_URL = "com.enonic.render.url.original";

    public static final String ORIGINAL_SITEPATH = "com.enonic.render.sitePath.original";

    public static final String BASEPATH_OVERRIDE_ATTRIBUTE_NAME = "com.enonic.render.basePathOverride";

    public static final String PREVIEW_ENABLED = "com.enonic.render.previewEnabled";

    public static final String PROCESSING_EXCEPTION_COUNT = "com.enonic.render.processingExceptionCount";

}
