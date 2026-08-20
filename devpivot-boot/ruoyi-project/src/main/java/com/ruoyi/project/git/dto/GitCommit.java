package com.ruoyi.project.git.dto;

/**
 * Git 提交记录(平台无关)
 */
public class GitCommit
{
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private String authorLogin;
    private String date;
    private int additions;
    private int deletions;
    private int total;
    /** 关联到的团队成员ID(服务端按 email/login 映射,可能为空) */
    private Long memberId;
    /** 关联到的团队成员昵称 */
    private String memberName;

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }
    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
