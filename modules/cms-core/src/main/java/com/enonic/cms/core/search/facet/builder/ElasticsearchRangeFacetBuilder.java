/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.facet.builder;

import org.elasticsearch.search.facet.range.RangeFacetBuilder;

import com.google.common.base.Strings;

import com.enonic.cms.core.search.facet.FacetQueryException;
import com.enonic.cms.core.search.facet.model.FacetRange;
import com.enonic.cms.core.search.facet.model.FacetRangeValue;
import com.enonic.cms.core.search.facet.model.RangeFacetModel;

public class ElasticsearchRangeFacetBuilder
    extends AbstractElasticsearchFacetBuilder
{
    final RangeFacetBuilder build( RangeFacetModel rangeFacetModel )
    {
        try
        {
            rangeFacetModel.validate();
        }
        catch ( Exception e )
        {
            throw new FacetQueryException( "Error in range-facet definition", e );
        }

        RangeFacetBuilder builder = new RangeFacetBuilder( rangeFacetModel.getName() );

        setField( rangeFacetModel, builder );

        setRanges( rangeFacetModel, builder );

        return builder;
    }

    private void setRanges( final RangeFacetModel rangeFacetModel, final RangeFacetBuilder builder )
    {

        for ( final FacetRange facetRange : rangeFacetModel.getRanges() )
        {
            final FacetRangeValue fromRangeValue = facetRange.getFromRangeValue();
            final FacetRangeValue toRangeValue = facetRange.getToRangeValue();
            addRange( builder, fromRangeValue, toRangeValue );
        }
    }

    private void addRange( final RangeFacetBuilder builder, final FacetRangeValue fromRangeValue, final FacetRangeValue toRangeValue )
    {
        String from = fromRangeValue != null ? fromRangeValue.getStringValue() : null;
        String to = toRangeValue != null ? toRangeValue.getStringValue() : null;

        builder.addRange( from, to );
    }

    protected void setField( final RangeFacetModel rangeFacetModel, final RangeFacetBuilder builder )
    {
        final String indexName = rangeFacetModel.getIndex();

        if ( !Strings.isNullOrEmpty( rangeFacetModel.getValueIndex() ) )
        {
            if ( rangeFacetModel.isNumericRanges() )
            {
                builder.keyField( createNumericFieldName( indexName ) );
                builder.valueField( createNumericFieldName( rangeFacetModel.getValueIndex() ) );
            }
            else
            {
                builder.keyField( createDateFieldName( indexName ) );
                builder.valueField( createDateFieldName( rangeFacetModel.getValueIndex() ) );
            }
        }
        else
        {
            if ( rangeFacetModel.isNumericRanges() )
            {
                builder.field( createNumericFieldName( rangeFacetModel.getIndex() ) );
            }
            else
            {
                builder.field( createDateFieldName( rangeFacetModel.getIndex() ) );
            }
        }
    }
}
