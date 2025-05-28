package com.popmart.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.popmart.entity.MonitoredProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MonitoredProductRepository extends BaseMapper<MonitoredProduct> {
    
    @Select("SELECT * FROM monitored_products WHERE is_active = 1 AND deleted = 0")
    List<MonitoredProduct> findByIsActiveTrue();
    
    @Select("SELECT * FROM monitored_products WHERE url = #{url} AND deleted = 0")
    Optional<MonitoredProduct> findByUrl(@Param("url") String url);
    
    @Select("SELECT * FROM monitored_products WHERE added_by_user_id = #{userId} AND deleted = 0")
    List<MonitoredProduct> findByAddedByUserId(@Param("userId") String userId);
    
    @Select("SELECT * FROM monitored_products WHERE added_by_user_id = #{userId} AND is_active = 1 AND deleted = 0")
    List<MonitoredProduct> findByAddedByUserIdAndIsActiveTrue(@Param("userId") String userId);
    
    @Select("SELECT * FROM monitored_products WHERE is_active = 1 AND url LIKE CONCAT('%', #{domain}, '%') AND deleted = 0")
    List<MonitoredProduct> findActiveProductsByDomain(@Param("domain") String domain);
    
    @Select("SELECT COUNT(*) FROM monitored_products WHERE is_active = 1 AND deleted = 0")
    long countActiveProducts();
    
    @Select("SELECT COUNT(*) FROM monitored_products WHERE is_active = 1 AND last_known_stock = 1 AND deleted = 0")
    long countInStockProducts();

    @Select("SELECT * FROM monitored_products WHERE product_id = #{productId} AND deleted = 0")
    Optional<MonitoredProduct> findByProductId(@Param("productId") String productId);
} 