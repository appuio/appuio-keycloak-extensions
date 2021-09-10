FROM docker.io/library/busybox

COPY target/*.jar /extensions/

RUN \
  chmod -R ugo+r /extensions

USER 1001:0
