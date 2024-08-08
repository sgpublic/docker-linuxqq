#!/bin/bash

set -e
set -u

sed -i '/# add by docker-linuxqq/d' /opt/base/etc/nginx/nginx.conf
sed -i '/# add by docker-linuxqq/d' /etc/cont-init.d/10-nginx.sh
rm -rf /var/tmp/nginx-proxy/*

if [ -z "$WEB_PATH_PREFIX" ]; then
  printf "WEB_PATH_PREFIX is empty, skip apply."
  exit 0
fi

mkdir -p /var/tmp/nginx-proxy

PROXY_CONF=/var/tmp/nginx-proxy/proxy.conf
PROXY_PORT=${WEB_LISTENING_PORT:-5800}
WEB_LISTENING_PORT=6010

echo "WEB_PATH_PREFIX set to '$WEB_PATH_PREFIX'."
sed -i "/# Generate the listen directive for HTTP access./a WEB_LISTENING_PORT=$WEB_LISTENING_PORT # add by docker-linuxqq" /etc/cont-init.d/10-nginx.sh

# Determine if secure port is used.
if is-bool-val-true "${SECURE_CONNECTION:-0}"; then
  LISTEN_SSL="ssl"
  PROXY_SCHEME="https"
else
  LISTEN_SSL=
  PROXY_SCHEME="http"
fi

# Add the listen directive for IPv6.
if ifconfig -a | grep -wq inet6; then
  LISTEN_V6="listen [::]:$PROXY_PORT $LISTEN_SSL default_server;"
else
  LISTEN_V6=
fi

sed -i "/default_site.conf/a include $PROXY_CONF; # add by docker-linuxqq" /opt/base/etc/nginx/nginx.conf

cat <<EOF > $PROXY_CONF
server {
  listen $PROXY_PORT $LISTEN_SSL default_server;
  $LISTEN_V6

  include /var/tmp/nginx/ssl[.]conf;

  location = $WEB_PATH_PREFIX {
    return 302 $PROXY_SCHEME://\$host$WEB_PATH_PREFIX/;
  }
  location ~ $WEB_PATH_PREFIX/ {
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$remote_addr;
    proxy_set_header X-Forwarded-Proto \$scheme;
    proxy_set_header X-Forwarded-Protocol \$scheme;
    proxy_set_header X-Forwarded-Host \$http_host;
    proxy_set_header X-NginX-Proxy true;
    proxy_set_header Upgrade \$http_upgrade;
    proxy_set_header Connection "Upgrade";
    proxy_buffering off;
    rewrite ^$WEB_PATH_PREFIX(.*)\$ \$1 break;
    proxy_redirect https://\$host/ $WEB_PATH_PREFIX/;
    proxy_pass $PROXY_SCHEME://127.0.0.1:$WEB_LISTENING_PORT;
  }
}

EOF
