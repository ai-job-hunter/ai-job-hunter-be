package com.aijobhunter.service;

import com.aijobhunter.agent.ChatAgent;
import com.aijobhunter.agent.model.ChatReplyResult;
import com.aijobhunter.domain.conversation.Conversation;
import com.aijobhunter.domain.conversation.ConversationRepository;
import com.aijobhunter.domain.conversation.Message;
import com.aijobhunter.domain.conversation.MessageRepository;
import com.aijobhunter.domain.job.Job;
import com.aijobhunter.domain.job.JobRepository;
import com.aijobhunter.domain.user.User;
import com.aijobhunter.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JobRepository jobRepository;
    private final ChatAgent chatAgent;

    /**
     * 本机粘贴聊天记录导入：按行解析「HR:」「我:」前缀；无前缀默认视为 HR。
     */
    @Transactional
    public Map<String, Object> importFromPaste(String userId, String jobId, String hrName, String chatText) {
        if (chatText == null || chatText.isBlank()) {
            throw new IllegalArgumentException("请粘贴聊天内容");
        }
        List<String[]> lines = new ArrayList<>();
        for (String raw : chatText.split("\\R")) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("hr:") || lower.startsWith("hr：")
                    || line.startsWith("对方:") || line.startsWith("对方：")) {
                int idx = Math.max(line.indexOf(':'), line.indexOf('：'));
                String body = idx >= 0 ? line.substring(idx + 1).trim() : line;
                if (!body.isEmpty()) lines.add(new String[]{"INBOUND", body});
            } else if (line.startsWith("我:") || line.startsWith("我：")
                    || lower.startsWith("me:") || lower.startsWith("me：")) {
                int idx = Math.max(line.indexOf(':'), line.indexOf('：'));
                String body = idx >= 0 ? line.substring(idx + 1).trim() : line;
                if (!body.isEmpty()) lines.add(new String[]{"OUTBOUND", body});
            } else {
                lines.add(new String[]{"INBOUND", line});
            }
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("没有可导入的消息行");
        }

        Job job = null;
        if (jobId != null && !jobId.isBlank()) {
            job = jobRepository.findByIdAndUserId(jobId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));
        }
        Conversation conv = Conversation.builder()
                .userId(userId)
                .jobId(job == null ? null : job.getId())
                .hrName(blankTo(hrName, "HR"))
                .status(Conversation.ConversationStatus.CHATTING)
                .totalMessages(0)
                .unreadCount(0)
                .greetingSent(false)
                .build();
        conv = conversationRepository.save(conv);

        for (String[] pair : lines) {
            Message.Direction dir = "OUTBOUND".equals(pair[0])
                    ? Message.Direction.OUTBOUND : Message.Direction.INBOUND;
            appendMessage(conv, dir, pair[1], null, null);
            conv = conversationRepository.findById(conv.getId()).orElse(conv);
        }

        Map<String, Object> data = toListItem(conv, job);
        data.put("importedMessages", lines.size());
        return data;
    }

    @Transactional
    public Map<String, Object> create(String userId, String jobId, String hrName, String firstHrMessage) {
        Job job = null;
        if (jobId != null && !jobId.isBlank()) {
            job = jobRepository.findByIdAndUserId(jobId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));
        }

        Conversation conv = Conversation.builder()
                .userId(userId)
                .jobId(job == null ? null : job.getId())
                .hrName(blankTo(hrName, "HR"))
                .status(Conversation.ConversationStatus.CHATTING)
                .totalMessages(0)
                .unreadCount(0)
                .greetingSent(false)
                .build();
        conv = conversationRepository.save(conv);

        if (firstHrMessage != null && !firstHrMessage.isBlank()) {
            appendMessage(conv, Message.Direction.INBOUND, firstHrMessage.trim(), null, null);
            conv = conversationRepository.findById(conv.getId()).orElse(conv);
        }

        return toListItem(conv, job);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String userId) {
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId).stream()
                .map(c -> {
                    Job job = c.getJobId() == null ? null
                            : jobRepository.findByIdAndUserId(c.getJobId(), userId).orElse(null);
                    return toListItem(c, job);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String userId, String id) {
        Conversation conv = require(userId, id);
        Job job = conv.getJobId() == null ? null
                : jobRepository.findByIdAndUserId(conv.getJobId(), userId).orElse(null);
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(id);

        Map<String, Object> data = toListItem(conv, job);
        data.put("greetingText", conv.getGreetingText());
        data.put("messages", messages.stream().map(this::toMessageDto).collect(Collectors.toList()));
        return data;
    }

    @Transactional
    public Map<String, Object> addMessage(String userId, String id, String content, String direction) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        Conversation conv = require(userId, id);
        Message.Direction dir = parseDirection(direction);
        Message msg = appendMessage(conv, dir, content.trim(), null, null);
        return toMessageDto(msg);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> suggestedReply(String userId, User user, String id) {
        Conversation conv = require(userId, id);
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(id);
        Message lastInbound = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getDirection() == Message.Direction.INBOUND) {
                lastInbound = messages.get(i);
                break;
            }
        }
        if (lastInbound == null) {
            throw new IllegalArgumentException("还没有 HR 消息，请先录入一条来信");
        }

        String history = messages.stream()
                .map(m -> (m.getDirection() == Message.Direction.INBOUND ? "HR: " : "我: ") + m.getContent())
                .collect(Collectors.joining("\n"));

        ChatReplyResult result;
        try {
            var agentResult = chatAgent.generateReply(
                    lastInbound.getContent(),
                    history,
                    user.getUsername() != null ? user.getUsername() : "求职者",
                    "",
                    "",
                    "可谈",
                    "在职看机会");
            result = agentResult != null && agentResult.getData() != null
                    ? agentResult.getData()
                    : chatAgent.degradeReply();
        } catch (AgentException e) {
            log.warn("ChatAgent failed, degrade: {}", e.getMessage());
            result = chatAgent.degradeReply();
        }

        List<Map<String, Object>> suggestions = new ArrayList<>();
        Map<String, Object> primary = new HashMap<>();
        primary.put("content", result.getReplyText());
        primary.put("confidence", result.getConfidence());
        primary.put("intent", result.getIntent());
        suggestions.add(primary);
        if (result.getAlternativeReplies() != null) {
            for (ChatReplyResult.AlternativeReply alt : result.getAlternativeReplies()) {
                if (alt == null || alt.getContent() == null) continue;
                Map<String, Object> s = new HashMap<>();
                s.put("content", alt.getContent());
                s.put("confidence", 0.7);
                s.put("intent", result.getIntent());
                suggestions.add(s);
            }
        }

        Map<String, Object> context = new HashMap<>();
        context.put("hrLastMessage", lastInbound.getContent());
        context.put("hrIntent", result.getIntent());

        Map<String, Object> data = new HashMap<>();
        data.put("suggestions", suggestions);
        data.put("context", context);
        return data;
    }

    @Transactional
    public Map<String, Object> sendSuggested(String userId, String id, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        Conversation conv = require(userId, id);
        Map<String, Object> meta = new HashMap<>();
        meta.put("agentType", "CHAT");
        meta.put("source", "suggested-reply");
        Message msg = appendMessage(conv, Message.Direction.OUTBOUND, content.trim(), meta, null);
        return toMessageDto(msg);
    }

    private Message appendMessage(Conversation conv, Message.Direction direction, String content,
                                  Map<String, Object> aiMeta, Map<String, Object> hrIntent) {
        Message msg = Message.builder()
                .conversationId(conv.getId())
                .direction(direction)
                .content(content)
                .aiMetadata(aiMeta)
                .hrIntent(hrIntent)
                .status(Message.MessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        msg = messageRepository.save(msg);

        Map<String, Object> last = new HashMap<>();
        last.put("content", content);
        last.put("direction", direction.name());
        last.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().toString() : null);
        conv.setLastMessage(last);
        conv.setLastMessageAt(msg.getSentAt());
        conv.setTotalMessages((conv.getTotalMessages() == null ? 0 : conv.getTotalMessages()) + 1);
        if (direction == Message.Direction.INBOUND) {
            conv.setUnreadCount((conv.getUnreadCount() == null ? 0 : conv.getUnreadCount()) + 1);
        }
        if (conv.getStatus() == Conversation.ConversationStatus.GREETING) {
            conv.setStatus(Conversation.ConversationStatus.CHATTING);
        }
        conversationRepository.save(conv);
        return msg;
    }

    private Conversation require(String userId, String id) {
        return conversationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));
    }

    private Map<String, Object> toListItem(Conversation c, Job job) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", c.getId());
        data.put("hrName", c.getHrName());
        data.put("status", c.getStatus() == null ? null : c.getStatus().name());
        data.put("totalMessages", c.getTotalMessages());
        data.put("unreadCount", c.getUnreadCount());
        data.put("lastMessage", c.getLastMessage());
        data.put("greetingSent", c.getGreetingSent());
        data.put("lastMessageAt", c.getLastMessageAt());
        data.put("createdAt", c.getCreatedAt());
        if (job != null) {
            Map<String, Object> j = new LinkedHashMap<>();
            j.put("id", job.getId());
            j.put("title", job.getTitle());
            j.put("companyName", job.getCompanyName());
            j.put("city", job.getCity());
            data.put("job", j);
        } else {
            data.put("job", null);
        }
        return data;
    }

    private Map<String, Object> toMessageDto(Message m) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", m.getId());
        data.put("direction", m.getDirection() == null ? null : m.getDirection().name());
        data.put("content", m.getContent());
        data.put("status", m.getStatus() == null ? null : m.getStatus().name());
        data.put("sentAt", m.getSentAt());
        data.put("aiMetadata", m.getAiMetadata());
        data.put("hrIntent", m.getHrIntent());
        return data;
    }

    private static Message.Direction parseDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Message.Direction.OUTBOUND;
        }
        try {
            return Message.Direction.valueOf(direction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("direction 仅支持 INBOUND / OUTBOUND");
        }
    }

    private static String blankTo(String s, String def) {
        return s == null || s.isBlank() ? def : s.trim();
    }
}
