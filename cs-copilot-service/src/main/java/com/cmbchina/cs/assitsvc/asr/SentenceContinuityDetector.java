package com.cmbchina.cs.assitsvc.asr;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户句子连续性判定器。
 *
 * <p>MVP 阶段使用硬编码词典，后续可替换为配置化词典或模型判断。
 */
@Component
public class SentenceContinuityDetector {

    private static final List<String> TRIGGER_KEYWORDS = Arrays.asList(
            "查询", "办理", "申请", "挂失", "修改", "取消", "激活", "还款",
            "账单", "卡片", "分期", "额度", "积分", "年费"
    );

    private static final List<String> PAUSE_WORDS = Arrays.asList(
            "就是", "然后", "因为", "所以", "但是", "呃", "那个", "嗯", "就那个", "上个月", "那笔"
    );

    private static final List<String> SUPPRESSOR_KEYWORDS = PAUSE_WORDS;

    private static final List<String> INCOMPLETE_SUFFIXES = append(PAUSE_WORDS, "就");

    /**
     * 判断一句客户文本的连续性。
     *
     * @param content ASR 文本
     * @return 连续性类型
     */
    public SentenceContinuity detect(String content) {
        if (!StringUtils.hasText(content)) {
            return SentenceContinuity.NEUTRAL;
        }

        String text = content.trim();
        if (endsWithAny(text, INCOMPLETE_SUFFIXES)) {
            return SentenceContinuity.INCOMPLETE;
        }

        boolean hasTrigger = containsAny(text, TRIGGER_KEYWORDS) || hasQuestionPattern(text);
        if (hasTrigger && !containsAny(text, SUPPRESSOR_KEYWORDS)) {
            return SentenceContinuity.COMPLETE;
        }

        return SentenceContinuity.NEUTRAL;
    }

    private static boolean hasQuestionPattern(String text) {
        return text.contains("怎么")
                || text.contains("能不能")
                || text.contains("多少")
                || text.contains("什么时候")
                || (text.contains("可以") && text.contains("吗"));
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithAny(String text, List<String> suffixes) {
        for (String suffix : suffixes) {
            if (text.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> append(List<String> values, String extraValue) {
        List<String> result = new ArrayList<>(values);
        result.add(extraValue);
        return result;
    }
}
