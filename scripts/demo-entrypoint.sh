#! /bin/bash
set -e

ONOS_HOME=${ONOS_HOME:-$(cd $(dirname $0)/.. >/dev/null 2>&1 && pwd)}

cd $ONOS_HOME

sh ${SCRIPTS_DIR}/activate-onos-apps.sh &
${ONOS_HOME}/bin/onos-service server
