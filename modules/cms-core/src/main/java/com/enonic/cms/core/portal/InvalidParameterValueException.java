/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import com.enonic.cms.core.BadRequestErrorType;
import com.enonic.cms.core.StacktraceLoggingUnrequired;

public class InvalidParameterValueException
    extends RuntimeException
    implements BadRequestErrorType, StacktraceLoggingUnrequired
{
    private String parameterName;

    private String parameterValue;

    private String message;


    public InvalidParameterValueException( String parameterName, String parameterValue )
    {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
        message = "Parameter '" + parameterName + "' has invalid value: '" + parameterValue + "'";
    }

    public String getParameterName()
    {
        return parameterName;
    }

    public String getParameterValue()
    {
        return parameterValue;
    }

    public String getMessage()
    {
        return message;
    }
}
