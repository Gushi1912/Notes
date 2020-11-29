[TOC]

# APISIX笔记

APISIX一个动态、实时、高性能的 API 网关，基于 [Nginx](https://juejin.cn/post/6844904144235413512)网络库和[etcd](https://juejin.cn/post/6844904031186321416)实现， 提供负载均衡、动态上游、灰度发布、服务熔断、身份认证、可观测性等丰富的流量管理功能。

![](..\APISIX\Img\apisix.png)

我们可以把Apache APISIX 当做流量入口，来处理所有的业务数据，包括动态路由、动态上游、动态证书、 A/B 测试、金丝雀发布(灰度发布)、蓝绿部署、限流限速、抵御恶意攻击、监控报警、服务可观测性、服务治理等。

以上这些都是官方文档里的内容。

APISIX还提供了很多功能，这写还需要慢慢去摸索发现使用。

### 一：安装

安装APISIX有很多种方式。比如使用源码编译，使用Docker镜像，使用RPM包。

这里我是用的Centos7系统，我采用RPM包的形式安装。

##### 1.1  安装依赖

因为APISIX是基于Nginx和etcd的，所以需要安装**etcd**和**OpenResty**依赖。

**etcd**是一个key-value形式的数据库，可用户配置共享和服务发现。

**OpenResty** 是一个强大的 Web 应用服务器，Web 开发人员可以使用 Lua 脚本语言调动 Nginx 支持的各种 C 以及 Lua 模块,更主要的是在性能方面，OpenResty可以 快速构造出足以胜任 10K 以上并发连接响应的超高性能 Web 应用系统。

~~~
# 安装 epel, `luarocks` 需要它
wget http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
sudo rpm -ivh epel-release-latest-7.noarch.rpm

# 安装 etcd
wget https://github.com/etcd-io/etcd/releases/download/v3.4.13/etcd-v3.4.13-linux-amd64.tar.gz
tar -xvf etcd-v3.4.13-linux-amd64.tar.gz && \
    cd etcd-v3.4.13-linux-amd64 && \
    sudo cp -a etcd etcdctl /usr/bin/

# 添加 OpenResty 源
sudo yum install yum-utils
sudo yum-config-manager --add-repo https://openresty.org/package/centos/openresty.repo

# 安装 OpenResty 和 编译工具
sudo yum install -y openresty curl git gcc luarocks lua-devel

# 开启 etcd server
nohup etcd &
~~~

##### 1.2 安装APISIX

我这里选择的是使用RPM包安装，系统是Centos7版本

* 安装APISIX

~~~shell
sudo yum install -y https://github.com/apache/apisix/releases/download/2.0/apisix-2.0-0.el7.noarch.rpm
~~~

* 安装完成之后检查APISIX的版本号：

~~~shell
apisix version
~~~

* 启动APISIX

~~~shell
apisix start
~~~

安装的路径在**/usr/local/apisix**，可以看到具体的目录如下：

~~~reStructuredText
drwxr-xr-x 11 root   root 4096 Nov 23 16:34 apisix
drwx------  2 nobody root 4096 Nov 23 16:34 client_body_temp
drwxr-xr-x  3 root   root 4096 Nov 27 17:14 conf
drwxr-xr-x  5 root   root 4096 Nov 23 16:34 deps
drwx------  2 nobody root 4096 Nov 23 16:34 fastcgi_temp
drwxrwxr-x  2 root   root 4096 Nov 27 17:14 logs
-rw-------  1 root   root  734 Nov 27 15:38 nohup.out
drwx------  2 nobody root 4096 Nov 23 16:34 proxy_temp
drwx------  2 nobody root 4096 Nov 23 16:34 scgi_temp
drwx------  2 nobody root 4096 Nov 23 16:34 uwsgi_temp

conf：
-rw-r--r-- 1 root root    36 Nov 23 16:34 apisix.uid
drwxr-xr-x 2 root root  4096 Nov 23 16:34 cert
-rwxr-xr-x 1 root root 10134 Oct 28 09:45 config-default.yaml
-rwxr-xr-x 1 root root  1306 Nov 27 17:14 config.yaml
-rwxr-xr-x 1 root root  4003 Oct 28 09:45 mime.types
-rw-r--r-- 1 root root  9478 Nov 27 17:14 nginx.conf
conf文件夹里是关于apsix的一些配置文件。
~~~

APISIX是启动在9080端口，可以使用curl http://127.0.0.1:9080 来测试服务是否正常启动

~~~shell
$ curl http://127.0.0.1:9080
{"error_msg":"404 Route Not Found"}
~~~



##### 1.3 Dashboard

APISIX提供了一个可视化的操作界面，可以避免使用curl来直接操作。

出于安全考虑，Dashboard默认只允许127.0.0.1访问。可以在config.yaml文件里进行配置。

只需要添加上allow_admin字段，下面配置一些允许访问Dashboard的IP列表就可以了。

~~~shell
apisix:
  admin_key:
    -
      name: "admin"
      key: edd1c9f034335f136f87ad84b625c8f1 # using fixed API token has security risk, please
                                            # update it when you deploy to production environment
      role: admin

  allow_admin:
     - 127.0.0.0/24
     - 192.168.17.0/24
添加的IP为远程访问的主机IP地址
~~~

配置完成之后需要重启 

~~~shell
apisix restart
~~~



### 二、入门

使用APISIX来配置出一个可以对外提供服务的API。

一个微服务可以通过APISIX的路由、服务、上游和插件等多个实体之间的关系进行配置。Route（路由）与客户端请求匹配，并指定让门到达APISIX后如何发送到Upstream（上游，后端API服务）。Service（服务）为上游服务提供了抽象。因此，我们可以在多个Route中引用它[^理解]。

##### 2.1 Upstream

地址：/apisix/admin/upstreams/{id}

> 请求方法同下面的Route

> body请求参数

![](..\APISIX\Img\Upstream1.png)

![](..\APISIX\Img\Upstream2.png)

![](./APISIX/Img/Upstream3.png)



Upstream 是一个虚拟主机抽象，它根据配置规则在给定的一组服务节点上执行负载平衡。 因此，单个上游配置可以由提供相同服务的多个服务器组成。每个节点将包括一个 key（地址/ip:port）和一个 value （节点的权重）。 服务可以通过轮询或一致哈希（cHash）机制进行负载平衡。**Upstream 的地址信息可以直接配置到 `Route`（或 `Service`) 上，当 Upstream 有重复时，就需要用“引用”方式避免重复了**。

我自己写了一个最简单的SpringBoot应用，将其打包部署在服务器上,并且分别启动在不同的端口上。

~~~shell
nohup java -jar jpa-0.0.1-SNAPSHOT.jar --server.port=10800 &
nohup java -jar jpa-0.0.1-SNAPSHOT.jar --server.port=11800 &
~~~

然后通过APISIX Admin API来设置转发到Upstream的请求，并对代理节点进行负载均衡。

执行以下指令的意思就是在APISIX中创建一个id为10的上游信息，并使用round-robin机制进行负载均衡。

~~~shell
[root@izuf6e1jhv5i6fv2ux4x53z ~]# curl "http://127.0.0.1:9080/apisix/admin/upstreams/10" -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PUT -d '
{
    "type": "roundrobin",  #使用什么类型的负载均衡
    "nodes": {
        "127.0.0.1:10800": 1, #代理的节点以及权重
        "127.0.0.1:11800": 4
    }
}'
以下是指令的响应结果：
{"node":{"key":"\/apisix\/upstreams\/10","value":{"hash_on":"vars","id":"10","type":"roundrobin","pass_host":"pass","nodes":{"127.0.0.1:10800":1,"127.0.0.1:11800":4}}},"header":{"cluster_id":"14841639068965178418","raft_term":"4","member_id":"10276657743932975437","revision":"315"},"action":"set"}
~~~

看到**"action":"set"**表示指令执行成功。



##### 2.2 Route

Route通过定义一些路由规则来匹配客户端的请求，然后根据匹配结果加载并执行相应的插件，并把请求转发给指定Upstream。

配置完Upstream之后还要配置相应的Route（路由）信息。路由就是负责客户端到apisix之间的连接，对请求的获取匹配。

具体的配置信息：

~~~shell
[root@izuf6e1jhv5i6fv2ux4x53z ~]# curl "http://127.0.0.1:9080/apisix/admin/routes/5" -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PUT -d '
{
    "uri": "/test1",
    "host": "47.102.204.127",
    "upstream_id": 10
}'
{"node":{"key":"\/apisix\/routes\/5","value":{"uri":"\/test1","upstream_id":10,"host":"47.102.204.127","priority":0,"id":"5"}},"header":{"raft_term":"4","revision":"320","cluster_id":"14841639068965178418","member_id":"10276657743932975437"},"action":"set"}

~~~

之后就可以通过访问APISIX的服务来进行转发访问具体的应用：

![](..\APISIX\Img\Route0.png)

###### 具体的请求方法：

![请求方法](..\APISIX\Img\Route1.png)

**这里的path表示url请求参数中的名字部分**

###### URL请求参数：

![url请求参数](..\APISIX\Img\Route2.png)

###### body请求参数

![body请求参数](..\APISIX\Img\Route3.png "第一部分")

![body请求参数](..\APISIX\Img\Route4.png "第二部分")

![body请求参数](..\APISIX\Img\Route5.png "第三部分")

![body请求参数](..\APISIX\Img\Route6.png "第四部分")

###### 运算符列表

![](..\APISIX\Img\Route7.png)

具体的示例：

~~~shell
# 创建一个路由
$ curl http://127.0.0.1:9080/apisix/admin/routes/1 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PUT -i -d '
{
    "uri": "/index.html",
    "hosts": ["foo.com", "*.bar.com"],
    "remote_addrs": ["127.0.0.0/8"],
    "methods": ["PUT", "GET"],
    "enable_websocket": true,
    "upstream": {
        "type": "roundrobin",
        "nodes": {
            "39.97.63.215:80": 1
        }
    }
}'

HTTP/1.1 201 Created
Date: Sat, 31 Aug 2019 01:17:15 GMT
...

# 创建一个有效期为 60 秒的路由，过期后自动删除
$ curl http://127.0.0.1:9080/apisix/admin/routes/2?ttl=60 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PUT -i -d '
{
    "uri": "/aa/index.html",
    "upstream": {
        "type": "roundrobin",
        "nodes": {
            "39.97.63.215:80": 1
        }
    }
}'

HTTP/1.1 201 Created
Date: Sat, 31 Aug 2019 01:17:15 GMT
...


# 给路由增加一个 upstream node
$ curl http://127.0.0.1:9080/apisix/admin/routes/1 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '
{
    "upstream": {
        "nodes": {
            "39.97.63.216:80": 1
        }
    }
}'
HTTP/1.1 200 OK
...

执行成功后，upstream nodes 将更新为：
{
    "39.97.63.215:80": 1,
    "39.97.63.216:80": 1
}


# 给路由更新一个 upstream node 的权重
$ curl http://127.0.0.1:9080/apisix/admin/routes/1 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '
{
    "upstream": {
        "nodes": {
            "39.97.63.216:80": 10
        }
    }
}'
HTTP/1.1 200 OK
...

执行成功后，upstream nodes 将更新为：
{
    "39.97.63.215:80": 1,
    "39.97.63.216:80": 10
}


# 给路由删除一个 upstream node
$ curl http://127.0.0.1:9080/apisix/admin/routes/1 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '
{
    "upstream": {
        "nodes": {
            "39.97.63.215:80": null
        }
    }
}'
HTTP/1.1 200 OK
...

执行成功后，upstream nodes 将更新为：
{
    "39.97.63.216:80": 10
}


# 替换路由的 methods -- 数组
$ curl http://127.0.0.1:9080/apisix/admin/routes/1 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '{
    "methods": ["GET", "POST"]
}'
HTTP/1.1 200 OK
...

执行成功后，methods 将不保留原来的数据，整个更新为：
["GET", "POST"]


# 替换路由的 upstream nodes -- sub path
$ curl http://127.0.0.1:9080/apisix/admin/routes/1/upstream/nodes -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '
{
    "39.97.63.200:80": 1
}'
HTTP/1.1 200 OK
...

执行成功后，nodes 将不保留原来的数据，整个更新为：
{
    "39.97.63.200:80": 1
}


# 替换路由的 methods  -- sub path
$ curl http://127.0.0.1:9080/apisix/admin/routes/1/methods -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -i -d '["POST", "DELETE", "PATCH"]'
HTTP/1.1 200 OK
...

执行成功后，methods 将不保留原来的数据，整个更新为：
["POST", "DELETE", "PATCH"]
~~~



##### 2.3 Service

对应的URI：/apisix/admin/services/{id}

Service是某类API的抽象（也可以理解为一组Route的抽象）。它通常与上游服务抽象是一一对应的，Route与Service之间通常是N:1的关系。

> 请求方法

![](..\APISIX\Img\Service1.png)

> body请求参数

![](..\APISIX\Img\Service2.png)



##### 2.4 Consumer

地址：/apisix/admin/consumers/{username}

Consumer是某类服务的消费者，需要与用户认证体系配合才能使用。Consumer使用username作为唯一的标识，*只支持使用HTTP的*  **PUT** 方法创建Consumer*

> 请求方法

![](..\APISIX\Img\Consumer1.png)

> body请求参数

![](..\APISIX\Img\Consumer2.png)

绑定认证授权插件有些特别，当它需要与 consumer 联合使用时，需要提供用户名、密码等信息；另一方面，当它与 route/service 绑定时，是不需要任何参数的。因为这时候是根据用户请求数据来反向推出用户对应的是哪个 consumer。

**关于授权**

因为必须要和consumer联合使用所以我们呢首先得创建一个consumer：

~~~shell
[root@izuf6e1jhv5i6fv2ux4x53z ~]# curl http://127.0.0.1:9080/apisix/admin/consumers -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PUT -d '
> {
>     "username": "gugugu",
>     "plugins": {
>         "key-auth": {
>             "key": "superSecretAPIKey"
>         }
>     }
> }'
以下为设置成功的响应：
{"node":{"key":"\/apisix\/consumers\/gugugu","value":{"plugins":{"key-auth":{"key":"superSecretAPIKey"}},"username":"gugugu"}},"header":{"raft_term":"4","revision":"321","cluster_id":"14841639068965178418","member_id":"10276657743932975437"},"action":"set"}

~~~

创建成功之后我们还要在route里添加相应的插件（还没有测试没有添加插件的情况下到底能不能行）。

我是在之前的创建的路由里使用patch来添加新功能。

~~~shell
[root@izuf6e1jhv5i6fv2ux4x53z ~]# curl http://127.0.0.1:9080/apisix/admin/routes/5 -H 'X-API-KEY: edd1c9f034335f136f87ad84b625c8f1' -X PATCH -d '
{
    "plugins": {
        "key-auth": {}
>     }
> }'
{"header":			{"cluster_id":"14841639068965178418","raft_term":"4","member_id":"10276657743932975437","revision":"322"},"responses":[{"response_put":{"header":{"revision":"322"}}}],"succeeded":true,"node":{"value":{"plugins":{"key-auth":{}},"upstream_id":10,"id":"5","priority":0,"host":"47.102.204.127","uri":"\/test1"},"key":"\/apisix\/routes\/5"},"action":"compareAndSwap"}
~~~

以下是测试执行情况，首先是没有使用apikey的情况，当然是会返回错误信息的。添加了apikey之后就能正常访问了。

![](..\APISIX\Img\key-auth.png)

##### 2.5 SSL

地址：/apisix/admin/ssl/{id}

> 请求方法

![](..\APISIX\Img\SSL1.png)

> body请求参数

![](..\APISIX\Img\SSL2.png)



### 三、插件

##### 3.1 常规插件

* **batch-requests**：以http pipeline的方式在网关一次性发起多个http请求（默认启用）。
* **插件热加载**：无序重启服务，完成插件热加载或者卸载（自带功能）。
* **HTTPS/TLS**:根据TLS扩展字段SNI（Server Name Indication）动态加载证书。
* **serverless**：允许在APISIX中的不同阶段动态运行Lua代码。
* **redirect**:URI重定向。



##### 3.2转换类插件

* **response-rewrite**：支持自定义修改返回内容的status code、body、headers。
* **proxy-rewrite**：支持自定义修改proxy到上游的信息。
* **grpc-transcode**：REST <--> gRPC转码
* **fault-injection**：故障注入，可以返回指定的响应体，响应码和响应时间，从而提供了不同失败场景下的

处理的能力，例如服务失败、服务过载、服务高延时等。



##### 3.3 认证类插件

* **authz-keycloak**：支持Keyclock身份认证服务器
* **wolf-rabc**：基于RBAC的用户认证以及授权
* **key-auth**：基于key Authentication的用户认证
* **JWT-token**：基于JWT（JSON Web Tokens）Authentication的用户认证
* **basic-auth**：基于basic auth的用户认证
* **oauth**：提供OAuth2身份认证和自省。
* **openid-connect**



##### 3.4 安全类插件

* cors：为你的API启用CORS
* uri-blocker：根据URI拦截用户请求
* referer-restriction：Referer白名单
* ip-restriction：IP黑白名单



##### 3.5 流量控制类

* **limit-req**：基于漏桶原理的请求限速实现。
* **limit-conn**：限制并发请求（或者并发连接）
* **limit-count**：基于“固定窗口”的限速实现
* **proxy-cache**：代理缓存插件**提供缓存后端响应数据的能力**。
* **request-validation**：请求验证。
* **proxy-mirror**：代理镜像插件提供镜像客户端请求的能力。
* **api-breaker**：API的断路器，在状态不正常的情况下停止将请求转发到上游。



##### 3.6监视器





##### 3.7 日志类插件

* **http-logger**：将请求记录到HTTP服务器
* **tcp-logger**：将请求记录到TCP服务器
* **kafka-logger**：将请求记录到外部Kafka服务器
* **udp-logger**：将请求记录到UDP服务器
* **sys-log**：将请求记录到syslog服务
* **log-rotate**：日志文件定期切分

[^理解]: service其实就是请求通过转发到上游之后，由service进行处理。



