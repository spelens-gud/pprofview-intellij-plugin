<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# pprofview 更新日志

## [Unreleased]
### Added
- 初始化项目结构
- 配置基础开发环境
- Go 运行配置 pprof 集成
  - 支持编译时插桩模式
  - 支持运行时采样模式
  - 支持手动采集模式
  - 支持 HTTP 服务模式
  - 支持多种性能分析类型（CPU、堆内存、协程、阻塞、互斥锁等）
  - 可配置输出目录和采样参数
  - 程序结束后自动打开分析结果
