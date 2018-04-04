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

import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.api.TenantConfigurationApi;
import com.github.cafdataprocessing.processing.service.client.model.EffectiveTenantConfigValue;
import com.sun.jersey.api.client.ClientHandlerException;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods that are available to be called during Workflow XSLT transformation. These are intended to provide some useful
 * operations at transformation time.
 */
public class TransformerFunctions {
    
    public static final Logger LOG = LoggerFactory.getLogger(TransformerFunctions.class);

    /**
     * Escape characters in the passed value that need to be escaped before being written to JavaScript.
     * @param valueToEscape value that should have characters escaped
     * @return escaped value
     * @throws NullPointerException if {@code valueToEscape} is null
     */
    public static String escapeForJavaScript(final String valueToEscape) {
        Objects.requireNonNull(valueToEscape);
        return StringEscapeUtils.escapeEcmaScript(valueToEscape);
    }

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
        if (workerName == null || workerName.isEmpty()) {
            return null;
        }
        return escapeForJavaScript(getEnvironmentValue(workerName.toLowerCase(Locale.ENGLISH) + ".taskqueue"));
    }

    /**
     * Returns the value of the tenant's configuration that was requested.
     *
     * @param apiClientObject The api client that should be used when contacting the data processing service.
     * @param tenantId A unique string that identifies the tenant.
     * @param key The unique string used to identify a specific configuration.
     * @return The string representation of the value of the configuration requested.
     * @throws ApiException When an error occurs while trying to retrieve a config's value from the processing service.
     * @throws NullPointerException When the tenantId or key passed to the method is null.
     */
    public static String getTenantSpecificConfigValue(final Object apiClientObject, final String tenantId, final String key)
        throws ApiException
    {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(key);
        final ApiClient apiClient = (ApiClient) apiClientObject;
        final TenantConfigurationApi tenantsApi = new TenantConfigurationApi(apiClient);
        try {
            final EffectiveTenantConfigValue effectiveTenantConfigValue = tenantsApi.getEffectiveTenantConfig(tenantId, key);
            LOG.debug("Retrieved value for tenant configuration using key: {}", key);
            LOG.debug("Retrieved value for tenant configuration is of type: {}", effectiveTenantConfigValue.getValueType());
            // escape the config value in case it has characters that would cause issues in JavaScript
            return escapeForJavaScript(effectiveTenantConfigValue.getValue());
        } catch (final ApiException ex) {
            if (ex.getCode() == 404) {
                LOG.error("Unable to obtain tenant configuration from processing service for tenant: {} using key: {} as no configuration "
                    + "could be found match the provided key", tenantId, key);
                throw ex;
            }
            LOG.error("Unable to obtain tenant configuration from processing service for tenant: {} using key: {}", tenantId, key);
            throw ex;
        } catch (final ClientHandlerException ex) {
            /**
             * Wrapping this exception as an ApiException as it was caused by an in ability to contact the processing service. Wrapping it
             * in an ApiException allows for this to be picked up be the calling code and it can then make a decision to retry as the
             * connection problem may have been transient.
             */
            LOG.error("Unable to obtain tenant configuration from processing service for tenant: {} using key: {}", tenantId, key);
            throw new ApiException(500, "Unable to contact processing service to retrieve tenant configs.");
        }
    }
}
