package com.zjgsu.todoservice.repository;

import com.zjgsu.todoservice.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Todo数据访问层
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    /**
     * 根据用户ID查找所有Todo
     */
    List<Todo> findByUserId(Long userId);

    /**
     * 根据用户ID和完成状态查找Todo
     */
    List<Todo> findByUserIdAndCompleted(Long userId, Boolean completed);

    /**
     * 根据完成状态查找Todo
     */
    List<Todo> findByCompleted(Boolean completed);

    /**
     * 根据标题模糊查询
     */
    List<Todo> findByTitleContaining(String keyword);
}
