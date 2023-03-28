package com.xl.mapper;

import com.xl.pojo.Role;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

/**
 * This class is for xxxx.
 *
 * @author kk37005
 */
public interface RoleMapper {
    public Role getRole(Long id);
    public Role getRoleByName(@Param("id") Long id,@Param("roleName") String roleName);
    public Role findRole(String roleName);
    public int deleteRole(Long id);
    public int insertRole(Role role);
}

