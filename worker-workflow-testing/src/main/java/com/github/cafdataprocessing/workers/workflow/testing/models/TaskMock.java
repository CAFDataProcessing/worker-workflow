/*
 * Copyright 2017-2026 Open Text.
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
package com.github.cafdataprocessing.workers.workflow.testing.models;

import com.github.cafdataprocessing.workers.document.model.Application;
import com.github.cafdataprocessing.workers.document.model.Document;
import com.github.cafdataprocessing.workers.document.model.Response;
import com.github.cafdataprocessing.workers.document.model.Scripts;
import com.github.cafdataprocessing.workers.document.model.Task;
import com.github.workerframework.api.WorkerTaskData;

import java.util.Map;

public class TaskMock implements Task
{

    private final Map<String, String> customData;
    private Document document;
    private final Scripts scripts;
    private final WorkerTaskData service;
    private final Response response;
    private final Application application;
    private final String correlationId;

    public TaskMock(final Map<String, String> customData, final Document document, final Scripts scripts,
                    final WorkerTaskData service, final Response response, final Application application,
                    final String correlationId)
    {
        this.customData = customData;
        this.document = document;
        this.scripts = scripts;
        this.service = service;
        this.response = response;
        this.application = application;
        this.correlationId = correlationId;
    }

    @Override
    public String getCustomData(final String dataKey)
    {
        return this.customData.get(dataKey);
    }

    @Override
    public Document getDocument()
    {
        return document;
    }

    @Override
    public Scripts getScripts()
    {
        return scripts;
    }

    @Override
    public <S> S getService(final Class<S> service)
    {
        return (S) this.service;
    }

    @Override
    public Response getResponse()
    {
        return response;
    }

    @Override
    public Application getApplication()
    {
        return application;
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public void setDocument(final Document document)
    {
        this.document = document;
    }

}
