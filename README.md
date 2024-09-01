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
      - ./config:/home/linuxqq/config
      - ./config/baseimage-gui:/config # 上游项目的配置目录
```

支持修改的环境变量：

| 环境变量                        | 描述	                                                     | 默认值（留空则表示必填）                   |
|-----------------------------|---------------------------------------------------------|--------------------------------|
| USER_ID                     | 运行 QQ 所使用的用户 ID                                         | 1000                           |
| GROUP_ID                    | 运行 QQ 所使用的用户组 ID                                        | 1000                           |
| KEEP_APP_RUNNING            | 保持应用运行，当 QQ 意外停止时自动重启，`1` 表示开启                          | 0                              |
| SECURE_CONNECTION           | 为 noVNC 启用 HTTPS 访问                                     | 0                              |
| WEB_LISTENING_PORT          | noVNC 端口                                                | 5800                           |
| WEB_AUTHENTICATION          | 为 noVNC 启用认证保护，需开启 `SECURE_CONNECTION` 才能开启             | 0                              |
| WEB_AUTHENTICATION_USERNAME | noVNC 认证用户名                                             | （若启用 `WEB_AUTHENTICATION` 则必填） |
| WEB_AUTHENTICATION_PASSWORD | noVNC 认证密码                                              | （若启用 `WEB_AUTHENTICATION` 则必填） |
| XDG_CONFIG_HOME             | 配置文件目录，包括 QQ 产生的文件                                      | /home/linuxqq/config           |
| QQ_HOME                     | QQ 安装目录，若想使用外部 QQ 安装实例可修改此环境变量或直接挂载 /opt/QQ             | /opt/QQ                        |

其他环境变量请参阅：[jlesage/docker-baseimage-gui#environment-variables](https://github.com/jlesage/docker-baseimage-gui?tab=readme-ov-file#environment-variables)

### 网页路径前缀

由于上游项目的大佬貌似更倾向于在外部反向代理里间接实现前缀，因此此需求实现方案参考如下：

/etc/nginx/conf.d/linuxqq.conf

```
server {
    server_name example.com;
    listen 80;
    listen [::]:80;

    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header X-Download-Options noopen;
    add_header X-Permitted-Cross-Domain-Policies none;

    location = /your/prefix {
        return 302 http://$server_name/your/prefix/;
    }
    location ~ /your/prefix/ {
        include /etc/nginx/common.d/listen_location.conf;
        rewrite ^/your/prefix(.*)$ $1 break;
        proxy_redirect http://$server_name/ /your/prefix/;
        proxy_pass https://$NAS_IP:5800;
    }
}
```
