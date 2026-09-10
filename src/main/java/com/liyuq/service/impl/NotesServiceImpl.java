package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liyuq.DTO.CreateNoteDto;
import com.liyuq.DTO.UpdateNoteDto;
import com.liyuq.DTO.queryNotesDto;
import com.liyuq.VO.NoteDetailVo;
import com.liyuq.VO.notesVo;
import com.liyuq.VO.TotalnumbersnotesVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.NoteTagRelations;
import com.liyuq.entity.NoteTags;
import com.liyuq.entity.Notes;
import com.liyuq.mapper.NoteTagRelationsMapper;
import com.liyuq.mapper.NoteTagsMapper;
import com.liyuq.mapper.NotesMapper;
import com.liyuq.service.NotesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 绗旇?琛 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class NotesServiceImpl extends ServiceImpl<NotesMapper, Notes> implements NotesService {

    @Autowired
    private NotesMapper notesMapper;
    @Autowired
    private UserContext userContext;
    @Autowired
    private NoteTagRelationsMapper noteTagRelationsMapper;
    @Autowired
    private NoteTagsMapper noteTagsMapper;
//### 3.13 获取笔记列表
    public TotalnumbersnotesVo queryNotes(queryNotesDto dto) {
        Long userId = UserContext.getUserContextId();   // 从ThreadLocal取当前登录用户，token解析时存进去的
        long pageNum  = dto.getPage()     == null ? 1  : dto.getPage();
        long pageSize = dto.getPageSize() == null ? 20 : dto.getPageSize();
        Page page = new Page(pageNum, pageSize);
        LambdaQueryWrapper<Notes> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Notes::getUserId, userId);

        if (dto.getTagId() != null) {
            LambdaQueryWrapper<NoteTagRelations> tagWrapper = Wrappers.<NoteTagRelations>lambdaQuery();
            tagWrapper.eq(NoteTagRelations::getTagId, dto.getTagId());
            List<NoteTagRelations> rels = noteTagRelationsMapper.selectList(tagWrapper);

            if (rels.isEmpty()) {                          // 标签下没笔记：直接返回空，不能让空集合进in()
                TotalnumbersnotesVo empty = new TotalnumbersnotesVo();
                empty.setTotal(0L);
                empty.setList(new ArrayList<>());
                return empty;
            }

            List<Long> noteIds = new ArrayList<>();
            for (NoteTagRelations rel : rels) {
                noteIds.add(rel.getNoteId());
            }
            queryWrapper.in(Notes::getId, noteIds);        // 主查询只查这些id
        }




        queryWrapper.orderByDesc(Notes::getId);
        IPage<Notes> result=notesMapper.selectPage(page, queryWrapper);


        TotalnumbersnotesVo totalnumbersnotesVo=new TotalnumbersnotesVo();
        totalnumbersnotesVo.setTotal(result.getTotal());
        List<notesVo> voList = new ArrayList<>();
        for (Notes n : result.getRecords()) {          // 注意：你的变量叫 result 不叫 page
            notesVo vo = new notesVo();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setUpdatedAt(n.getUpdatedAt());

            // —— 给这一条笔记拼它的标签数组（多对多的"读"）——
            LambdaQueryWrapper<NoteTagRelations> relWrapper = Wrappers.<NoteTagRelations>lambdaQuery();
            relWrapper.eq(NoteTagRelations::getNoteId, n.getId());              // 关联表里 note_id=当前这条笔记的所有行
            List<NoteTagRelations> rels = noteTagRelationsMapper.selectList(relWrapper);
            List<String> tagNames = new ArrayList<>();                         // 这条笔记的标签名数组（循环内新建，每条独立）
            for (NoteTagRelations rel : rels) {                                // 遍历每个关联行，把 tag_id 换成名字
                NoteTags tag = noteTagsMapper.selectById(rel.getTagId());      // 用 tag_id 去标签表查
                tagNames.add(tag.getName());                                   // 名字塞进数组
            }
            vo.setTags(tagNames);                                             // 挂到VO上（漏了这步tags就是null）

            voList.add(vo);
        }
        totalnumbersnotesVo.setList(voList);
        return totalnumbersnotesVo;


    }
// public Map<String, Long> createnotes(String title, String content, List<String> list) {
//          Long userId = UserContext.getUserContextId();
//          Notes notes = new Notes();
//          for (String tagName : list) {
//              NoteTags noteTags = new NoteTags();
//              noteTags.setUserId(userId);
//              noteTags.setName(tagName);
//              noteTagsMapper.insert(noteTags);
//              NoteTagRelations noteTagRelations = new NoteTagRelations();
//              noteTagRelations.setTagId(noteTags.getId());
//              noteTagRelations.setNoteId(notes.getId());
//              noteTagRelationsMapper.insert(noteTagRelations);
//          }
//          notes.setTitle(title);
//          notes.setContent(content);
//          notes.setUserId(userId);
//          notes.setCreatedAt(LocalDateTime.now());
//          notes.setUpdatedAt(LocalDateTime.now());
//          notesMapper.insert(notes);
//          return Map.of("id", notes.getId());
//      }
@Transactional

    public Map<String, Long> createnotes(CreateNoteDto dto) {// 入参改成DTO，字段从dto取
    if (dto.getTitle() == null || dto.getTitle().isEmpty()) {
        throw new BusinessException(404, "标题不能为空");
    }

    Long userId = UserContext.getUserContextId();
    Notes notes = new Notes();
    notes.setTitle(dto.getTitle());        // 从DTO取，不再是散参数
    notes.setContent(dto.getContent());
    notes.setUserId(userId);
    notes.setCreatedAt(LocalDateTime.now());
    notes.setUpdatedAt(LocalDateTime.now());
    notesMapper.insert(notes);
    // 标签是可选的：null（前端 JSON 里没有 tagNames 这个键）或空数组，都跳过整个循环，
    // 笔记本身照样创建成功——"不打标签的笔记"是合法用法，不是错误
    // != null 必须写在 && 左边：&& 从左往右求值，靠短路挡住右边的 .isEmpty()
    if (dto.getTagNames() != null && !dto.getTagNames().isEmpty()) {
        for (String tagName : dto.getTagNames()) {   // 遍历前端传的 tagNames
            // 每轮新建 wrapper，条件跟着本轮 tagName 变
            LambdaQueryWrapper<NoteTags> w = Wrappers.lambdaQuery();
            w.eq(NoteTags::getUserId, userId).eq(NoteTags::getName, tagName);
            NoteTags exist = noteTagsMapper.selectOne(w);

            Integer tagId;                    // 不管新建还是复用，最后都要有它
            if (exist == null) {              // 查不到 → 新建
                NoteTags noteTags = new NoteTags();
                noteTags.setUserId(userId);
                noteTags.setName(tagName);
                noteTagsMapper.insert(noteTags);
                tagId = noteTags.getId();     // 用回填的新 id
            } else {                          // 查到了 → 直接用现成的
                tagId = exist.getId();
            }

            // 到这里 tagId 一定有值 —— 插关系
            NoteTagRelations rel = new NoteTagRelations();
            rel.setNoteId(notes.getId());     // 前提：notes 已在循环前 insert 过
            rel.setTagId(tagId);
            noteTagRelationsMapper.insert(rel);
        }
    }
        return Map.of("id", notes.getId());
    }

// 3.15 更新笔记：更新正文 + 标签全量覆盖。跨表多写，@Transactional保证要么全成要么全滚
@Transactional
    public void UpdateNotes(Long id, UpdateNoteDto dto) {
        getandkonwuseridnotes(id);        // 先鉴权：不是本人的笔记直接抛403
        Notes notes = new Notes();
        notes.setId(id);                  // 指定改哪条
        notes.setTitle(dto.getTitle());
        notes.setUpdatedAt(LocalDateTime.now());
        notes.setContent(dto.getContent());
        notesMapper.updateById(notes);    // 更新笔记本身(title/content/时间)

       // LambdaQueryWrapper<NoteTags> queryWrapper = Wrappers.lambdaQuery();
      //  queryWrapper.eq(NoteTags::getUserId,UserContext.getUserContextId()).eq(NoteTags::getId,notes.getId());

       // List<String> tagNames = dto.getTagNames();
       // if (tagNames != null && !tagNames.isEmpty()) {
         //   for (String tagName : tagNames) {
        //        NoteTags noteTags = new NoteTags();
            //    noteTags.setName(tagName);

          //  noteTagsMapper.update(noteTags,queryWrapper);
          //  }
        noteTagRelationsMapper.delete(new LambdaQueryWrapper<NoteTagRelations>().eq(NoteTagRelations::getNoteId, id));
        List<String> tagNames = dto.getTagNames();
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String tagName : tagNames) {
                LambdaQueryWrapper<NoteTags> w = Wrappers.lambdaQuery();
                w.eq(NoteTags::getName, tagName).eq(NoteTags::getUserId, UserContext.getUserContextId());
                NoteTags exist = noteTagsMapper.selectOne(w);
                Integer tagId;
                if (exist == null) {
                    NoteTags noteTag = new NoteTags();
                    noteTag.setName(tagName);
                    noteTag.setUserId(UserContext.getUserContextId());
                    noteTagsMapper.insert(noteTag);
                    tagId = noteTag.getId();                    // 用回填的新id
                } else {                                        // 查到了 → 复用现成的
                    tagId = exist.getId();
                }

                NoteTagRelations rel = new NoteTagRelations();
                rel.setNoteId(id);
                rel. setTagId(tagId);
 noteTagRelationsMapper.insert(rel);
            }

        }
    }
// 3.16 删除笔记：先清关系再删笔记，标签本身不动(别的笔记还共用)
@Transactional
    @Override
    public void deleteNotes(Long id) {
        getandkonwuseridnotes(id);        // 鉴权
        // 先删这条笔记的所有关联关系(按 note_id 条件删；关联表无单id主键)
        noteTagRelationsMapper.delete( new LambdaQueryWrapper<NoteTagRelations>().eq(NoteTagRelations::getNoteId, id));
        notesMapper.deleteById(id);       // 再删笔记本身
    }


    @Override
    public TotalnumbersnotesVo searchNotes(String keyword) {
        Long userId = UserContext.getUserContextId();

        //查所有的notes笔记
        List<Notes> list = notesMapper.searchByKeyword(userId, keyword);
        List<notesVo> voList = new ArrayList<>();          // 装转好的VO
        for (Notes notes : list) {                         // 逐条笔记处理
            notesVo vo = new notesVo();
            vo.setId(notes.getId());
            vo.setTitle(notes.getTitle());
            vo.setUpdatedAt(notes.getUpdatedAt());

            // —— 查"这一条笔记"自己的标签（循环内，各查各的）——
            LambdaQueryWrapper<NoteTagRelations> relW = Wrappers.<NoteTagRelations>lambdaQuery();
            relW.eq(NoteTagRelations::getNoteId, notes.getId());   // 只要这条笔记的关系行
            List<NoteTagRelations> rels = noteTagRelationsMapper.selectList(relW);

            List<String> tagNames = new ArrayList<>();     // 这条笔记的标签名（循环内新建，独立）
            for (NoteTagRelations rel : rels) {            // 每个关系行：tag_id → 名字
                NoteTags tag = noteTagsMapper.selectById(rel.getTagId());
                tagNames.add(tag.getName());
            }
            vo.setTags(tagNames);                          // 贴各自的标签

            voList.add(vo);
        }

        TotalnumbersnotesVo result = new TotalnumbersnotesVo();
        result.setTotal((long) list.size());              // 不分页，size就是total
        result.setList(voList);
        return result;
    }
// LambdaQueryWrapper<NoteTags> queryWrapper = Wrappers.lambdaQuery();
//
//        queryWrapper.eq(NoteTags::getUserId, userId).eq(NoteTags::getName, keyword);
//        //查该用户的标签
//        List<NoteTags> list1 = noteTagsMapper.selectList(queryWrapper);
//
//        List<String>list2=new ArrayList<>();
//
//        List<notesVo> list3=new ArrayList<>();
//
//        TotalnumbersnotesVo totalnumbersnotesVo=new TotalnumbersnotesVo();
//        //遍历查到的标签，塞进list2方便使用
//        for (NoteTags noteTags : list1) {
//       list2.add(noteTags.getName());
//       }
//        //遍历查到的笔记放进实体类，转vo
//        for (Notes notes : list) {
//            notesVo vo = new notesVo();
//            vo.setId(notes.getId());
//            vo.setTitle(notes.getTitle());
//            vo.setUpdatedAt(notes.getUpdatedAt());
//vo.setTags(list2);
//            list3.add(vo);
//        }
//        totalnumbersnotesVo.setTotal((long) list.size());
//        totalnumbersnotesVo.setList(list3);




    // 详情：GET /api/note/detail/{id}
    @Override
    public NoteDetailVo getDetail(Long id) {
        Notes notes = getandkonwuseridnotes(id);   // 先鉴权+取笔记：不存在404、不是本人403

        NoteDetailVo vo = new NoteDetailVo();
        vo.setId(notes.getId());
        vo.setTitle(notes.getTitle());
        vo.setContent(notes.getContent());         // 详情才给正文全文
        vo.setCreatedAt(notes.getCreatedAt());
        vo.setUpdatedAt(notes.getUpdatedAt());

        // —— 拼这条笔记的标签数组：跟你 3.13/3.17 一模一样的套路 ——
        LambdaQueryWrapper<NoteTagRelations> relW = Wrappers.<NoteTagRelations>lambdaQuery();
        relW.eq(NoteTagRelations::getNoteId, id);              // 关联表里这条笔记的所有行
        List<NoteTagRelations> rels = noteTagRelationsMapper.selectList(relW);
        List<String> tagNames = new ArrayList<>();
        for (NoteTagRelations rel : rels) {                    // tag_id → 名字
            NoteTags tag = noteTagsMapper.selectById(rel.getTagId());
            tagNames.add(tag.getName());
        }
        vo.setTags(tagNames);

        return vo;
    }

    // 归属校验公共方法：查到→非本人抛403；查不到→抛404
    private Notes getandkonwuseridnotes(Long id) {
           Notes notes=notesMapper.selectById(id);
           if (notes == null)                        // 先判空：不存在直接404，避免下面 .getUserId() 空指针
               throw new BusinessException(404,"笔记不存在");
           Long userid = UserContext.getUserContextId();
          if(!userid.equals(notes.getUserId()))
              throw new BusinessException(403,"无权操作");
          return notes;
}

}
