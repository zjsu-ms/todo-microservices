#!/bin/bash

echo "=== 测试微服务拆分 ==="

echo -e "\n1. 测试用户服务 - 创建用户"
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"张三","email":"zhangsan@example.com"}')
echo $USER_RESPONSE

# 提取用户ID
USER_ID=$(echo $USER_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "创建的用户ID: $USER_ID"

echo -e "\n2. 测试用户服务 - 获取所有用户"
curl -s http://localhost:8081/api/users | jq '.'

echo -e "\n3. 测试Todo服务 - 创建Todo（关联到存在的用户）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"学习微服务\",\"description\":\"完成拆分实践\",\"userId\":$USER_ID}" | jq '.'

echo -e "\n4. 测试Todo服务 - 获取所有Todo"
curl -s http://localhost:8082/api/todos | jq '.'

echo -e "\n5. 测试服务间通信 - 创建Todo（用户不存在，应该失败）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","userId":999}' | jq '.'

echo -e "\n=== 测试完成 ==="
