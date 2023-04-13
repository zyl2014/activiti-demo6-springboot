package com.yawn.util;

/**
 * @author yonglin.zhi. Date: 2023/4/12 Time: 18:54
 */
public enum DictEnum {

   APPLY_APPROVAL_OPINION_ASSIGN(0,"1"),
   APPLY_APPROVAL_OPINION_ASSIGN2(0,"1"),
   APPLY_APPROVAL_OPINION_ASSIGN3(0,"1"),
    ;

   int code;

   String key;

    DictEnum(int code, String key) {
        this.code = code;
        this.key = key;
    }

    DictEnum() {

    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
