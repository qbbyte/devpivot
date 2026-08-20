package com.ruoyi.project.git.dto;

/**
 * Git 贡献者统计(每人提交数 / 增删行)
 */
public class GitContributor
{
    private String login;
    private String name;
    private String email;
    private int contributions;
    private int additions;
    private int deletions;
    /** 关联到的团队成员ID(服务端按 email/login 映射,可能为空) */
    private Long memberId;
    /** 关联到的团队成员昵称 */
    private String memberName;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getContributions() { return contributions; }
    public void setContributions(int contributions) { this.contributions = contributions; }
    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }
    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
