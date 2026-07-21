package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.entity.AiMessages;
import org.apache.ibatis.annotations.Mapper;

/**
 * ai_messages表的数据库操作入口。
 * 继承BaseMapper就白得insert/selectList/delete等一整套方法；
 * 3.29/3.30的查询用QueryWrapper就够表达，暂时不需要手写SQL——需要时再加（和TodosMapper的月视图统计同理）
 */
@Mapper
public interface AiMessagesMapper extends BaseMapper<AiMessages> {
}
