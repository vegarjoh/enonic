/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.instruction;

/**
 * Created by rmy - Date: Nov 18, 2009
 */
public interface PostProcessInstructionExecutor
{
    public String execute( PostProcessInstruction instruction, PostProcessInstructionContext context );

}                                  
