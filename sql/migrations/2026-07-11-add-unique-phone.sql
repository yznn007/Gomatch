-- 先执行以下查询；如存在结果，先在业务侧补齐或去重后再执行后续 ALTER。
SELECT user_id, username, phone
FROM t_user
WHERE phone IS NULL OR phone = '';

SELECT phone, COUNT(*) AS duplicate_count
FROM t_user
WHERE phone IS NOT NULL AND phone <> ''
GROUP BY phone
HAVING COUNT(*) > 1;

-- 执行前将所有号码规范化为带 + 的 E.164 格式（最多 15 位数字）。
ALTER TABLE t_user
    MODIFY COLUMN phone VARCHAR(16) NOT NULL COMMENT 'E.164国际手机号';

ALTER TABLE t_user
    ADD CONSTRAINT uk_user_phone UNIQUE (phone);