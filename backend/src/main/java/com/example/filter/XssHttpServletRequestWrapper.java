package com.example.filter;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern IFRAME_PATTERN = Pattern.compile(
            "<iframe(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
            "<object(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern EMBED_PATTERN = Pattern.compile(
            "<embed(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<link(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern META_PATTERN = Pattern.compile(
            "<meta(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern STYLE_PATTERN = Pattern.compile(
            "<style(.*?)>(.*?)</style>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern COOKIE_PATTERN = Pattern.compile(
            "document\\.cookie",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
            "on\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SRC_PATTERN = Pattern.compile(
            "src[\r\n]*=[\r\n]*\\'(.*?)\\'",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern EVAL_PATTERN = Pattern.compile(
            "eval\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern BASE64_PATTERN = Pattern.compile(
            "data:text/html;base64",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
            "expression\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "javascript:",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile(
            "vbscript:",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ONLOAD_PATTERN = Pattern.compile(
            "onload(.*?)=",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleanedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanedValues[i] = cleanXss(values[i]);
        }
        return cleanedValues;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXss(value);
    }

    private String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String cleaned = value;
        cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = IFRAME_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = OBJECT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EMBED_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = LINK_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = META_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = STYLE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = SRC_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EVAL_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = BASE64_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EXPRESSION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = VBSCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = COOKIE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EVENT_HANDLER_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = ONLOAD_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replace("<", "&lt;").replace(">", "&gt;");
        return cleaned;
    }
}
