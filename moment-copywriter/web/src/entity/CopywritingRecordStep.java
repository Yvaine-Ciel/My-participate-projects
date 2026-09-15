package entity;

public class CopywritingRecordStep {
    private int id;
    private int recordId;
    private int stepNo;
    private String userMessage;
    private String generatedContent;
    private String createTime;

    // 获取步骤 ID
    public int getId() {
        return id;
    }

    // 设置步骤 ID
    public void setId(int id) {
        this.id = id;
    }

    // 获取记录 ID
    public int getRecordId() {
        return recordId;
    }

    // 设置记录 ID
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    // 获取步骤序号
    public int getStepNo() {
        return stepNo;
    }

    // 设置步骤序号
    public void setStepNo(int stepNo) {
        this.stepNo = stepNo;
    }

    // 获取用户要求
    public String getUserMessage() {
        return userMessage;
    }

    // 设置用户要求
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    // 获取生成内容
    public String getGeneratedContent() {
        return generatedContent;
    }

    // 设置生成内容
    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
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
