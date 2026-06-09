package com.example.vo;

import java.io.Serializable;

public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String base64;

    private String uuid;

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
