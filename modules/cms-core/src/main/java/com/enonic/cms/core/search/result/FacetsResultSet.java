/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search.result;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

public class FacetsResultSet
    implements Iterable<FacetResultSet>
{
    Set<FacetResultSet> facetResultSets = Sets.newLinkedHashSet();

    @Override
    public Iterator<FacetResultSet> iterator()
    {
        return facetResultSets.iterator();
    }

    public void addFacetResultSet( FacetResultSet facetResultSet )
    {
        if ( facetResultSets == null )
        {
            this.facetResultSets = Sets.newLinkedHashSet();
        }

        this.facetResultSets.add( facetResultSet );
    }
}
