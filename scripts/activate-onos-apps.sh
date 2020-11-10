#! /bin/bash
set -e

## Active the apps required for the NG-CDI intents demo

sh ${SCRIPTS_DIR}/wait-for-onos.sh

curl --silent -X POST --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.openflow/active > /dev/null
curl --silent -X DELETE --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.fwd/active > /dev/null
curl --silent -X POST --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.imr/active > /dev/null

# Install new apps
cd ${APP_HOME}
curl --silent -X POST -HContent-Type:application/octet-stream http://localhost:8181/onos/v1/applications?activate=true --user onos:rocks --data-binary @onos-app-ifwd-1.9.0-SNAPSHOT.oar > /dev/null

echo "ONOS Apps Installed"

# Print App ID for IMR
imr_id=$(curl --silent -X GET --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.imr | jq '.id')
echo "IMR APP ID: "${imr_id}
