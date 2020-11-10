#! /bin/bash

## Wait until a response can be aquired from the ONOS API

response=1

while [ $response -ne 0 ]; do
  sleep 1
  curl --silent -f --user onos:rocks http://localhost:8181/onos/v1/devices > /dev/null
  response=$?
done

sleep 5
echo "ONOS API Active"
