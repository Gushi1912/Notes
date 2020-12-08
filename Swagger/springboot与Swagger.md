## springboot与Swagger

### Swagger简介

Swagger是一套基于OpenAPI规范构建的开源工具，可以帮助我们设计，构建，记录和使用REST API。Swagger的主要工具包括：

* Swagger编辑器：基于浏览器的编辑器，可以在其中编写OpenAPI规范。
* Swagger-ui：将OpenAPI规范以交互式文档的形式呈现。
* Swagger-Codengen - 根据OPenAPI规范生成服务器存根和客户端库。

### Swagger入门

首先创建一个Springboot项目，该项目可以很简单，不需要数据库，直接返回数据即可。

然后就是导入相关依赖：

~~~
<dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>2.9.2</version>
        </dependency>
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>2.9.2</version>
        </dependency>
~~~

swagger-ui依赖可以帮我们实现一个可视化的API操作界面。

访问地址一般是：**ipaddress:port/context-path/swagger-ui.html** 

> context-path是项目的启动路径

##### swagger配置类

~~~java
@Configuration //该注解是告诉springboot要加载该配置类
@EnableSwagger2 //该注解是swagger的注解，加上这个注解表示使用swagger，下面的配置才有用
public class SwaggerConfig
{
	//注册组件，组件名可以任意，返回的对象必须是Docket
    @Bean
    public Docket createRestApi()
    {
        return new Docket(DocumentationType.SWAGGER_2)
                // 是否启用Swagger
                .enable(enabled)
                // 用来创建该API的基本信息，展示在文档的页面中（自定义展示的信息）
                .apiInfo(apiInfo())
                // 设置哪些接口暴露给Swagger展示
                .select()
                // 扫描所有有注解的api，用这种方式更灵活
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                // 扫描指定包中的swagger注解
                // .apis(RequestHandlerSelectors.basePackage("com.ruoyi.project.tool.swagger"))
                // 扫描所有 .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                /* 设置安全模式，swagger可以设置访问token */
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts())
                .pathMapping(pathMapping);
    }

~~~



~~~java
@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口", hidden = false)
public class UserController {

    @ApiOperation("添加用户")
    @PostMapping("/add")
    public String add(@RequestBody User user) {
        StringBuilder builder = new StringBuilder();
        return builder.append("username:").append(user.getUsername()).append(",")
                .append("age: ").append(user.getAge()).append(",")
                .append("isFemale: ").append(user.getIsFemale()).append(",")
                .append("height: ").append(user.getHeight()).toString();
    }

    @ApiOperation("删除用户")
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable @ApiParam(name = "id",value = "用户id") Integer id) {
        return "the user is deleted and it's id is " + id;
    }

    @ApiOperation("更新用户")
    @PostMapping("/update")
    public String update(@RequestBody User user) {
        return "用户更新成功";
    }

    @ApiOperation("查找用户")
    @GetMapping("/find/{id}")
    public String find(@PathVariable @ApiParam(name = "id",value = "用户id") Integer id) {
        return "未能查找到id为：" + id + "的用户";
    }
}
~~~

