/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.result;

import java.util.Map;

import com.google.common.collect.Maps;

public class TermsFacetResultSet
    extends AbstractFacetResultSet
    implements FacetResultSet
{
    FacetResultType facetResultType = FacetResultType.TERMS;

    private Long total;

    private Long missing;

    private Long other;

    private Map<String, Integer> results = Maps.newLinkedHashMap();

    public Map<String, Integer> getResults()
    {
        return results;
    }

    public void addResult( String term, Integer count )
    {
        results.put( term, count );
    }

    @Override
    public FacetResultType getFacetResultType()
    {
        return facetResultType;
    }

    public Long getTotal()
    {
        return total;
    }

    public void setTotal( final Long total )
    {
        this.total = total;
    }

    public Long getMissing()
    {
        return missing;
    }

    public void setMissing( final Long missing )
    {
        this.missing = missing;
    }

    public Long getOther()
    {
        return other;
    }

    public void setOther( final Long other )
    {
        this.other = other;
    }
}
