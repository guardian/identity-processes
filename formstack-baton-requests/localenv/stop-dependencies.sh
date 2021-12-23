#!/bin/bash

set -ex
cd `dirname $0`

docker-compose -p formstack-baton-requests stop
