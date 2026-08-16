package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.entity.Posts;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface PostMapper extends BaseMapper<Posts> {
}
