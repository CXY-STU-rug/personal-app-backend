package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyuq.DTO.TodosDto;
import com.liyuq.DTO.UpdateTodosDto;
import com.liyuq.VO.TodosVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Todos;
import com.liyuq.mapper.TodosMapper;
import com.liyuq.service.TodosService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 待办任务 服务实现类
 * </p>
 * 业务规则集中地：数据隔离（所有查询带user_id）、归属校验（改删先过getOwnedTodo）
 * 泛型链：ServiceImpl<TodosMapper, Todos> —— mapper和实体必须指向同一套
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class TodosServiceImpl extends ServiceImpl<TodosMapper, Todos> implements TodosService {

    @Autowired
    private  TodosMapper todosMapper;   // 也可以不声明，直接用父类白送的baseMapper，是同一个东西

    /**
     * 创建待办（契约3.4）。原先住在controller里，AI工具（3.28）也要创建待办时搬进了service——
     * controller是HTTP收发室只有前端能敲门，能力放车间才能多方复用，口径永远一份
     */
    public Long create(TodosDto dto) {
        Todos todo = new Todos();
        todo.setTitle(dto.getTitle());          // ① 调用方传的字段，逐个搬运
        todo.setRemark(dto.getRemark());
        todo.setDeadline(dto.getDeadline());
        todo.setPriority(dto.getPriority());    //    null也没关系，MP跳过该列，数据库DEFAULT 2兜底

        todo.setUserId(UserContext.getUserContextId());   // ② 归属：只能来自token——AI调用也自动继承这条红线

        todo.setCreatedAt(LocalDateTime.now());  // ③ NOT NULL列，不set就是SQL报错
        todo.setUpdatedAt(LocalDateTime.now());

        this.save(todo);     // ④ 落库（ServiceImpl白送的save），自增id会回填进todo
        return todo.getId(); // ⑤ 把新id交给调用方，包装成什么样是各调用方自己的事
    }

    /**
     * 待办列表：status是"筛选视角"（today/upcoming/completed），
     * 由后端翻译成不同的WHERE条件组合，和数据库status列(0/1)不是一回事
     */
    public List<TodosVo> list(String status) {
        LambdaQueryWrapper<Todos> queryWrapper = new LambdaQueryWrapper<Todos>();
        // 数据隔离第一锁：任何查询都先钉死user_id=当前登录人（来自JWT解析，绝不信前端传参）
        queryWrapper.eq(Todos::getUserId, UserContext.getUserContextId());
        // 常量放前面调equals：status为null（不传筛选）时返回false安全跳过，不会空指针
        if ("today".equals(status)) {

            // 必须放在方法内每次现算：这个类是单例，若提成字段则只在启动时算一次，过了午夜就是错的日期
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();   // 今天 00:00:00
            LocalDateTime tomorrowStart = todayStart.plusDays(1);        // 明天 00:00:00

            // 半开区间[今天0点, 明天0点)：比 <=23:59:59 严谨，不会漏掉带毫秒的时间
            queryWrapper.ge(Todos::getDeadline, todayStart)   // ge = >=，deadline >= 今天0点
                    .lt(Todos::getDeadline, tomorrowStart) // lt = <，deadline < 明天0点
                    .eq(Todos::getStatus, 0);              // 且未完成（产品决策：做完的不占today位置）

        } else if ("upcoming".equals(status)) {
            // upcoming = 未来 + 未完成，两个条件缺一不可（少了时间条件会把逾期任务也混进来）
            queryWrapper.eq(Todos::getStatus, 0).
                    gt(Todos::getDeadline, LocalDateTime.now());   // gt = >，截止时间在未来
        } else if ("completed".equals(status)) {
            queryWrapper.eq(Todos::getStatus, 1);   // 已完成只看status列，和时间无关
        }
        // status为null或乱传：不加额外条件，返回该用户全部待办

        List<Todos> list = todosMapper.selectList(queryWrapper);
        // 实体转VO：列表页只给文档3.3承诺的6个字段，userId/时间戳不外泄
        List<TodosVo> voList = new ArrayList<>();
        for (Todos todos : list) {
            TodosVo todosVo = new TodosVo();
            todosVo.setId(todos.getId());
            todosVo.setStatus(todos.getStatus());
            todosVo.setTitle(todos.getTitle());
            todosVo.setDeadline(todos.getDeadline());
            todosVo.setPriority(todos.getPriority());
            todosVo.setRemark(todos.getRemark());
            voList.add(todosVo);
        }
        return voList;
    }


    /**
     * 更新待办（文档3.5，含勾选完成）："不传的字段不修改"
     */
    public Integer update(Long id, UpdateTodosDto dto) {
          getOwnedTodo(id);   // 归属校验：不存在404/不是本人403，异常直接飞总闸，走不到下一行
        // 用updateById而不是UpdateWrapper.set()：前者对null字段自动跳过（不修改），
        // 后者会把null强写进数据库（清空）——"部分更新"语义必须用前者
        Todos todos = new Todos();
        todos.setId(id);                          // updateById靠id定位WHERE条件
        todos.setDeadline(dto.getDeadline());     // 以下字段：前端传了就更新，没传是null就跳过
        todos.setStatus(dto.getStatus());
        todos.setTitle(dto.getTitle());
        todos.setPriority(dto.getPriority());
        todos.setRemark(dto.getRemark());
        todos.setUpdatedAt(LocalDateTime.now());  // 更新时间无条件刷新
        int a = todosMapper.updateById(todos);    // 返回受影响行数
        if (a ==0) {
            throw new BusinessException(403,"更新失败");
        }
        return a;

    }

    /**
     * 删除待办（文档3.6）：先验归属再删，controller不允许直接调removeById
     */
    public Integer delete(Long id) {
        getOwnedTodo(id);   // 同update：校验不过直接抛异常中断
        Integer a =todosMapper.deleteById(id);
      if (a==0)
       throw new BusinessException(403,"删除失败");
        return a;
    }

    /**
     * 按id取待办并校验归属（全项目通用的"归属校验"模式，接口文档第4节要求）
     * fail-fast风格：不返回boolean让调用方自己判断（容易写反），
     * 而是校验不过直接抛异常——调用方只需一行，没有写反的可能
     * @param todoId 待办id
     * @return 校验通过的待办实体
     * @throws BusinessException 404=任务不存在，403=不是当前登录用户的数据
     */
    private Todos getOwnedTodo(Long todoId) {
        Todos todo = todosMapper.selectById(todoId);
        if (todo == null) {
            throw new BusinessException(404, "任务不存在");        // 情况一：查无此物
        }
        // Long是包装类型，比较数值必须用equals，==/!=比的是内存地址（超过127必错）
        if (!todo.getUserId().equals(UserContext.getUserContextId())) {
            throw new BusinessException(403, "无权操作");           // 情况二：不是你的
        }
        return todo;                                               // 情况三：合法，放行
    }


}
