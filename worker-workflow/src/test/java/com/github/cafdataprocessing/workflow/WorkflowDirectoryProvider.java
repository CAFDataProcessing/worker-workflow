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
package com.github.cafdataprocessing.workflow;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class WorkflowDirectoryProvider {
    public static String getWorkflowDirectory(final String resourceDirectoryName) throws URISyntaxException {
        final URL resource = WorkflowDirectoryProvider.class.getClassLoader().getResource(resourceDirectoryName);
        return Paths.get(resource.toURI()).toString();
    }
}
