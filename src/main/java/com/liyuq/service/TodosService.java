package com.liyuq.service;

import com.liyuq.DTO.TodosDto;
import com.liyuq.DTO.UpdateTodosDto;
import com.liyuq.VO.TodosVo;
import com.liyuq.entity.Todos;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 待办任务 服务类
 * </p>
 * 接口=对外菜单：这里声明的是"业务能力"；IService白送的通用CRUD不在此列。
 * 私有工序（如getOwnedTodo）只存在于实现类，不对外暴露。
 *
 * @author liyuq
 * @since 2026-07-13
 */
public interface TodosService extends IService<Todos> {

    /** 创建待办，返回新任务id；归属自动取当前登录用户（controller和AI工具共用的能力） */
    Long create(TodosDto dto);

    /** 按筛选视角查当前登录用户的待办：today/upcoming/completed/null=全部 */
    List<TodosVo> list(String status);

    /** 部分更新（不传的字段不修改），内部先做归属校验 */
    Integer update(Long id, UpdateTodosDto dto);

    /** 删除，内部先做归属校验 */
    Integer delete(Long id);
}
