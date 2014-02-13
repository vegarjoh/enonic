/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

public class Ticket
{
    private final static String PARAMETER_NAME = "_ticket";

    private final static String PLACEHOLDER = "##ticket##";

    public static String getParameterName()
    {
        return PARAMETER_NAME;
    }

    public static String getPlaceholder()
    {
        return PLACEHOLDER;
    }
}
