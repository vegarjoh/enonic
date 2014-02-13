/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.xslt.functions.portal;

import java.util.Date;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.SequenceType;

import com.enonic.cms.core.xslt.functions.AbstractXsltFunctionCall;

public class DateTimeFunction
    extends AbstractPortalFunction
{
    private final class Call
        extends AbstractXsltFunctionCall
    {
        @Override
        protected Item call( final XPathContext context, final SequenceIterator[] args )
            throws XPathException
        {
            final String value = toSingleString( args[0] );

            final Date date;

            try
            {
                date = getPortalFunctions().dateTime( value );
            }
            catch ( Exception e )
            {
                throw new XPathException( "Cannot parse date: " + value );
            }

            return DateTimeValue.fromJavaDate( date );
        }
    }

    public DateTimeFunction()
    {
        super( "dateTime" );
        setMinimumNumberOfArguments( 1 );
        setMaximumNumberOfArguments( 1 );
        setResultType( SequenceType.ATOMIC_SEQUENCE );
    }

    @Override
    protected AbstractXsltFunctionCall createCall()
    {
        return new Call();
    }
}
