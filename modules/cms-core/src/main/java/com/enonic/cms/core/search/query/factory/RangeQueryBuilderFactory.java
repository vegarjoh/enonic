/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.query.factory;

import org.elasticsearch.index.query.QueryBuilder;
import org.joda.time.DateTime;

import com.enonic.cms.core.search.query.QueryField;
import com.enonic.cms.core.search.query.QueryValue;

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

public class RangeQueryBuilderFactory
    extends BaseQueryBuilderFactory
{

    public QueryBuilder buildRangeQuery( final QueryField queryField, final QueryValue lower, final QueryValue upper,
                                         final boolean lowerInclusive, final boolean upperInclusive )
    {
        final boolean isNumericComparison = ( lower != null && lower.isNumeric() ) || ( upper != null && upper.isNumeric() );

        final boolean isDateComparison =
            !isNumericComparison && ( ( lower != null && lower.isDateTime() ) || ( upper != null && upper.isDateTime() ) );

        final boolean doStringComparison = !( isNumericComparison || isDateComparison );

        if ( doStringComparison )
        {
            return buildRangeQueryString( queryField, lower, upper, lowerInclusive, upperInclusive );
        }
        else if ( isNumericComparison )
        {
            Number lowerNumeric = lower != null ? lower.getNumericValue() : null;
            Number upperNumeric = upper != null ? upper.getNumericValue() : null;

            return buildRangeQueryNumeric( queryField, lowerNumeric, upperNumeric, lowerInclusive, upperInclusive );
        }
        else
        {
            DateTime lowerDateTime = lower != null ? lower.getDateTime().toDateTime() : null;
            DateTime upperDateTime = upper != null ? upper.getDateTime().toDateTime() : null;

            return buildRangeQueryDateTime( queryField, lowerDateTime, upperDateTime, lowerInclusive, upperInclusive );
        }
    }

    private QueryBuilder buildRangeQueryDateTime( final QueryField queryField, final DateTime lowerDateTime, final DateTime upperDateTime,
                                                  final boolean lowerInclusive, final boolean upperInclusive )
    {
        if ( lowerDateTime == null && upperDateTime == null )
        {
            throw new IllegalArgumentException( "Invalid lower and upper - values in range query" );
        }

        final String fieldName = queryField.isWildcardQueryField() ? ALL_USERDATA_FIELDNAME_DATE : queryField.getFieldNameForDateQueries();

        return rangeQuery( fieldName ).
            from( lowerDateTime ).
            to( upperDateTime ).
            includeLower( lowerInclusive ).
            includeUpper( upperInclusive );
    }

    private QueryBuilder buildRangeQueryNumeric( final QueryField queryField, final Number lowerNumeric, final Number upperNumeric,
                                                 final boolean lowerInclusive, final boolean upperInclusive )
    {
        if ( lowerNumeric == null && upperNumeric == null )
        {
            throw new IllegalArgumentException( "Invalid lower and upper - values in range query" );
        }

        final String queryName =
            queryField.isWildcardQueryField() ? ALL_USERDATA_FIELDNAME_NUMBER : queryField.getFieldNameForNumericQueries();
        return rangeQuery( queryName ).
            from( lowerNumeric ).
            to( upperNumeric ).
            includeLower( lowerInclusive ).
            includeUpper( upperInclusive );
    }

    private QueryBuilder buildRangeQueryString( final QueryField queryField, final QueryValue lower, final QueryValue upper,
                                                final boolean lowerInclusive, final boolean upperInclusive )
    {
        if ( lower == null && upper == null )
        {
            throw new IllegalArgumentException( "Invalid lower and upper - values in range query" );
        }
        final String queryName = queryField.isWildcardQueryField() ? ALL_USERDATA_FIELDNAME : queryField.getFieldName();
        return rangeQuery( queryName ).
            from( lower != null ? lower.getStringValueNormalized() : null ).
            to( upper != null ? upper.getStringValueNormalized() : null ).
            includeLower( lowerInclusive ).
            includeUpper( upperInclusive );
    }

}
