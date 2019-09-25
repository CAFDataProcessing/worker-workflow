/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.workflow.exceptions;

import java.util.List;

/**
 * Exception to be thrown when there is an unexpected setting in CAF_WORKFLOW_SETTINGS 
 * during processing of a document with an existing CAF_WORKFLOW_SETTINGS field.
 */
public class UnexpectedCafWorkflowSettingException extends Exception
{
    /**
     * Create a new UnexpectedCafWorkflowSettingException
     *
     * @param unexpectedSetting the unexpected setting
     * @param validSettings a list of valid settings
     */
    public UnexpectedCafWorkflowSettingException(final String unexpectedSetting, final List<String> validSettings)
    {
        super(String.format(
            "Document contains an unexpected setting inside the CAF_WORKFLOW_SETTINGS field: %s. "
            + "Valid settings are: %s", unexpectedSetting, String.join(", ", validSettings)));
    }
    
    /**
     * Create a new UnexpectedCafWorkflowSettingException
     *
     * @param message information about this exception
     */
    public UnexpectedCafWorkflowSettingException(final String message)
    {
        super(message);
    }

    /**
     * Create a new UnexpectedCafWorkflowSettingException
     *
     * @param message information about this exception
     * @param cause the original cause of this exception
     */
    public UnexpectedCafWorkflowSettingException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
