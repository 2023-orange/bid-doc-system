package com.example.biddoc.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.auth.entity.SysRole;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysRoleMapper; // 请替换为你实际的 Mapper 路径
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 Sa-Token 权限与角色认证接口实现类
 * <p>
 * 核心作用：
 * 告诉 Sa-Token 框架，当前登录的这个用户，到底拥有哪些“角色（Role）”和“权限码（Permission）”。
 * 只要实现了这个接口，代码里的 @SaCheckRole 和 @SaCheckPermission 注解就会自动生效。
 * </p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    /**
     * 【权限码列表】获取当前账号所拥有的权限集合
     * <p>
     * 1. 什么是权限码？
     * 它是最细粒度的权限控制，通常对应页面上的一个“按钮”或后端的一个“具体操作”。
     * 例如："file:download" (文件下载), "user:delete" (用户删除)。
     * <p>
     * 2. 什么时候会被触发？
     * 当你如果在 Controller 的方法上加上了 @SaCheckPermission("file:download") 时，
     * Sa-Token 就会自动跑来调用这个方法，索要当前用户的权限列表，看看里面有没有 "file:download"。
     * <p>
     * 3. 为什么现在返回空集合？
     * 因为目前的系统需求（按 PRD）只划分了“角色（管理员/普通用户）”和“职级（经理/员工）”，
     * 还没有细化到“按钮级别”的交叉权限控制，所以暂时不需要。
     * <p>
     * 4. 后期如何扩展（怎么用）？
     * 如果未来需要控制特定按钮，你需要：
     * - 数据库新建 `sys_permission` 表和 `sys_role_permission` 关联表。
     * - 在这里通过 loginId (即 userId) 查出他拥有的所有权限码字符串，add 到 list 中返回。
     * </p>
     *
     * @param loginId   当前登录人的账号id (即 SysUser 的 id)
     * @param loginType 账号体系标识 (默认是 "login")
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissionList = new ArrayList<>();

        /* * ⬇️ 后期扩展示例代码 (目前先注释掉) ⬇️
         *
         * // 1. 根据 userId 查询出他对应的 roleId
         * SysUser user = (SysUser) StpUtil.getSessionByLoginId(loginId).get("user");
         * * // 2. 根据 roleId 去 sys_role_permission 表查出所有的 permissionCode
         * List<String> codes = permissionMapper.selectCodesByRoleId(user.getRoleId());
         * permissionList.addAll(codes);
         */

        return permissionList;
    }

    /**
     * 【角色列表】获取当前账号所拥有的角色集合
     * <p>
     * 1. 什么是角色？
     * 它是用户的“头衔”或“岗位”。例如："ADMIN" (管理员), "MANAGER" (经理)。
     * <p>
     * 2. 什么时候会被触发？
     * 当你在代码里写了 StpUtil.hasRole("ADMIN")
     * 或者在 Controller 上加了 @SaCheckRole("MANAGER") 时被触发。
     * <p>
     * 3. 当前实现逻辑：
     * 为了防止每次鉴权都去查数据库（导致性能瓶颈），这里直接从 Sa-Token 的 Redis Session
     * 中读取用户缓存信息，将用户的【系统角色】和【业务职级】统一放入集合中。
     * </p>
     *
     * @param loginId   当前登录人的账号id
     * @param loginType 账号体系标识
     * @return 角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleList = new ArrayList<>();

        // 1. 【性能优化】从 Redis 高速缓存中取出当前用户对象 (在 AuthServiceImpl 登录时存入的)
        SysUser user = (SysUser) StpUtil.getSessionByLoginId(loginId).get("user");
        if (user == null) {
            return roleList;
        }

        // ==========================================================
        // 维度一：加载【系统角色】 (例如：SUPER_ADMIN, ADMIN, USER)
        // 作用：控制系统级的大模块权限，比如“能否进入后台管理页面”、“能否新建账号”
        // ==========================================================
        if (user.getRoleId() != null) {
            // 根据 RoleId 查询 SysRole 表，获取角色英文编码
            // （注：如果觉得这里每次查库慢，后期可以对 SysRoleMapper 加个 @Cacheable 缓存）
            SysRole sysRole = sysRoleMapper.selectById(user.getRoleId());
            if (sysRole != null && sysRole.getRoleCode() != null) {
                // 转大写加入集合，保持代码规范统一
                roleList.add(sysRole.getRoleCode().toUpperCase());
            }
        }

        // ==========================================================
        // 维度二：加载【业务职级】 (例如：MANAGER, SUPERVISOR, STAFF)
        // 作用：控制业务流程审批权限，比如“文档上传后是否需要当前登录人(经理)审批”
        // 映射规则：1-经理, 2-主管, 3-普通员工
        // ==========================================================
        if (user.getJobLevel() != null) {
            switch (user.getJobLevel()) {
                case 1:
                    roleList.add("MANAGER");      // 经理
                    break;
                case 2:
                    roleList.add("SUPERVISOR");   // 主管
                    break;
                case 3:
                    roleList.add("STAFF");        // 普通员工
                    break;
                default:
                    // 其他未知职级暂不处理
                    break;
            }
        }

        // 最终返回合并后的角色列表，比如可能是：["ADMIN", "MANAGER"]
        return roleList;
    }
}