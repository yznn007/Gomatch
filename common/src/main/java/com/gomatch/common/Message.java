package com.gomatch.common;

import org.json.JSONObject;

/**
 * JSON 协议消息，统一客户端与服务端的请求/响应格式。
 *
 * <h3>JSON 格式</h3>
 * <pre>{@code
 * {"cmd":"COMMAND","data":{"key":"value",...}}
 * }</pre>
 *
 * <h3>用法示例</h3>
 * <pre>{@code
 * // 构建消息
 * Message msg = Message.builder("LOGIN")
 *     .set("username", "<user-name>")
 *     .set("password", "<user-password>")
 *     .build();
 * String json = msg.toJson();
 *
 * // 解析消息
 * Message parsed = Message.fromJson(json);
 * String cmd = parsed.getCmd();
 * String username = parsed.getString("username");
 * }</pre>
 */
public class Message {

    private final String cmd;
    private final JSONObject data;

    private Message(String cmd, JSONObject data) {
        this.cmd = cmd;
        this.data = data != null ? data : new JSONObject();
    }

    // ==================== 工厂方法 ====================

    /** 从 JSON 字符串解析 */
    public static Message fromJson(String json) {
        JSONObject obj = new JSONObject(json);
        String cmd = obj.getString("cmd");
        JSONObject data = obj.optJSONObject("data");
        return new Message(cmd, data);
    }

    /** 构建仅含命令、无数据的消息（如心跳、认输等） */
    public static Message cmdOnly(String cmd) {
        return new Message(cmd, new JSONObject());
    }

    /** 快速构建（key-value 成对传入，数量必须为偶数） */
    public static Message of(String cmd, Object... keyValues) {
        JSONObject data = new JSONObject();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            data.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return new Message(cmd, data);
    }

    /** Builder 入口 */
    public static Builder builder(String cmd) {
        return new Builder(cmd);
    }

    // ==================== 序列化 ====================

    public String toJson() {
        JSONObject obj = new JSONObject();
        obj.put("cmd", cmd);
        if (data != null && !data.isEmpty()) {
            obj.put("data", data);
        }
        return obj.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }

    // ==================== 读取字段 ====================

    public String getCmd() { return cmd; }

    public JSONObject getData() { return data; }

    public String getString(String key) {
        return data.optString(key, "");
    }

    public int getInt(String key) {
        return data.optInt(key, 0);
    }

    public long getLong(String key) {
        return data.optLong(key, 0);
    }

    public boolean getBoolean(String key) {
        return data.optBoolean(key, false);
    }

    public boolean optBoolean(String key, boolean defaultValue) {
        return data.optBoolean(key, defaultValue);
    }

    public long optLong(String key, long defaultValue) {
        return data.optLong(key, defaultValue);
    }

    public boolean has(String key) {
        return data.has(key);
    }

    /** 判断 data 是否有有效内容 */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    // ==================== Builder ====================

    public static class Builder {
        private final String cmd;
        private final JSONObject data = new JSONObject();

        private Builder(String cmd) {
            this.cmd = cmd;
        }

        public Builder set(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Message build() {
            return new Message(cmd, data);
        }
    }
}
