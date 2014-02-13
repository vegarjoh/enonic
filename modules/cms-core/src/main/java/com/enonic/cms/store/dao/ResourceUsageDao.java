/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.HashMap;

import com.google.common.collect.Multimap;

import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.resource.ResourceReferencer;

public interface ResourceUsageDao
{
    public HashMap<ResourceKey, Long> getUsageCountMap();

    public Multimap<ResourceKey, ResourceReferencer> getUsedBy( ResourceKey resourceKey );

    public void updateResourceReference( ResourceKey oldResourceKey, ResourceKey newResourceKey );

    public void updateResourceReferencePrefix( String oldPrefix, String newPrefix );
}
