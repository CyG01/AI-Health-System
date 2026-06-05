package com.example.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI openAPI() {
        Schema<Object> resultSchema = new Schema<>();
        resultSchema.setName("Result");
        resultSchema.addProperties("code", new Schema<>().type("integer").description("响应码").example(200));
        resultSchema.addProperties("msg", new Schema<>().type("string").description("响应消息").example("操作成功"));
        resultSchema.addProperties("data", new Schema<>().description("响应数据"));

        return new OpenAPI()
                .info(new Info()
                        .title("AI健康管理系统API文档")
                        .version("1.0.0"))
                .components(new Components()
                        .addSchemas("Result", resultSchema));
    }

    @Bean
    public GroupedOpenApi controllerApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.example.controller")
                .addOperationCustomizer(globalResultCustomizer())
                .build();
    }

    @Bean
    public OperationCustomizer globalResultCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            if (!responses.containsKey("200") && handlerMethod != null) {
                responses.addApiResponse("200", buildResultApiResponse("操作成功"));
            }
            responses.addApiResponse("400", buildResultApiResponse("参数错误"));
            responses.addApiResponse("401", buildResultApiResponse("未登录或token已过期"));
            responses.addApiResponse("403", buildResultApiResponse("无权限访问"));
            responses.addApiResponse("404", buildResultApiResponse("资源不存在"));
            responses.addApiResponse("500", buildResultApiResponse("操作失败"));
            return operation;
        };
    }

    private ApiResponse buildResultApiResponse(String description) {
        Schema<Object> schema = new Schema<>();
        schema.set$ref("#/components/schemas/Result");
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(schema)));
    }
}
