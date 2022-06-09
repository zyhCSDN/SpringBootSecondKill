package com.debug.kill.server.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhaoYongHeng
 * @date 2020/12/14
 */
@Configuration
@EnableSwagger2
//public class SwaggerConfig extends WebMvcConfigurationSupport {
public class SwaggerConfig extends WebMvcConfigurationSupport {


    /**
     * 创建API应用
     * apiInfo() 增加API相关信息
     * 通过select()函数返回一个ApiSelectorBuilder实例,用来控制哪些接口暴露给Swagger来展现，
     * 本例采用指定扫描的包路径来定义指定要建立API的目录。
     *
     * @return
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(true)
                .select()
                //在controller包下以及子包下的controller类,你放其他包下可能就没用了
//                .apis(RequestHandlerSelectors.basePackage("com.debug.kill.server.controller"))
                //生成api的任何包
                .apis(RequestHandlerSelectors.any())
                // 对所有路径进行监控
                .paths(path -> !"/error".equals(path))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    /**
     * 配置swagger2的静态资源路径
     *
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 解决静态资源无法访问
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        // 解决swagger无法访问
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 解决swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 给API文档接口添加安全认证
     */
    private List<ApiKey> securitySchemes() {
        List<ApiKey> apiKeys = new ArrayList<>();
        apiKeys.add(new ApiKey("Authorization", "Authorization", "header"));
        return apiKeys;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("^(?!auth).*$")).build());
        return securityContexts;
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }

    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * 访问地址：http://项目实际地址/swagger-ui.html
     * <p>
     * <p>
     * 把Swagger2的API接口导入Postman 复制 http://127.0.0.1:8092/v2/api-docs  打开postman-->import-->import Form Link
     *
     * @return
     */
    private ApiInfo apiInfo() {
        Contact contact = new Contact("zyh", "", "1461264354@qq.com");
        return new ApiInfoBuilder()
                //就是说这个界面叫什么名字
                .title("kill秒杀及高并发插入数据测试接口")
                .description("Zhaoyongheng会变强的")
                .contact(contact)
                .termsOfServiceUrl("http://127.0.0.1:8092/swagger-ui.html")
                //对这个界面的描述
                .version("1.0")
                .build();
    }
}

