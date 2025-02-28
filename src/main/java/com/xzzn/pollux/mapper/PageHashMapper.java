package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.PageHash;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageHashMapper extends BaseMapper<PageHash> {
    // 使用 MyBatis Plus 提供的 selectOne 方法
    default Optional<PageHash> findByUrl(String url) {
        if (url.endsWith("#")) {
            url = url.substring(0, url.length() - 1);
        }
        QueryWrapper<PageHash> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", url);
        PageHash pageHash = this.selectOne(queryWrapper);
        return Optional.ofNullable(pageHash);
    }
}
