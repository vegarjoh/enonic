/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.index.queryexpression;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.enonic.cms.core.content.index.ContentIndexConstants;

/**
 * This class implements the field expression.
 */
public final class FieldExpr
    implements Expression
{
    private static final Set<String> DATE_FIELDS = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );

    private static final Set<String> INTEGER_FIELDS = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );

    static
    {
        DATE_FIELDS.addAll( Arrays.asList( ContentIndexConstants.DATE_FIELDS ) );
        INTEGER_FIELDS.addAll( Arrays.asList( ContentIndexConstants.INTEGER_FIELDS ) );
    }

    /**
     * Field expression.
     */
    private final String path;

    /**
     * Construct the field.
     */
    public FieldExpr( String path )
    {
        this.path = path;
    }

    /**
     * Return the field.
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Return the expression as string.
     */
    public String toString()
    {
        return this.path;
    }

    public boolean isDateField()
    {
        return DATE_FIELDS.contains( path );
    }

    public boolean isIntegerField()
    {
        return INTEGER_FIELDS.contains( path );
    }

    public boolean isContentType()
    {
        return ContentIndexConstants.F_CONTENT_TYPE_NAME.equalsIgnoreCase( path );
    }

    /**
     * Evaluate the expression.
     */
    public Object evaluate( QueryEvaluator evaluator )
    {
        return evaluator.evaluate( this );
    }
}
