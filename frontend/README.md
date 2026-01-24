# JinkOps Frontend

> 本 README 由 AI（ChatGPT）撰寫，配合後端說明呈現前端能力。

---

## 專案簡介

JinkOps 前端是一個以 Vue 3 + Vite 構建的單頁應用，聚焦將後端的認證、授權、審計、緩存、降級等能力「可視化、可驗證、可演示」。介面與流程對齊後端範例：登入取得 JWT、基於 RBAC 控制頁面與按鈕、操作日誌檢索與降級展示，全程由 AI 按照後端寫法整理而成。

---

## 技術棧

- Vue 3 + TypeScript + Vite
- Element Plus（UI）+ @element-plus/icons-vue
- Pinia（狀態管理）
- Vue Router（路由守衛 + Meta 權限）
- Axios（攔截器統一處理 401/403 與錯誤提示）
- dayjs（時間處理）

---

## 主要頁面與功能

- 登入：表單校驗、請求 `/api/auth/login`，成功後寫入 localStorage 並拉取使用者詳情。
- 能力總覽：Token 預覽/校驗/複製、後端能力對齊卡片、快捷入口（用戶/角色/權限、操作日誌）、鏈路健康狀態展示。
- 用戶管理：列表、搜尋、分頁、建立/編輯用戶，並可分配角色。
- 角色管理：角色 CRUD、角色權限綁定，僅 `requiresAdmin` 角色可見。
- 權限管理：權限碼 CRUD，支援分頁與搜尋。
- 操作日誌：依關鍵字、時間區間、使用者過濾；展示 traceId，支援分頁查看。

---

## 設計與交互要點

- 路由守衛：`meta.requiresAuth` 與 `meta.requiresAdmin` 控制訪問；未登入跳轉 `/login`，無權限回退並提示。
- 權限判斷：`useAuthStore().hasPermission` 統一按鈕/菜單可見性，內建 `ROLE_*` 大寫化處理與 Admin 快捷放行。
- 請求攔截：自動附帶 `Authorization: Bearer <token>`，後端 `code != 200` 或 HTTP 401/403 時彈出提示並清理本地登入態。
- 狀態持久：token、username 落地 localStorage，刷新後自動 bootstrap 拉取使用者、角色、權限。
- UI 風格：玻璃態卡片 + Element Plus 組件，與後端能力對應的標籤、徽章、流程卡片。

---

## 目錄導覽

- `src/api/`：Axios 客戶端、端點定義、模型類型（`client.ts`、`endpoints.ts` 等）。
- `src/store/auth.ts`：登入、bootstrap、權限判斷與全域單例存取。
- `src/router/index.ts`：路由表與全域守衛。
- `src/views/`：頁面組件（Login、Overview、Users、Roles、Permissions、OperationLogs）。
- `src/layouts/MainLayout.vue`：側邊菜單、麵包屑區塊與登出行為。
- `src/styles/`：主題變數與全局樣式。

---

## 環境變數

- 請依 `./.env.example` 建立 `.env`，並設定 `VITE_API_BASE_URL` 指向後端（例：`http://localhost:8080`）。
- 其他變數依預設即可，若後端端口或上下文路徑調整，需同步更新。

---

## 快速啟動

1) 安裝依賴（需 Node.js 18+）  
   `cd frontend && npm install`
2) 開發模式  
   `npm run dev`，預設使用 Vite 開啟本地開發伺服器。
3) 構建與預覽  
   `npm run build` 產出 `dist/`；`npm run preview` 以本地伺服器預覽產物。

---

## 與後端對齊

- API 預設遵循後端的 `/api/**` 路徑與 `code / message / data` 響應格式。
- 頁面與操作對映後端能力：JWT 校驗、RBAC 鑑權、Redis 緩存命中展示、Redisson 鎖、RabbitMQ / Quartz / Elasticsearch 降級流程皆有對應說明卡片或列表。
- 體驗重點在於「可展示、可驗證」，若後端改動請同步更新 `src/api/endpoints.ts` 與相關表單校驗。

---

## 小結

這份前端 README 由 AI 根據後端寫法自動整理，目的在快速理解並啟動介面演示。如需調整內容或補充案例，可直接修改本檔或提交 Issue。
