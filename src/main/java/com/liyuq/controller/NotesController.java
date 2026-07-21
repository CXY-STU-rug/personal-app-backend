package com.liyuq.controller;

import com.liyuq.DTO.CreateNoteDto;
import com.liyuq.DTO.UpdateNoteDto;
import com.liyuq.DTO.queryNotesDto;
import com.liyuq.VO.NoteDetailVo;
import com.liyuq.VO.TotalnumbersnotesVo;
import com.liyuq.common.Result;
import com.liyuq.service.NotesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 绗旇?琛 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController                       // 声明这是REST控制器，方法返回值自动转JSON
@RequestMapping("/api/note")          // 类级前缀：下面每个方法的路径都以 /api/note 开头
public class NotesController {
@Autowired                            // 由Spring注入service实例，不用自己new
private NotesService notesService;


      // 3.13 笔记列表：GET /api/note/list?page=&pageSize=&tagId=
      // 无注解的 dto → Spring 按字段名从 query 参数填充
      @GetMapping("/list")
     public Result<TotalnumbersnotesVo> listnotes(queryNotesDto dto) {
        TotalnumbersnotesVo totalnumbersnotesVo= notesService.queryNotes(dto);   // 交给service查
       return Result.success(totalnumbersnotesVo);                              // 包进统一返回体
     }

// 3.14 新建笔记：POST /api/note/create
// @RequestBody：把前端发的JSON body({title,content,tagNames})转成DTO，和前端契约对齐
@PostMapping("/create")
    public Result<Map<String,Long>> createnotes(@RequestBody CreateNoteDto dto)
{

    Map<String,Long>list1=notesService.createnotes(dto);   // 返回新笔记id


return Result.success(list1);
}

// 详情：GET /api/note/detail/{id}，前端编辑页打开已有笔记时取正文全文+标签
@GetMapping("/detail/{id}")
    public Result<NoteDetailVo> detail(@PathVariable Long id){
    NoteDetailVo vo = notesService.getDetail(id);
    return Result.success(vo);
}

// 3.15 更新笔记：PUT /api/note/update/{id}
// {id} 从路径取(@PathVariable)，其余字段从JSON body取(@RequestBody)
@PutMapping("/update/{id}")
    public Result<Void> updateNotes(@PathVariable Long id, @RequestBody UpdateNoteDto dto){

          notesService.UpdateNotes(id,dto);
          return  Result.success();                 // 无数据返回，只给成功状态
}
// 3.16 删除笔记：DELETE /api/note/delete/{id}
@DeleteMapping("/delete/{id}")
    public Result<Void> deleteNotes(@PathVariable Long id){
          notesService.deleteNotes(id);
return  Result.success();
}

// 3.17 搜索笔记：GET /api/note/search?keyword=
@GetMapping("/search")
    public Result<TotalnumbersnotesVo> searchnotes(@RequestParam String keyword){

    TotalnumbersnotesVo totalnumbersnotesVo=notesService.searchNotes(keyword);
    return Result.success(totalnumbersnotesVo);

}


}
