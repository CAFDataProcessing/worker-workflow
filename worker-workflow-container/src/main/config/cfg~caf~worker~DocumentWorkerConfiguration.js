/*
 * Copyright 2017-2024 Open Text.
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
({
    workerName: "worker-workflow",
    workerVersion: "${project.version}",
    outputQueue: getenv("CAF_WORKER_OUTPUT_QUEUE")
            || (getenv("CAF_WORKER_BASE_QUEUE_NAME") || getenv("CAF_WORKER_NAME") || "worker") + "-out",
    failureQueue: getenv("CAF_WORKER_FAILURE_QUEUE") || undefined,
    threads: getenv("CAF_WORKFLOW_WORKER_THREADS") || getenv("CAF_WORKER_THREADS") || 1,
    inputMessageProcessing: {
        documentTasksAccepted: true,
        fieldEnrichmentTasksAccepted: false,
        processSubdocumentsSeparately: false
    }
});
