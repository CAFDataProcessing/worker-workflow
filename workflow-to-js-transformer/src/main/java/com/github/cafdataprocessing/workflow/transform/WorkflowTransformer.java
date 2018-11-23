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
package com.github.cafdataprocessing.workflow.transform;

import com.github.cafdataprocessing.processing.service.client.ApiClient;
import com.github.cafdataprocessing.processing.service.client.ApiException;
import com.github.cafdataprocessing.processing.service.client.model.Action;
import com.github.cafdataprocessing.processing.service.client.model.BaseProcessingRule;
import com.github.cafdataprocessing.processing.service.client.model.BaseWorkflow;
import com.github.cafdataprocessing.processing.service.client.model.ExistingCondition;
import com.github.cafdataprocessing.workflow.spec.WorkflowSpec;
import com.github.cafdataprocessing.workflow.transform.exceptions.InvalidWorkflowSpecificationException;
import com.github.cafdataprocessing.workflow.transform.models.FullAction;
import com.github.cafdataprocessing.workflow.transform.models.FullProcessingRule;
import com.github.cafdataprocessing.workflow.transform.models.FullWorkflow;
import com.github.cafdataprocessing.workflow.transform.models.WorkflowSettings;
import com.github.cafdataprocessing.workflow.transform.xstream.GeneralEnumToStringConverter;
import com.github.cafdataprocessing.workflow.transform.xstream.KeyAsElementNameMapConverter;
import com.github.cafdataprocessing.workflow.transform.xstream.TypeAttributeCollectionConverter;
import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * Transforms a processing workflow to a JavaScript representation of its logic that can be executed against a Document Worker document.
 */
public class WorkflowTransformer
{

    private static final Gson GSON;

    static {
        GSON = new Gson();
    }

    private WorkflowTransformer()
    {
    }

    /**
     * Retrieves a Workflow, including its rules, actions and conditions, using provided workflow ID, project ID and processing API url
     * and converts the workflow to a JavaScript logic that a Document can be executed against.
     *
     * @param workflowSpec specification of workflow to generate JavaScript for.
     * @param processingApiUrl Contactable URL for a processing API web service that the workflow can be retrieved from.
     * @return JavaScript representation of the workflow logic.
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow e.g. Invalid
     * requests will result in this exception.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to retrieve the workflow.
     * e.g. The processing service not being contactable.
     * @throws WorkflowTransformerException if there is an error transforming workflow returned to JavaScript representation
     * @throws NullPointerException if the projectId or tenantId passed to the method is null
     */
    public static WorkflowRepresentation retrieveAndTransformWorkflowToJavaScript(final WorkflowSpec workflowSpec,
                                                                                  final String processingApiUrl)
        throws ApiException, WorkflowTransformerException, WorkflowRetrievalException, InvalidWorkflowSpecificationException
    {
        Objects.requireNonNull(workflowSpec.getProjectId());
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(processingApiUrl);
        final String workflowAsXML = retrieveAndTransformWorkflowToXml(workflowSpec, apiClient);
        return transformXmlWorkflowToJavaScript(workflowAsXML, workflowSpec.getProjectId());
    }

    /**
     * Retrieves a Workflow, including its rules, actions and conditions, using provided workflow ID, project ID and processing API url
     * and returns its as an XML representation.
     *
     * @param workflowSpec specification of workflow to generate JavaScript for.
     * @return XML representation of the workflow and its children.
     * @throws ApiException if certain failures occur communicating with the processing service to retrieve the workflow e.g. Invalid
     * requests will result in this exception.
     * @throws WorkflowRetrievalException if certain failures occur communicating with the processing service to retrieve the workflow.
     * e.g. The processing service not being contactable.
     * @throws WorkflowTransformerException if there is an error transforming workflow returned to XML representation
     * @throws NullPointerException if the projectId passed to the method is null
     */
    public static String retrieveAndTransformWorkflowToXml(final WorkflowSpec workflowSpec, final ApiClient apiClient)
        throws ApiException, WorkflowTransformerException, WorkflowRetrievalException, InvalidWorkflowSpecificationException
    {
        Objects.requireNonNull(workflowSpec);
        final FullWorkflowRetriever workflowRetriever = new FullWorkflowRetriever(apiClient);
        final FullWorkflow fullWorkflow = workflowRetriever.getFullWorkflow(workflowSpec);
        return transformFullWorkflowToXml(fullWorkflow);
    }

    /**
     * Transforms the provided workflow to an XML representation.
     *
     * @param fullWorkflow A workflow, including its rules, actions and conditions, to convert to XML.
     * @return XML representation of the workflow and its children.
     * @throws NullPointerException if {@code fullWorkflow} is null
     * @throws WorkflowTransformerException if there is an error transforming workflow to XML representation
     */
    public static String transformFullWorkflowToXml(FullWorkflow fullWorkflow)
        throws NullPointerException, WorkflowTransformerException
    {
        Objects.requireNonNull(fullWorkflow);
        final XStream xstream = new XStream(new XppDriver(new NoNameCoder()));
        xstream.alias("workflow", FullWorkflow.class);
        xstream.alias("processingRule", FullProcessingRule.class);
        xstream.alias("action", FullAction.class);
        xstream.alias("condition", ExistingCondition.class);
        xstream.registerConverter(new KeyAsElementNameMapConverter(xstream.getMapper(), "value", String.class));
        xstream.registerConverter(new GeneralEnumToStringConverter());
        xstream.registerConverter(new TypeAttributeCollectionConverter(xstream.getMapper()));
        xstream.omitField(BaseProcessingRule.class, "enabled");
        xstream.omitField(BaseProcessingRule.class, "description");
        xstream.omitField(Action.class, "description");
        xstream.omitField(BaseWorkflow.class, "description");
        xstream.omitField(BaseWorkflow.class, "notes");
        final String asXml = xstream.toXML(fullWorkflow);
        return asXml;
    }

    /**
     * Converts a workflow in XML form to a JavaScript logic representation that documents can be executed against.
     *
     * @param workflowXml Workflow in XML form. The expected schema maps to the {@link FullWorkflow} class.
     * @param projectId The projectId to use in workflow transformation
     * @return JavaScript representation of the workflow logic.
     * @throws WorkflowTransformerException if there is an error transforming workflow to JavaScript representation
     * @throws NullPointerException if the projectId or tenantId passed to the method is null
     */
    public static WorkflowRepresentation transformXmlWorkflowToJavaScript(
        final String workflowXml,
        final String projectId
    ) throws WorkflowTransformerException
    {
        Objects.requireNonNull(projectId);
        final WorkflowRepresentation workflow = new WorkflowRepresentation();
        workflow.setWorkflowJavascript(transform("Workflow.xslt", workflowXml, projectId));
        workflow.setWorkflowSettings(GSON.fromJson(transform("settings.xslt", workflowXml, projectId), WorkflowSettings.class));
        return workflow;
    }

    private static String transform(final String workflowResourceName, final String workflowXml, final String projectId)
        throws WorkflowTransformerException, TransformerFactoryConfigurationError
    {
        final InputStream defaultXsltStream = WorkflowTransformer.class.getClassLoader().getResourceAsStream(workflowResourceName);
        if (defaultXsltStream == null) {
            throw new WorkflowTransformerException("Unable to find workflow XSLT resource for transform. Resource name: "
                + workflowResourceName);
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final StreamSource xmlInputStream = new StreamSource(new ByteArrayInputStream(workflowXml.getBytes()));
        final StreamResult outputStream = new StreamResult(byteArrayOutputStream);
        final Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(defaultXsltStream));
            transformer.setParameter("projectId", projectId);
        } catch (final TransformerConfigurationException e) {
            throw new WorkflowTransformerException("Failed to create Transformer from XSLT file input.", e);
        }
        try {
            transformer.transform(xmlInputStream, outputStream);
        } catch (final TransformerException e) {
            throw new WorkflowTransformerException("Failed to transform and output workflow XML input.", e);
        }

        return byteArrayOutputStream.toString();
    }
}
