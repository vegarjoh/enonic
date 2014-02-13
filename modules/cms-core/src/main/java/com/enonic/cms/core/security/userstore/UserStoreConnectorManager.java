/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.userstore;

import java.util.Map;

import com.enonic.cms.core.security.userstore.connector.UserStoreConnector;
import com.enonic.cms.core.security.userstore.connector.config.UserStoreConnectorConfig;
import com.enonic.cms.core.security.userstore.connector.remote.RemoteUserStoreConnector;

public interface UserStoreConnectorManager
{
    UserStoreConnector getUserStoreConnector( final UserStoreKey userStoreKey );

    RemoteUserStoreConnector getRemoteUserStoreConnector( final UserStoreKey userStoreKey );

    Map<String, UserStoreConnectorConfig> getUserStoreConnectorConfigs();

    UserStoreConnectorConfig getUserStoreConnectorConfig( final String configName );

    UserStoreConnectorConfig getUserStoreConnectorConfig( final UserStoreKey userStoreKey );

    void invalidateCachedConfig( final UserStoreKey userStoreKey );
}
