package com.lanyu.xiaolanaitravel.travel.dto;

/**
 * 固定 Workflow 在检查候选行程时发现的问题。
 *
 * 后续既可以返回给前端，
 * 也可以作为 Repair Agent 的明确修改依据。
 */
public record TravelValidationIssue(

        /**
         * 问题类型。
         *
         * 例如：
         * POI_NOT_FOUND
         * ROUTE_NOT_FOUND
         * ROUTE_TIME_CONFLICT
         * INVALID_TIME
         * PACE_TOO_TIGHT
         * BUDGET_EXCEEDED
         */
        String code,

        /**
         * 严重程度。
         *
         * WARNING：
         * 可以保留方案，但应该提醒。
         *
         * ERROR：
         * 原则上需要 Repair 或人工确认后再保存。
         */
        String severity,

        /**
         * 当前主要出问题的候选节点。
         */
        String draftItemKey,

        /**
         * 与当前问题有关的另一个候选节点。
         *
         * 例如路线冲突通常涉及：
         * 前一个节点 + 当前节点。
         *
         * 不需要时可以为 null。
         */
        String relatedDraftItemKey,

        /**
         * 给用户或 Repair Agent 阅读的具体问题说明。
         */
        String message
) {
}