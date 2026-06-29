package com.example.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多模型路由配置属性。
 * 绑定 resilience.qwen / glm / moonshot 下的 API 密钥、服务地址和模型名称。
 */
@ConfigurationProperties(prefix = "resilience")
public class ModelRouterProperties {

    private ModelEndpoint qwen = new ModelEndpoint();
    private ModelEndpoint glm = new ModelEndpoint();
    private ModelEndpoint moonshot = new ModelEndpoint();

    public ModelEndpoint getQwen() {
        return qwen;
    }

    public void setQwen(ModelEndpoint qwen) {
        this.qwen = qwen;
    }

    public ModelEndpoint getGlm() {
        return glm;
    }

    public void setGlm(ModelEndpoint glm) {
        this.glm = glm;
    }

    public ModelEndpoint getMoonshot() {
        return moonshot;
    }

    public void setMoonshot(ModelEndpoint moonshot) {
        this.moonshot = moonshot;
    }

    /**
     * 单个模型端点的配置（API 密钥、服务地址、模型名称）。
     */
    public static class ModelEndpoint {

        /** API 密钥（从环境变量注入，默认为空） */
        private String apiKey;

        /** 模型服务 Base URL */
        private String baseUrl;

        /** 模型名称（如 qwen-max、glm-4、moonshot-v1-8k） */
        private String modelName;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }
}
