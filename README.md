# ConsoleTriggerPlugin
MC检测控制台输出自动执行命令插件
# 配置文件：
全局冷却时间（秒），优先级低于独立配置
global-cooldown: 5

触发规则配置（两种格式兼容）
triggers:
格式1：带独立冷却时间
  "玩家死亡":
    command: "say [系统] 玩家又双叒叕死了！"
    cooldown: 10

格式2：使用全局冷却时间
  "ERROR":
    command: "save-all"

格式3：旧版简写模式（无冷却）
  "Server overloaded": "restart"
只写了中文版

# 修复了冷却刷屏的问题
