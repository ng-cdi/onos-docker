#! /bin/bash
set -e

ONOS_HOME=${ONOS_HOME:-$(cd $(dirname $0)/.. >/dev/null 2>&1 && pwd)}
KARAF_ARGS=server
SYS_APPS=drivers
ONOS_APPS=${ONOS_APPS:-}

cd $ONOS_HOME

while [ $# -gt 0 ]; do
  case $1 in
    apps-clean)
      find ${ONOS_HOME}/apps -name "active" -exec rm \{\} \;
      ;;
    *)
      KARAF_ARGS+=" $1"
      ;;
  esac
  shift
done

for app in ${SYS_APPS//,/ } ${ONOS_APPS//,/ }; do
    if  [ -d "${ONOS_HOME}/apps/org.onosproject.$app/" ]; then
        touch ${ONOS_HOME}/apps/org.onosproject.$app/active
    elif [ -d "${ONOS_HOME}/apps/$app" ]; then
        touch ${ONOS_HOME}/apps/$app/active
    else
        echo "[WARN] Don't know how to activate $app"
    fi
done

sh ${ONOS_HOME}/apache-karaf-3.0.8/bin/karaf $KARAF_ARGS &

sleep 25
cd ${APP_HOME}

# Deactivate FWD Appliaction
curl -X DELETE --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.fwd/active

# Activate IMR Application
curl -X POST --user onos:rocks http://localhost:8181/onos/v1/applications/org.onosproject.imr/active

# Install IFWD Application
curl -X POST -HContent-Type:application/octet-stream http://localhost:8181/onos/v1/applications?activate=true --user onos:rocks --data-binary @onos-app-ifwd-1.9.0-SNAPSHOT.oar

# Wait to Die
read
