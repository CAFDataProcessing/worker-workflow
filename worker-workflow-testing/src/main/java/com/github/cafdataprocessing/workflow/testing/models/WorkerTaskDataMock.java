/*
 * Copyright 2017-2021 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.workflow.testing.models;

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskSourceInfo;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;

public class WorkerTaskDataMock implements WorkerTaskData
{
    private final String classifier;
    private final int version;
    private final TaskStatus status;
    private final byte[] data;
    private final byte[] context;
    private final TrackingInfo trackingInfo;
    private final String to;
    private final TaskSourceInfo taskSourceInfo;

    public WorkerTaskDataMock(final String classifier, final int version, final TaskStatus status, final byte[] data,
                              final byte[] context, final TrackingInfo trackingInfo, final String to, final TaskSourceInfo taskSourceInfo)
    {
        this.classifier = classifier;
        this.version = version;
        this.status = status;
        this.data = data;
        this.context = context;
        this.trackingInfo = trackingInfo;
        this.to = to;
        this.taskSourceInfo = taskSourceInfo;
    }

    @Override
    public String getClassifier()
    {
        return classifier;
    }

    @Override
    public int getVersion()
    {
        return version;
    }

    @Override
    public TaskStatus getStatus()
    {
        return status;
    }

    @Override
    public byte[] getData()
    {
        return data;
    }

    @Override
    public byte[] getContext()
    {
        return context;
    }

    @Override
    public String getCorrelationId() {
        return null;
    }

    @Override
    public TrackingInfo getTrackingInfo()
    {
        return trackingInfo;
    }

    @Override
    public String getTo()
    {
        return to;
    }

    @Override
    public TaskSourceInfo getSourceInfo()
    {
        return taskSourceInfo;
    }

    @Override
    public void addResponse(final WorkerResponse response, final boolean includeTaskContext)
    {
    }

    @Override
    public void sendMessage(final TaskMessage tm)
    {
    }

}
