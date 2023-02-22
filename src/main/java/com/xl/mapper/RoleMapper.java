package com.xl.mapper;

import com.xl.pojo.Role;

/**
 * This class is for xxxx.
 *
 * @author kk37005
 */
public interface RoleMapper {
    public Role getRole(Long id);
    public Role findRole(String roleName);
    public int deleteRole(Long id);
    public int insertRole(Role role);
}

