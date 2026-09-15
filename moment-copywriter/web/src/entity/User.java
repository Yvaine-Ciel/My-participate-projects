package entity;

public class User {
    private int id;
    private String username;
    private String phone;
    private String role;
    private String createTime;

    // 创建空用户对象
    public User() {
    }

    // 创建完整用户对象
    public User(int id, String username, String phone, String role, String createTime) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.role = role;
        this.createTime = createTime;
    }

    // 获取用户 ID
    public int getId() {
        return id;
    }

    // 设置用户 ID
    public void setId(int id) {
        this.id = id;
    }

    // 获取用户名
    public String getUsername() {
        return username;
    }

    // 设置用户名
    public void setUsername(String username) {
        this.username = username;
    }

    // 获取手机号
    public String getPhone() {
        return phone;
    }

    // 设置手机号
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // 获取用户角色
    public String getRole() {
        return role;
    }

    // 设置用户角色
    public void setRole(String role) {
        this.role = role;
    }

    // 获取创建时间
    public String getCreateTime() {
        return createTime;
    }

    // 设置创建时间
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
