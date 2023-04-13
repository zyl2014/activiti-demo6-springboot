package com.yawn.entity;

/**
 * @author yonglin.zhi. Date: 2023/4/12 Time: 18:28
 */
public abstract class ApprovalOpinionConverter {
    public static ApprovalOpinionConverter INSTANCE = null;


    /**
     * vo转dto
     *
     * @param vo
     * @return
     */
    public abstract ApprovalOpinionDTO vo2dto(ApprovalOpinionVO vo);

}
