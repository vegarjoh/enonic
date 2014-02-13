/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.log;

public enum LogEventType
{
    LOGIN,
    LOGIN_USERSTORE,
    LOGIN_FAILED,
    LOGOUT,
    ENTITY_CREATED,
    ENTITY_UPDATED,
    ENTITY_REMOVED,
    ENTITY_OPENED
}
