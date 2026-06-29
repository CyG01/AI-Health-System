package com.example.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：对 VARCHAR 字段透明加解密。
 *
 * 用法：
 * <pre>
 * &#064;TableField(typeHandler = EncryptedStringTypeHandler.class)
 * private String sensitiveField;
 * </pre>
 *
 * 写入时自动加密，读取时自动解密。
 * 对于空值直接透传，不做加解密处理。
 */
@Slf4j
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, AesEncryptor.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String cipher = rs.getString(columnName);
        return cipher == null ? null : AesEncryptor.decrypt(cipher);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String cipher = rs.getString(columnIndex);
        return cipher == null ? null : AesEncryptor.decrypt(cipher);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String cipher = cs.getString(columnIndex);
        return cipher == null ? null : AesEncryptor.decrypt(cipher);
    }
}