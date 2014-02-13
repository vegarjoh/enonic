/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.facet.builder;

import org.elasticsearch.search.facet.histogram.HistogramFacetBuilder;

import com.google.common.base.Strings;

import com.enonic.cms.core.search.facet.FacetQueryException;
import com.enonic.cms.core.search.facet.model.HistogramFacetModel;

public class ElasticsearchHistogramFacetBuilder
    extends AbstractElasticsearchFacetBuilder
{

    final HistogramFacetBuilder build( HistogramFacetModel histogramFacetModel )
    {
        try
        {
            histogramFacetModel.validate();
        }
        catch ( Exception e )
        {
            throw new FacetQueryException( "Error in histogram-facet definition", e );
        }

        HistogramFacetBuilder builder = new HistogramFacetBuilder( histogramFacetModel.getName() );

        setField( histogramFacetModel, builder );

        setInterval( histogramFacetModel, builder );

        return builder;
    }

    private void setInterval( final HistogramFacetModel histogramFacetModel, final HistogramFacetBuilder builder )
    {
        builder.interval( histogramFacetModel.getInterval() );
    }

    protected void setField( final HistogramFacetModel histogramFacetModel, final HistogramFacetBuilder builder )
    {
        final String indexName = histogramFacetModel.getIndex();

        if ( !Strings.isNullOrEmpty( histogramFacetModel.getValueIndex() ) )
        {
            builder.keyField( createNumericFieldName( indexName ) );
            builder.valueField( createNumericFieldName( histogramFacetModel.getValueIndex() ) );

        }
        else
        {
            builder.field( createNumericFieldName( indexName ) );
        }
    }
}
