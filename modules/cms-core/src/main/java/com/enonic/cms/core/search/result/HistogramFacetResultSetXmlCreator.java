/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.result;

import java.util.Set;

import org.jdom.Element;

public class HistogramFacetResultSetXmlCreator
    extends AbstractFacetResultXmlCreator
{

    public Element create( HistogramFacetResultSet histogramFacetResultSet )
    {
        final Element rangeFacetRootElement = createFacetRootElement( "histogram", histogramFacetResultSet );

        final Set<HistogramFacetResultEntry> resultEntries = histogramFacetResultSet.getResultEntries();

        for ( HistogramFacetResultEntry result : resultEntries )
        {
            Element resultEl = new Element( "interval" );
            addAttributeIfNotNull( resultEl, "sum", result.getTotal() );
            addAttributeIfNotNull( resultEl, "total-count", result.getTotalCount() );
            addAttributeIfNotNull( resultEl, "hits", result.getCount() );
            addAttributeIfNotNull( resultEl, "min", result.getMin() );
            addAttributeIfNotNull( resultEl, "mean", result.getMean() );
            addAttributeIfNotNull( resultEl, "max", result.getMax() );

            resultEl.addContent( result.getKey() + "" );

            rangeFacetRootElement.addContent( resultEl );
        }

        return rangeFacetRootElement;
    }


}
