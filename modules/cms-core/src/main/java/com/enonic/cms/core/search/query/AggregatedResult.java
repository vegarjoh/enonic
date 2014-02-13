/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.search.query;

/**
 */
public interface AggregatedResult
{
    /**
     * Return the count.
     */
    public int getCount();

    /**
     * Return min value.
     */
    public double getMinValue();

    /**
     * Return max value.
     */
    public double getMaxValue();

    /**
     * Return average value.
     */
    public double getAverageValue();

    /**
     * Return sum value.
     */
    public double getSumValue();
}
