package com.example.filter;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern SRC_PATTERN = Pattern.compile(
            "src[\r\n]*=[\r\n]*\\'(.*?)\\'",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern EVAL_PATTERN = Pattern.compile(
            "eval\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
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
        cleaned = SRC_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EVAL_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = EXPRESSION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = JAVASCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = VBSCRIPT_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = ONLOAD_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replace("<", "&lt;").replace(">", "&gt;");
        return cleaned;
    }
}
