package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Notes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotesMapper  extends BaseMapper<Notes> {

    @Select("SELECT id, title, content, updated_at FROM notes WHERE user_id = #{userId} AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))")
    List<Notes> searchByKeyword(Long userId, String keyword);

}