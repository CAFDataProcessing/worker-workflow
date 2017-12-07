/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.github.cafdataprocessing.workflow.transform;

import java.util.Locale;

/**
 * Methods that are available to be called during Workflow XSLT transformation. These are intended to provide some useful
 * operations at transformation time.
 */
public class TransformerFunctions {
    /**
     * Checks system environment and system properties for the specified property name returning the value if it is found
     * or null if it is not. If both value is set for both environment and system property then the system property will
     * be returned.
     * @param name Name of property to retrieve value for.
     * @return Value matching the specified property name or null if no matching name is found.
     */
    public static String getEnvironmentValue(final String name)
    {
        return System.getProperty(name, System.getenv(name) != null ? System.getenv(name) : "");
    }

    /**
     * Returns value for worker queue set in the environment based on provided worker name. Returns null if no match
     * found in environment.
     * @param workerName Identifies the worker queue is for. System environment and property values will be checked using
     *                   this value.
     * @return Queue value associated with specified worker name or null if no match found.
     */
    public static String getWorkerQueueFromEnvironment(final String workerName){
        if(workerName==null || workerName.isEmpty()){
            return null;
        }
        return getEnvironmentValue(workerName.toLowerCase(Locale.ENGLISH) + ".taskqueue");
    }
}
