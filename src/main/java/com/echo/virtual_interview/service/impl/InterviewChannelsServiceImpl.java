package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.ChannelTopicMapper;
import com.echo.virtual_interview.mapper.InterviewChannelsMapper;
import com.echo.virtual_interview.mapper.TopicMapper;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelCardDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelCreateDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelFilterDTO;
import com.echo.virtual_interview.model.entity.ChannelTopic;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.echo.virtual_interview.model.entity.Topic;
import com.echo.virtual_interview.model.entity.Users;
import com.echo.virtual_interview.service.IInterviewChannelsService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 存储面试的配置模板，即“频道” 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
@Transactional
public class InterviewChannelsServiceImpl extends ServiceImpl<InterviewChannelsMapper, InterviewChannels> implements IInterviewChannelsService {
    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private ChannelTopicMapper channelTopicMapper;
    @Autowired
    private UsersServiceImpl usersServiceImpl;

    @Override
    public Map<String, Object> getFilterOptions() {
        // 使用 LinkedHashMap 来保证前端展示的顺序
        Map<String, Object> filterMap = new LinkedHashMap<>();

        // 1. 定义各个分类的列表
        List<String> jobTypes = Arrays.asList("校招", "社招", "实习", "兼职", "不限");
        List<String> interviewerStyles = Arrays.asList("综合","温和", "严肃", "技术型", "压力面", "行为面", "随机");
        List<String> interviewModes = Arrays.asList("一对一", "群面", "多对一","随机");
        List<String> durations = Arrays.asList("15", "30", "45","随机");

        List<String> majors = Arrays.asList("计算机科学与技术", "软件工程", "电子信息工程", "通信工程", "自动化");
        List<String> positions = Arrays.asList("后端工程师", "前端工程师", "算法工程师", "产品经理", "数据分析师");
        List<String> companies = Arrays.asList("阿里巴巴", "腾讯", "字节跳动", "华为", "美团");
        List<String> topics = Arrays.asList("Java", "Spring Boot", "分布式系统", "MySQL", "Redis", "操作系统", "计算机网络");
        List<String> usageCountSortOptions = Arrays.asList("正序", "倒序");

        // 3. 将中文键和对应的列表放入 Map
        filterMap.put("工作类型", jobTypes);
        filterMap.put("应聘岗位", positions);
        filterMap.put("专业方向", majors);
        filterMap.put("应聘公司", companies);
        filterMap.put("考官风格", interviewerStyles);
        filterMap.put("面试形式", interviewModes);
        filterMap.put("面试时长", durations);
        filterMap.put("考察重点", topics);
        filterMap.put("热门排行", usageCountSortOptions);

        return filterMap;
    }

    @Override
    public Page<ChannelCardDTO> listChannelsByPage(ChannelFilterDTO filterDTO) {
        // 1. 创建MyBatis-Plus的分页对象
        Page<InterviewChannels> page = new Page<>(filterDTO.getPageNum(), filterDTO.getPageSize());

        // 2. 【核心修改】处理多topics
        List<String> topicList = null;
        if (StringUtils.isNotBlank(filterDTO.getTopic())) {
            // 将 "java-mysql" 这种字符串分割成一个列表 ["java", "mysql"]
            topicList = Arrays.stream(filterDTO.getTopic().split("-"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
        }
        int topicCount = (topicList != null) ? topicList.size() : 0;
        Long userIdContext = Long.valueOf(UserIdContext.getUserIdContext());
        // 3. 调用Mapper XML中的自定义分页查询方法，并传入处理好的topicList
        Page<InterviewChannels> entityPage = this.baseMapper.selectChannelsByPage(page, filterDTO, topicList, topicCount, userIdContext);

        // 4. 将查询结果转换为DTO（这部分逻辑不变）
        Page<ChannelCardDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ChannelCardDTO> cardDTOs = entityPage.getRecords().stream().map(channel -> {
            ChannelCardDTO cardDTO = new ChannelCardDTO();
            BeanUtils.copyProperties(channel, cardDTO);
            return cardDTO;
        }).collect(Collectors.toList());
        dtoPage.setRecords(cardDTOs);

        return dtoPage;
    }



    @Override
    public ChannelDetailDTO getChannelDetails(Long id) { // 【修改】返回类型为 ChannelDetailDTO
        // 1. 查询频道主体信息，这部分逻辑不变
        InterviewChannels channel = this.baseMapper.selectById(id);

        if(channel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"频道不存在");
        }
        // 3. 增加使用次数的逻辑不变
        // 【优化】对于并发场景，这种先读后写的更新方式不是线程安全的。
        // 更优化的方式是直接在UPDATE语句中加1，例如 this.baseMapper.increaseUsageCount(id);
        // 但为了保持简单，我们暂时维持原样。
        channel.setUsageCount(channel.getUsageCount() == null ? 1 : channel.getUsageCount() + 1);
        this.baseMapper.updateById(channel);

        // 4. 【新增】调用新写的Mapper方法，查询该频道关联的所有topics
        List<String> topicNames = this.baseMapper.selectTopicNamesByChannelId(id);

        // 5. 【新增】组装最终的 DTO 对象
        ChannelDetailDTO detailDTO = new ChannelDetailDTO();

        // 5.1 从 entity 拷贝所有同名属性到 DTO
        BeanUtils.copyProperties(channel, detailDTO);

        // 5.2 将查询到的 topics 列表设置到 DTO 中
        detailDTO.setTopics(topicNames);

        // 6. 返回包含完整信息的 DTO
        return detailDTO;
    }
    @Override
    public ChannelDetailDTO getChannelDetailsNoAdd(Long id) { // 【修改】返回类型为 ChannelDetailDTO
        // 1. 查询频道主体信息，这部分逻辑不变
        InterviewChannels channel = this.baseMapper.selectById(id);

        if(channel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"频道不存在");
        }

        // 4. 【新增】调用新写的Mapper方法，查询该频道关联的所有topics
        List<String> topicNames = this.baseMapper.selectTopicNamesByChannelId(id);

        // 5. 【新增】组装最终的 DTO 对象
        ChannelDetailDTO detailDTO = new ChannelDetailDTO();

        // 5.1 从 entity 拷贝所有同名属性到 DTO
        BeanUtils.copyProperties(channel, detailDTO);

        // 5.2 将查询到的 topics 列表设置到 DTO 中
        detailDTO.setTopics(topicNames);

        // 6. 返回包含完整信息的 DTO
        return detailDTO;
    }
    @Override
    public InterviewChannels createChannel(ChannelCreateDTO createDTO, Long creatorId) {
        // 1. 创建并保存 InterviewChannel 主体
        InterviewChannels channel = new InterviewChannels();
        BeanUtils.copyProperties(createDTO, channel);
        channel.setCreatorId(creatorId);
        Users user = usersServiceImpl.getLoginUser();
        //如果勾选了要公开，则状态修改为审核中，等待管理员审核
        if(createDTO.getRequestPublic()&&!user.getRole().equals("admin")){
            channel.setVisibility("审核中");
        }
        if("admin".equals(user.getRole())) {
            channel.setVisibility("公开");
        }
        this.baseMapper.insert(channel);
        Long channelId = channel.getId(); // 获取新生成的频道ID

        // 2. 处理 Topics
        List<String> topicNames = createDTO.getTopics();
        if (topicNames != null && !topicNames.isEmpty()) {
            for (String topicName : topicNames) {
                String trimmedName = topicName.trim();
                if (StringUtils.isNotBlank(trimmedName)) {
                    // 3. 查询Topic是否已存在（Find or Create）
                    LambdaQueryWrapper<Topic> queryWrapper = new LambdaQueryWrapper<Topic>().eq(Topic::getName, trimmedName);
                    Topic existingTopic = topicMapper.selectOne(queryWrapper);

                    Integer topicId;
                    if (existingTopic == null) {
                        // 不存在，则创建新的Topic
                        Topic newTopic = new Topic(trimmedName);
                        topicMapper.insert(newTopic);
                        topicId = newTopic.getId();
                    } else {
                        // 已存在，直接使用其ID
                        topicId = existingTopic.getId();
                    }

                    // 4. 在关联表中创建关系
                    ChannelTopic channelTopic = new ChannelTopic(channelId, topicId);
                    channelTopicMapper.insert(channelTopic);
                }
            }
        }

        return channel;
    }
}
