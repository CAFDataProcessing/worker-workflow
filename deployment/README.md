# deployment

This folder contains example Docker Compose overlay files to stand up an instance of the Workflow Worker docker image. The main service definition is in [docker-compose-workflow-worker.yml](docker-compose-workflow-worker.yml) and debug information to overlay on top of that is in [docker-compose-workflow-worker.debug.yml](docker-compose-workflow-worker.debug.yml).

These compose files expect to be used with other compose definitions that contain RabbitMQ and processing API services and a volume `worker-datastore`.