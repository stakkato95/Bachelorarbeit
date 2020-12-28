#!/bin/bash

curl -X GET "localhost:9000/cluster/state"
curl -X POST "localhost:9000/cluster/item" -H "Content-Type: application/json" -d '{ "value": "b" }'