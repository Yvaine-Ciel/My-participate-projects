package entity;

public class CopywritingRecord {
    private int id;
    private int userId;
    private String scene;
    private String mood;
    private String style;
    private String keywords;
    private String generatedContent;
    private String aiModel;
    private String createTime;
    private boolean favorite;
    private String favoriteTime;

    // 创建空文案记录
    public CopywritingRecord() {
    }

    // 获取记录 ID
    public int getId() {
        return id;
    }

    // 设置记录 ID
    public void setId(int id) {
        this.id = id;
    }

    // 获取用户 ID
    public int getUserId() {
        return userId;
    }

    // 设置用户 ID
    public void setUserId(int userId) {
        this.userId = userId;
    }

    // 获取场景
    public String getScene() {
        return scene;
    }

    // 设置场景
    public void setScene(String scene) {
        this.scene = scene;
    }

    // 获取情绪
    public String getMood() {
        return mood;
    }

    // 设置情绪
    public void setMood(String mood) {
        this.mood = mood;
    }

    // 获取风格
    public String getStyle() {
        return style;
    }

    // 设置风格
    public void setStyle(String style) {
        this.style = style;
    }

    // 获取关键词
    public String getKeywords() {
        return keywords;
    }

    // 设置关键词
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    // 获取生成内容
    public String getGeneratedContent() {
        return generatedContent;
    }

    // 设置生成内容
    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    // 获取 AI 模型
    public String getAiModel() {
        return aiModel;
    }

    // 设置 AI 模型
    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    // 获取创建时间
    public String getCreateTime() {
        return createTime;
    }

    // 设置创建时间
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    // 获取收藏状态
    public boolean isFavorite() {
        return favorite;
    }

    // 设置收藏状态
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    // 获取收藏时间
    public String getFavoriteTime() {
        return favoriteTime;
    }

    // 设置收藏时间
    public void setFavoriteTime(String favoriteTime) {
        this.favoriteTime = favoriteTime;
    }
}
