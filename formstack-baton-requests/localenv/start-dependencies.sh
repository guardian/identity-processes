#!/bin/bash

set -ex
cd `dirname $0`

./stop-dependencies.sh
docker-compose -p formstack-baton-requests up --force-recreate -d
