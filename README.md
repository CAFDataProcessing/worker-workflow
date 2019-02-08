# Workflow Worker

## Summary

A worker to generate a workflow of actions to execute in a chain for document workers and begin the sequence. More information on the functioning of the Workflow Worker can be found [here](worker-workflow/README.md).

## Modules

### worker-workflow

This project contains the Java implementation of the Workflow Worker. It can be found in [worker-workflow](worker-workflow).

### worker-workflow-container
This project builds a Docker image that packages the Workflow Worker for deployment. It can be found in [worker-workflow-container](worker-workflow-container).

## Feature Testing
The testing for the Workflow Worker is defined in [testcases](worker-workflow-container/testcases).