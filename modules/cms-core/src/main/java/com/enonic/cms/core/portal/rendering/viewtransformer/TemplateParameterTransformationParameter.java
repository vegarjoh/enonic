/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering.viewtransformer;

import com.enonic.cms.core.structure.TemplateParameter;

/**
 * May 20, 2009
 */
public class TemplateParameterTransformationParameter
    extends AbstractTransformationParameter
    implements TransformationParameter
{
    private TemplateParameter templateParameter;

    public TemplateParameterTransformationParameter( TemplateParameter templateParameter, TransformationParameterOrigin origin )
    {
        super( templateParameter.getName(), origin );
        this.templateParameter = templateParameter;
    }

    public Object getValue()
    {
        return templateParameter.getValue();
    }
}
