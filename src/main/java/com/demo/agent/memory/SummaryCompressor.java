package com.demo.agent.memory;

import com.demo.agent.chat.ChatClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryCompressor {

    private final ChatClientFactory factory;

    public String summarize(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return "";
        String history = messages.stream()
                .map(m -> m.getMessageType().getValue() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
        try {
            return factory.client("qwen-flash").prompt()
                    .system("""
                        请将以下对话压缩为 200 字以内的摘要，保留：
                        1) 用户已声明的事实和偏好
                        2) 关键决策和结论
                        3) 待办事项
                        去除寒暄和重复内容。
                        """)
                    .user(history)
                    .call().content();
        } catch (Exception e) {
            log.warn("summarize failed: {}", e.getMessage());
            return "[摘要生成失败，已保留 " + messages.size() + " 条原始消息]";
        }
    }

    public String merge(String prev, String latest) {
        try {
            return factory.client("qwen-flash").prompt()
                    .system("将两段摘要合并为一段，保留所有事实，去重，控制在 300 字内。")
                    .user("【已有摘要】\n" + prev + "\n\n【新摘要】\n" + latest)
                    .call().content();
        } catch (Exception e) {
            return prev + "\n" + latest;
        }
    }
}
