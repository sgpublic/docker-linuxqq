# docker-linuxqq

基于 [jlesage/docker-baseimage-gui](https://github.com/jlesage/docker-baseimage-gui) 构建，感谢大佬伟大的工作。

## 食用方法

```yaml
version: '3.1'
services:
  linuxqq:
    image: mhmzx/docker-linuxqq:v3.2.10_240715
    restart: unless-stopped
    ports:
      - 5800:5800
    volumes:
      - /path/to/config:/home/linuxqq/config
    # mac_address: XX:XX:XX:XX:XX:XX # 指定一个 mac 地址以支持自动登陆（TODO）
```

支持的环境变量请参阅：[jlesage/docker-baseimage-gui#environment-variables](https://github.com/jlesage/docker-baseimage-gui?tab=readme-ov-file#environment-variables)
