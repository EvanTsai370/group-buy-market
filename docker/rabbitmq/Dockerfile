# 1. 选材：基于官方的 RabbitMQ 3.13 版本，带 management 意味着自带网页管理界面
FROM rabbitmq:3.13-management

# 2. 准备工具：为了下载插件，我们需要先安装 curl 工具
# (apt-get update ... 是 Linux 安装软件的标准命令)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 3. 加料：下载 "延迟消息插件" (delayed_message_exchange)
# 把插件文件下载到 RabbitMQ 的插件目录里
RUN curl -L -o /opt/rabbitmq/plugins/rabbitmq_delayed_message_exchange-3.13.0.ez https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.13.0/rabbitmq_delayed_message_exchange-3.13.0.ez

# 4. 激活：运行命令启用这个插件
RUN rabbitmq-plugins enable rabbitmq_delayed_message_exchange