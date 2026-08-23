# Litematica Creator 项目概览

## 项目定位

Litematica Creator 是一个纯客户端 Litematica 附属。它把“编辑原理图”变成接近创造模式放置方块的第一人称操作，让生存玩家无需取得真实物品，也能在世界环境中快速设计 `.litematic` 草稿。

模组只修改客户端 schematic 数据，不改变真实世界，不发送对应的真实攻击或放置包。编辑结果仍是普通 Litematica schematic，可以继续使用 Litematica 的保存、渲染、材料列表、Verifier 和建造流程。

## 项目亮点

- **直接、空间化的编辑体验**：右键放置投影、左键删除、中键拾取，不需要先在二维界面中逐项修改原理图。
- **与真实背包分离**：原版创造栏式虚拟物品栏、快捷栏和副手完全保存在客户端，不消耗或覆盖玩家物品。
- **任意方向扩展**：稀疏 `1x1x1` subregion 按编辑位置动态创建，适合从零草拟，也适合扩展已有 schematic。
- **独立 Creator Camera**：可在地面模式中按原版方式移动，也可切换飞行和穿墙；真实玩家本体维持独立的原版物理和服务端位置。
- **保留 Litematica 生态**：Creator 直接编辑标准 `LitematicaSchematic`，而不是维护无法互通的私有投影格式。
- **统一原理图管理**：管理器将当前查看项、Creator Focus 和 Litematica Selected Placement 分开处理，同时提供 metadata、缩略图、placement、文件绑定和 Litematica 原生页面入口。
- **可预测的文件语义**：保存、另存并绑定和导出副本互不混淆；四种 region 模式可在保留稀疏编辑拓扑、压缩 Creator cells 或生成完整外边界之间选择。
- **异常恢复与并发安全**：未保存内容使用标准 `.litematic` recovery generation 缓存；编辑事务、异步 schematic chunk 重建和渲染 world-view 快照按一致性边界隔离。

## 典型使用场景

- 在生存服务器现场设计建筑，再根据材料列表收集资源并照投影施工。
- 从空白开始快速搭建红石、装饰或结构布局草稿。
- 从任意旋转或镜像 placement 进入编辑，同步修正同一 schematic 的所有实例。
- 使用 Creator Camera 检查高处、地下或封闭空间中的结构关系，而不把相机移动和转向同步到真实玩家本体。
- 在 Creator 管理器中同时整理多个 schematics 和 placements，选择合适的 region 结构后保存并继续编辑同一个内存对象，或只导出一个不影响当前工作的副本。
- 在断线或客户端异常退出后恢复尚未正式保存的设计。

## 技术实现概览

1. 输入层拦截 Creator 模式中的使用和攻击操作，阻止真实世界交互。
2. `CreatorTargeting` 从当前 Creator Camera 统一执行真实世界与投影 `VoxelShape` 射线。
3. `CreatorFocus` 决定边界外操作要扩展哪个 schematic；placement 负责坐标、旋转和镜像，schematic 始终是数据真源。
4. `CreatorSchematicEditor` 写入现有 region，或按需创建稀疏 Creator cell，并同步同一 schematic 的全部 placements 与 metadata。
5. Litematica placement manager 重建实际受影响的 schematic chunks；同一区块重建与渲染编译同步，并在稳定快照中刷新 world view，不同区块仍可并行。
6. `CreatorRecoveryManager` 在客户端生成不可变快照，后台原子提交标准 `.litematic` generation 和 manifest。
7. `CreatorSchematicExportService` 在不可变快照上执行原样、稀疏压缩或外边界规范化；真实世界补入按选定 placement 反向映射，并在客户端线程分批采样。
8. `CreatorSchematicBindingService` 只在文件原子写入成功后更新当前 schematic 和全部 placements 的文件身份；重新加载原地协调 region 拓扑，不替换对象或破坏 Focus/Selected。
9. 可选模组兼容注册表在启动时记录版本并验证必要契约；只有 Tweakeroo 使用缓存后的私有反射桥接，Syncmatica、Lithium 和 Sodium 路径均不链接其私有 API。

这条链路使 Creator 能复用 Litematica 的数据与渲染体系，同时把真实玩家背包、真实世界状态和服务端数据包保持在编辑边界之外。

## 当前边界

多方块放置、通用 BlockState/NBT 编辑、投影交互、撤销/重做、普通方块式预览和实时多人同步仍属于后续工作。具体状态见 [`todo.md`](../todo.md)，完整设计见 [`creator-design-and-roadmap.md`](creator-design-and-roadmap.md)。
