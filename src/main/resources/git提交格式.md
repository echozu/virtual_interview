
---

### 标准提交格式
```
<type>(<scope>): <subject>  // 标题行（必填）
<空行>
<body>                     // 详细说明（可选）
<空行>
<footer>                   // 底部信息（可选）
```

### 类型说明（type）：
- `feat`      : 新功能
- `fix`       : bug修复
- `docs`      : 文档更新
- `style`     : 代码样式/格式
- `refactor`  : 重构（非功能修改）
- `test`      : 测试相关
- `chore`     : 构建/工具变更
- `perf`      : 性能优化

---

### 多场景示例

1. **新功能开发**
```
feat(auth): 添加微信登录支持

- 实现微信OAuth2.0接入
- 新增用户绑定接口
- 补充第三方登录文档

BREAKING CHANGE: 需要更新.env配置文件
Resolves: #89
```

2. **Bug修复**
```
fix(api): 修复分页参数失效问题

当pageSize>100时返回错误的问题修正
原因为SQL注入检查误判

Closes: #123
```

3. **文档更新**
```
docs(readme): 更新项目快速入门指南

- 添加Docker部署说明
- 修正错别字
- 补充环境要求章节
```

4. **代码重构**
```
refactor(database): 优化ORM查询逻辑

将重复的查询方法抽象为BaseRepository
移除deprecated的findByRawSQL()

Related: #45
```

5. **样式调整**
```
style(components): 统一按钮CSS命名

所有按钮类名按BEM规范重命名：
.btn-confirm → .btn--confirm
.btn-cancel → .btn--cancel
```

6. **测试用例**
```
test(user): 添加个人资料编辑测试

- 覆盖昵称修改场景
- 测试头像上传边界值
- 添加并发请求测试
```

7. **依赖更新**
```
chore(deps): 升级axios到v2.0.0

更新所有相关import语句
移除已弃用的interceptors配置
```

---

### 最佳实践建议：
1. 标题行不超过72字符
2. 使用祈使语气（如"添加"而非"添加了"）
3. 重要变更标注`BREAKING CHANGE:`
4. 用`Resolves/Fixes/Closes`关联issue
5. 多行内容时保持每行不超过100字符
6. 涉及数据库变更时注明migration要求
