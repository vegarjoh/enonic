/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.tools.cluster;

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.monitor.jvm.JvmInfo;

public final class ClusterNodeWrapper
    implements Comparable<ClusterNodeWrapper>
{
    private final NodeInfo info;

    private boolean isMaster = false;

    @Override
    public int compareTo( final ClusterNodeWrapper o )
    {
        return this.getName().compareTo( o.getName() );
    }

    public ClusterNodeWrapper( final NodeInfo info, final boolean isMaster )
    {
        this.info = info;
        this.isMaster = isMaster;
    }

    public String getName()
    {
        return this.info.getNode().getName();
    }

    public boolean getIsMaster()
    {
        return isMaster;
    }

    public String getHostName()
    {
        return this.info.getHostname();
    }

    public String getTransportAddress()
    {
        return this.info.getNode().getAddress().toString();
    }

    public String getJvmVersion()
    {
        final JvmInfo jvm = this.info.getJvm();
        return "Java " + jvm.getVersion() + " (" + jvm.getVmVendor() + ")";
    }

    public String getJvmDirectMemoryMax()
    {
        return this.info.getJvm().getMem().getDirectMemoryMax().toString();
    }

    public String getJvmHeapInit()
    {
        return this.info.getJvm().getMem().getHeapInit().toString();
    }

    public String getJvmNonHeapInit()
    {
        return this.info.getJvm().getMem().getNonHeapMax().toString();
    }

    public String getJvmHeapMax()
    {
        return this.info.getJvm().getMem().getHeapMax().toString();
    }

    public String getJvmNonHeapMax()
    {
        return this.info.getJvm().getMem().getNonHeapMax().toString();
    }
}
