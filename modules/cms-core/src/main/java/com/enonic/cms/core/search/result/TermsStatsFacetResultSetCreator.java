/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.result;

import java.util.List;

import org.elasticsearch.search.facet.termsstats.TermsStatsFacet;

public class TermsStatsFacetResultSetCreator
    extends AbstractFacetResultSetCreator
{
    protected FacetResultSet create( final String facetName, final TermsStatsFacet facet )
    {
        TermsStatsFacetResultSet termsStatsFacetResultSet = new TermsStatsFacetResultSet();

        termsStatsFacetResultSet.setName( facetName );
        termsStatsFacetResultSet.setMissing( facet.getMissingCount() );

        final List<? extends TermsStatsFacet.Entry> entries = facet.getEntries();
        for ( TermsStatsFacet.Entry entry : entries )
        {
            TermsStatsFacetResultEntry termsStatsFacetResultEntry = new TermsStatsFacetResultEntry();

            termsStatsFacetResultEntry.setCount( entry.getCount() );
            termsStatsFacetResultEntry.setMax( getValueIfNumber( entry.getMax() ) );
            termsStatsFacetResultEntry.setMean( getValueIfNumber( entry.getMean() ) );
            termsStatsFacetResultEntry.setMin( getValueIfNumber( entry.getMin() ) );
            termsStatsFacetResultEntry.setTotal( getValueIfNumber( entry.getTotal() ) );
            // termsStatsFacetResultEntry.setTotalCount( entry.getTotalCount() );
            termsStatsFacetResultEntry.setTerm( entry.getTerm().toString() );

            termsStatsFacetResultSet.addResult( termsStatsFacetResultEntry );
        }

        return termsStatsFacetResultSet;
    }
}
