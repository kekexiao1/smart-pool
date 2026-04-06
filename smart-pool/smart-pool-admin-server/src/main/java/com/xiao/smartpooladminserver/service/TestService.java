package com.xiao.smartpooladminserver.service;

public interface TestService {
    
    /**
     * 发送同步消息到指定主题
     * @param topic 主题名
     * @param message 消息内容
     */
    void sendSyncMessage(String topic, String message);
    
    /**
     * 执行测试任务
     * @param poolName 线程池名称
     * @param taskCount 任务数量
     */
    void executeTestTask(String poolName, int taskCount);
    
    /**
     * 测试线程池队列满告警
     * @param poolName 线程池名称
     * @param durationSeconds 持续时间（秒）
     * @return 测试结果信息
     */
    String testQueueFullAlert(String poolName, int durationSeconds);
    
    /**
     * 测试线程池运行超时告警
     * @param poolName 线程池名称
     * @param durationSeconds 持续时间（秒）
     * @return 测试结果信息
     */
    String testRunTimeoutAlert(String poolName, int durationSeconds);
}
