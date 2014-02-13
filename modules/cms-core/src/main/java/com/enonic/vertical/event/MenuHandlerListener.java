/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.event;

public interface MenuHandlerListener
    extends VerticalEventListener
{

    /**
     * This method should be called whenever a menu is created.
     *
     * @param e The event object that was emitted.
     */
    void createdMenuItem( MenuHandlerEvent e );

    /**
     * This method should be called whenever a menu is updated.
     *
     * @param e The event object that was emitted.
     */
    void updatedMenuItem( MenuHandlerEvent e );

    /**
     * This method should be called whenever a menu is removed.
     *
     * @param e The event object that was emitted.
     */
    void removedMenuItem( MenuHandlerEvent e );
}
