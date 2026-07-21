package com.liyuq.controller;

import com.liyuq.DTO.TodosDto;
import com.liyuq.DTO.UpdateTodosDto;
import com.liyuq.VO.TodosVo;
import com.liyuq.common.Result;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Todos;
import com.liyuq.service.TodosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 待办任务 前端控制器
 * </p>
 * 分层职责：controller只做三件事——收参数、调service、包装Result返回。
 * 业务规则（归属校验、字段拼装）一律在service层，见TodosServiceImpl。
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController                    // = @Controller + @ResponseBody：返回值自动转JSON
@RequestMapping("/api/todo")       // 类级路径前缀，下面所有方法的路径都拼在它后面（对齐接口文档2.2）
public class TodosController {

    @Autowired
    private TodosService todosService;   // 注入service接口，Spring会找到它的实现类TodosServiceImpl

    /**
     * 3.3 待办列表：GET /api/todo/list?status=today|upcoming|completed
     */
    @GetMapping("/list")
    public Result<List<TodosVo>> list(@RequestParam(required = false) String status) {
        // @RequestParam：从URL问号后取参数；required=false表示可以不传（不传=查全部）
        // 筛选条件放query参数、资源身份放路径参数——REST惯例
        List<TodosVo> list = todosService.list(status);
        return Result.success(list);
    }

    /**
     * 3.4 创建待办：POST /api/todo/create
     * DTO是"字段白名单"：里面没有userId/id/status，前端就算恶意传了也会被Jackson丢弃
     */
    @PostMapping("/create")
    public Result<Map> create(@RequestBody TodosDto dto) {   // @RequestBody：把请求体JSON转成DTO对象
        // 业务已搬进service.create（AI工具也要复用创建能力），controller回归收发本职
        return Result.success(Map.of("id", todosService.create(dto)));   // 文档要求返回 {"id": 8}
    }
    /**
     * 3.5 更新待办（含勾选完成）：PUT /api/todo/update/:id
     * 归属校验在service内部（getOwnedTodo），不是本人的数据走不到更新那一步
     */
    @PutMapping("/update/{id}")   // {id}声明路径变量段，和@PathVariable成对出现
    public Result<Integer> update(@PathVariable Long id, @RequestBody UpdateTodosDto dto ) {
        // @PathVariable：把URL路径里{id}那一段绑定到参数id（名字相同自动对上）
       Integer a = todosService.update(id,dto);

    return Result.success(a);
    }

    /**
     * 3.6 删除待办：DELETE /api/todo/delete/:id
     * 同样先过service里的归属校验，404/403由BusinessException飞到全局异常总闸
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {

        Integer a=todosService.delete(id);

        return Result.success();
    }
}