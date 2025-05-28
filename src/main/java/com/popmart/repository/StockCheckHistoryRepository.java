package com.popmart.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.popmart.entity.StockCheckHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StockCheckHistoryRepository extends BaseMapper<StockCheckHistory> {
    
    @Select("SELECT * FROM stock_check_history WHERE product_id = #{productId} ORDER BY checked_at DESC")
    List<StockCheckHistory> findByProductIdOrderByCheckedAtDesc(@Param("productId") Long productId);
    
    @Select("SELECT * FROM stock_check_history WHERE product_id = #{productId} AND stock_changed = 1 ORDER BY checked_at DESC")
    List<StockCheckHistory> findByProductIdAndStockChangedTrueOrderByCheckedAtDesc(@Param("productId") Long productId);
    
    @Select("SELECT * FROM stock_check_history WHERE product_id = #{productId} AND checked_at >= #{since} ORDER BY checked_at DESC")
    List<StockCheckHistory> findByProductIdSince(@Param("productId") Long productId, @Param("since") LocalDateTime since);
    
    @Select("SELECT * FROM stock_check_history WHERE stock_changed = 1 AND checked_at >= #{since} ORDER BY checked_at DESC")
    List<StockCheckHistory> findStockChangesAfter(@Param("since") LocalDateTime since);
    
    @Select("SELECT * FROM stock_check_history WHERE product_id = #{productId} ORDER BY checked_at DESC LIMIT 10")
    List<StockCheckHistory> findLatestByProductId(@Param("productId") Long productId);
} 