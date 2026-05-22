# VSR 演讲稿 / Speaking Script — Lab 5: Room Integration

> **App 名称:** Transsistant(学习 / 翻译助手)
> **学生:** GU JINCHENG · A207420 · Instructor: Cikgu Izwan
>
> **用法:** 照着 **"🎤 念这段"** 部分念就行。`【中文】` 是给你自己看的提示和操作说明,**不要念出来**。
> 视频时间要求:概念讲解 ≈ 20 秒 + Demo ≈ 1 分钟。

---

## 录制前 Checklist

- [ ] Android Studio 打开本项目,启动模拟器或真机,App 已经跑起来
- [ ] 打开 **View → Tool Windows → App Inspection → Database Inspector**(等下要展示)
- [ ] 准备好录屏(OBS / 系统自带录屏),声音正常
- [ ] 先把 History 里的旧数据清掉一两条,留点空间好演示"新增"
- [ ] 编辑器里提前打开 `TranslationDao.kt` 和 `MainActivity.kt`(讲代码时要切过去)

---

## Part 1 — Room 概念讲解 (≈ 20 秒)

**🎤 念这段:**

> "Hi, I'm Gu Jincheng, matric A207420. In this lab I integrated **Room** into my app.
> Room is an **abstraction layer on top of SQLite**. Instead of writing raw SQL and managing cursors, I work with normal Kotlin objects.
> It has three core parts: an **Entity** — a data class mapped to a table; a **DAO** — the interface that defines the database operations; and the **Database** class that ties them together.
> The key benefit is **persistence**: the data survives even after the app is closed — something a ViewModel alone cannot do, because a ViewModel only keeps data in memory."

【提示:这段大约 20–22 秒,语速正常念刚好。重点说出 Entity / DAO / Database 三个词 + "persistence"。】

---

## Part 2 — 集成讲解 + Demo (≈ 1 分钟)

### ① 先讲架构(≈ 15 秒)

**🎤 念这段:**

> "My app follows the **MVVM + Repository** architecture.
> The flow is: the **UI** calls the **ViewModel**, the ViewModel calls a **Repository**, the Repository talks to the **DAO**, and Room turns that into SQLite operations.
> The ViewModel never touches the database directly — that keeps each layer clean and easy to test."

【操作:可以一边说一边在项目目录里指一下这几个文件:`MainActivity.kt`(UI+ViewModel)、`TranslationRepository.kt`、`TranslationDao.kt`、`AppDatabase.kt`。】

### ② 新增数据 + 确认存进了 Room(≈ 25 秒)

**🎤 念这段:**

> "Now let me add some data. I'll type a word here and tap the button."

【操作:在主页输入框输入一个词(比如 `Hello`)→ 点 **Translate**;
或进 **Add Word** 页输入 Word + Meaning → 点 **Save Word**。】

> "You can see it immediately shows up in the **History** screen."

【操作:点开 History(主页右下角 ➕ More → History),指着刚加的那条记录。】

> "Now the important part — proving it's really stored. Let me open the **Database Inspector**."

【操作:切到 Android Studio → Database Inspector → 选 `transsistant_database` → 点开 `history` 表。】

> "Here is my database `transsistant_database`, and inside the `history` table you can see the exact row I just added — with its **id**, **original** text, **result**, and **type**. The data is physically stored on the device."

### ③ 讲一小段代码(≈ 20 秒)

**🎤 念这段(切到 `TranslationDao.kt`):**

> "Let me explain one part of the code — my **DAO**.
> `getAll()` returns a **`Flow<List<TranslationData>>`**, so whenever the table changes, the new list is emitted automatically and the UI updates by itself.
> `insert()` and `deleteById()` are **`suspend`** functions, so Room runs them on a background thread — never blocking the UI."

**🎤 念这段(切到 `MainActivity.kt` 的 ViewModel):**

> "In the **ViewModel**, I expose that Flow as a **`StateFlow`** using `stateIn`, and the Compose UI collects it with `collectAsState`.
> Notice the ViewModel only calls the **Repository** — it never sees the DAO directly."

### ④ 收尾:证明持久化(≈ 10 秒,可选但很加分)

**🎤 念这段:**

> "Finally, to prove real persistence, I'll **close the app completely and reopen it** … and the data is still here. That's Room working — data survives an app restart."

【操作:从最近任务里划掉 App → 重新打开 → 进 History,数据还在。Database Inspector 里也能看到。】

> "Thank you for watching."

---

## Part 3 — 课堂 VSR 问答准备(老师可能问 1 个开放问题)

> 老师会回放你的视频,然后问 **一个** 关于代码的开放问题。下面是高频问题 + 简短答案,挑熟的记。

**Q1: 为什么用 Room 而不是直接用 SQLite?**
> Room 在 SQLite 之上做了封装:编译期检查 SQL、自动把查询结果映射成 Kotlin 对象、减少样板代码,还能直接配合协程和 Flow。比手写 SQL + Cursor 更安全、更省事。

**Q2: 为什么 `getAll()` 返回 `Flow`,而不是直接返回 `List`?**
> Flow 是可观察的数据流。表里数据一变,Room 就自动发出新的列表,UI 不用手动刷新就能更新——做到响应式 UI。

**Q3: Repository 这一层有什么用?能不能让 ViewModel 直接调 DAO?**
> 技术上可以,但 Repository 把"数据来源"和 ViewModel 解耦:ViewModel 不关心数据是来自 Room、网络还是缓存。这样分层清晰、好测试、好扩展(比如以后加网络数据源只改 Repository)。

**Q4: ViewModel 和 Room 在数据持久化上有什么区别?**
> ViewModel 只把数据存在内存里,App 进程被杀掉(或重启)数据就没了。Room 把数据写进设备上的 SQLite 文件,**关闭 App 后依然存在**。这正是这次 Lab 升级的核心。

**Q5: `@PrimaryKey(autoGenerate = true)` 是做什么的?**
> 把 `id` 设为主键,并让 Room/SQLite 自动递增生成,不用我手动指定,保证每条记录唯一。

**Q6: 为什么 DAO 的 `insert` / `deleteById` 是 `suspend` 函数?**
> 数据库操作不能在主线程跑(会卡 UI 甚至崩溃)。`suspend` 让 Room 自动切到后台线程执行,我在 ViewModel 里用 `viewModelScope.launch` 调用即可。

**Q7: `stateIn` / `StateFlow` 和原来的 `mutableStateListOf` 有什么不同?**
> `StateFlow` 是冷流 `Flow` 转成的热流,直接连着数据库的 Flow,是"单一数据源";`stateIn` 还提供初始值并在有订阅者时才收集,更省资源。UI 用 `collectAsState()` 订阅即可。

---

## 附:本项目对照 Tutorial 必需组件(自检表)

| 组件 | 文件 | 状态 |
|------|------|------|
| Entity | `MainActivity.kt` → `TranslationData` (`@Entity`) | ✅ |
| DAO(insert / getAll 返回 Flow / delete) | `TranslationDao.kt` | ✅ |
| Database(单例 + 含 DAO) | `AppDatabase.kt` + `AppApplication.kt` | ✅ |
| Repository(连接 ViewModel ↔ DAO) | `TranslationRepository.kt` | ✅ |
| ViewModel(调 Repository + 暴露 StateFlow) | `MainActivity.kt` → `TranssistantViewModel` | ✅ |
| 数据持久化(关 App 仍在) | Room 写入 `transsistant_database` | ✅ |
