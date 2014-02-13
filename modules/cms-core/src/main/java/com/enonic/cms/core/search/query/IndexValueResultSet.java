/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.search.query;

/**
 * This interface defines the index value result set.
 */
public interface IndexValueResultSet
{
    /**
     * Return the count.
     */
    public int getCount();

    /**
     * Return from index.
     */
    public int getFromIndex();

    /**
     * Return total count.
     */
    public int getTotalCount();

    /**
     * Return the result.
     */
    public IndexValueResult getIndexValue( int num );
}
