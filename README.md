# AI数据可视化平台
 `作者 CaoCao 😄`
## 项目介绍 📢
本项目是基于React+Spring Boot+RabbitMQ+AIGC的智能BI数据分析平台。 

访问地址：http://www.huahuaguagua.top  

随着AIGC的发展，越来越多的领域可以引入人工智能来帮助我们实现一些任务。于是本项目应运而生。不同于传统的数据分析平台,当我们分析数据趋势时需人工导入数据，选择要分析的字段和图表，并由专业数据分析师进行分析。然而，本项目只需导入原始数据和你想要分析的目标, 系统将利用AI自动生成可视化图表和详细的分析结论，使得分析数据更加轻松。  
## 项目架构图 🔥 
### 基础架构
基础架构：客户端输入分析诉求和原始数据，向业务后端发送请求。业务后端利用AI服务处理客户端数据，保持到数据库，并生成图表。处理后的数据由业务后端发送给AI服务，AI服务生成结果并返回给后端，最终将结果返回给客户端展示。

### 优化项目架构-异步化处理
优化流程（异步化）：客户端输入分析诉求和原始数据，向业务后端发送请求。业务后端将请求事件放入消息队列，并为客户端生成取餐号，让要生成图表的客户端去排队，消息队列根据I服务负载情况，定期检查进度，如果AI服务还能处理更多的图表生成请求，就向任务处理模块发送消息。

任务处理模块调用AI服务处理客户端数据，AI 服务异步生成结果返回给后端并保存到数据库，当后端的AI工服务生成完毕后，可以通过向前端发送通知的方式，或者通过业务后端监控数据库中图表生成服务的状态，来确定生成结果是否可用。若生成结果可用，前端即可获取并处理相应的数据，最终将结果返回给客户端展示。在此期间，用户可以去做自己的事情。

## 项目功能 🎊  
### 已有功能
1. 用户登录、注册、注销。
2. 智能分析（同步）: 根据用户上传的 Excel 表格，分析诉求，图标名称，图标类型调用 AIGC 将分析结果可视化展示并且给出相关结论(为提升用户体验已关闭此功能，优化为异步调用)  
3. 智能分析（异步）: 用户提交 Excel 表格，分析诉求，图标名称，图标类型后，系统自动提交给后台处理，随后在我的图表页面进行显示可视化和分析结论。
### TODO 新增功能规划
1. 新增AI对话功能
2. 增加死信队列提升系统稳定性
## 主要工作🎊 : 
1. 后端自定义 Prompt 预设模版并封装用户输入的数据和分析诉求,通过对接AIGC接口智能生成可视化图
表json配置和分析结论,返回给前端渲染。
2. 由于AIGC的输入 Token 限制,使用 Easy Excel 解析用户上传的 XLSX 表格数据文件并压缩为 CSV , 实
测提高了将近20%的单次输入数据量,并节约了成本。
3. 为保证系统的安全性, 对用户上传的初始数据文件进行了后缀名,文件大小的多重校验
4. 为防止某用户恶意占用系统资源, 基于 Redisson 的 RateLimiter 实现分布式分流, 控制用户访问的频
率。
5. 由于 AIGC 的响应时间较长,基于自定义 IO 密集型线程池 + 任务队列实现了 AIGC 的并发执行和异步
化.支持更多用户排队而不是无限给系统压力导致 提交失败,同时提交任务后即可响应前端提升用户体验。
6. 由于本地任务队列重启丢失数据, 使用 RabbitMQ来接受并持久化任务消息,通过 Direct 交换机转发给
解耦的 AI 生成模块消费并处理任务, 提高了系统的可靠性。
## 项目技术栈🎊
### 后端  
Spring Boot 2.7.2  
MyBatis   
MyBatis Plus   
Redis  
RabbitMQ  
讯飞星火API  
Swagger + Knife4j 项目文档  
Easy Excel  
 
### 前端
React  
Ant Design Pro    
Ant Design 组件库  
OpenAPI 代码生成  
EChart 图表生成  

### 数据存储
MySQL 

### 部署上线
腾讯云   
宝塔Linux  
## 项目展示🎊


