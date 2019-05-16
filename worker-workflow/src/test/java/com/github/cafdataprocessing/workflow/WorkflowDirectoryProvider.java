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
