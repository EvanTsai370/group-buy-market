#!/bin/bash

# 设置使用 Java 21 运行 Maven 测试的脚本

echo "=== 检查可用的 Java 版本 ==="
/usr/libexec/java_home -V

echo ""
echo "=== 设置 JAVA_HOME 为 Java 21 ==="
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
echo "JAVA_HOME: $JAVA_HOME"

echo ""
echo "=== 验证 Java 版本 ==="
java -version

echo ""
echo "=== 运行测试 ==="
mvn test -Dtest=TradeOrderServiceIdempotencyTest -pl my-group-by-market-application

echo ""
echo "=== 测试完成 ==="
