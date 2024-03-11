# AI数据可视化平台
 `作者 CaoCao 😄`
## 项目介绍 📢
本项目是基于React+Spring Boot+RabbitMQ+AIGC的智能BI数据分析平台。 

访问地址：http://www.huahuaguagua.top  

随着AIGC的发展，越来越多的领域可以引入人工智能来帮助我们实现一些任务。于是本项目应运而生。不同于传统的数据分析平台,当我们分析数据趋势时需人工导入数据，选择要分析的字段和图表，并由专业数据分析师进行分析。然而，本项目只需导入原始数据和你想要分析的目标, 系统将利用AI自动生成可视化图表和详细的分析结论，使得分析数据更加轻松。  
## 项目架构图 🔥 
### 基础架构
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/1.png)

### 优化项目架构-异步化处理
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/2.png)

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
Redis
### 部署上线
腾讯云   
宝塔Linux  
## 项目展示🎊
### 注册页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/10.png)
### 登录页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/11.png)
### 智能分析页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/12.png)
### 图表管理页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/13.png)
### 图表详情页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/14.png)
### 分页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/15.png)
### 查询页
![image](https://github.com/gitgg021/AI-Chart/blob/master/images/16.png)





