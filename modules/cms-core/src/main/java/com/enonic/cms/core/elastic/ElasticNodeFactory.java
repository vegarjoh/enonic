/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.elastic;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.search.NodeSettingsBuilder;


@Component
public final class ElasticNodeFactory
    implements FactoryBean<Node>
{
    private Node node;

    private NodeSettingsBuilder nodeSettingsBuilder;

    public Node getObject()
    {
        return this.node;
    }

    public Class<?> getObjectType()
    {
        return Node.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    @PostConstruct
    public void start()
    {
        ESLoggerFactory.setDefaultFactory( new Slf4jESLoggerFactory() );

        final Settings settings = nodeSettingsBuilder.buildNodeSettings();

        this.node = NodeBuilder.nodeBuilder().settings( settings ).build();

        this.node.start();
    }

    @Autowired
    public void setNodeSettingsBuilder( final NodeSettingsBuilder nodeSettingsBuilder )
    {
        this.nodeSettingsBuilder = nodeSettingsBuilder;
    }

    @PreDestroy
    public void stop()
    {
        this.node.close();
    }
}
