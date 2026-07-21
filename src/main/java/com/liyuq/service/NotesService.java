package com.liyuq.service;

import com.liyuq.DTO.CreateNoteDto;
import com.liyuq.DTO.UpdateNoteDto;
import com.liyuq.DTO.queryNotesDto;
import com.liyuq.VO.NoteDetailVo;
import com.liyuq.VO.TotalnumbersnotesVo;
import com.liyuq.entity.Notes;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 绗旇?琛 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
public interface NotesService extends IService<Notes> {

    // 3.13 分页查笔记列表，每条附带它的标签数组
    TotalnumbersnotesVo queryNotes(queryNotesDto dto);

    // 3.14 新建笔记 + 标签(find-or-create) + 写关联表，返回 {id:新笔记id}
    Map<String, Long> createnotes(CreateNoteDto dto);

    // 详情：GET /api/note/detail/{id}，返回单条笔记(含正文全文+标签)，先鉴权
    NoteDetailVo getDetail(Long id);

    // 3.15 更新笔记本身 + 标签全量覆盖(先删旧关系再重建)
    void UpdateNotes(Long id, UpdateNoteDto dto);

    // 3.16 删除笔记 + 清掉它的关联关系(标签不删)
    void deleteNotes(Long id);

    // 3.17 按关键字模糊搜标题/正文，返回结构同3.13
    TotalnumbersnotesVo searchNotes(String keyword);
}
