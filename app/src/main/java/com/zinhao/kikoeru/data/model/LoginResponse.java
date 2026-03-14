package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 登录响应
 */
public class LoginResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private UserInfo user;

    @SerializedName("error")
    private String error;

    @SerializedName("errors")
    private List<ErrorItem> errors;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<ErrorItem> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorItem> errors) {
        this.errors = errors;
    }

    /**
     * 获取错误消息
     */
    public String getErrorMessage() {
        if (error != null && !error.isEmpty() && !"null".equals(error)) {
            return error;
        }
        if (errors != null && !errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ErrorItem item : errors) {
                if (item.getMsg() != null && !item.getMsg().isEmpty()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(item.getMsg());
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return null;
    }

    public static class UserInfo {
        @SerializedName("name")
        private String name;

        @SerializedName("id")
        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class ErrorItem {
        @SerializedName("msg")
        private String msg;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}
