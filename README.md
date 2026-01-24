# JinkOps

一個以「可上線標準」設計的通用後台系統  
涵蓋認證、權限、快取、審計日誌、非同步處理與搜尋降級等企業級後端能力

---

## 專案簡介

JinkOps 是一個通用後台系統示範專案，  
聚焦後端系統中常見但容易被忽略的工程問題，例如：

- 認證與權限邊界劃分
- 快取一致性與併發安全
- 操作審計與日誌可追蹤性
- 非同步處理與系統穩定性
- 搜尋系統失效時的降級策略

本專案定位為「可直接展示與說明的後端實作範例」，  
整體設計與技術選型皆以實際上線場景為前提，而非僅止於功能實現。

---

## 技術棧

- Java / Spring Boot
- Spring Security + JWT
- RBAC 權限模型（User / Role / Permission）
- Redis（資料快取、權限快取）
- Redisson（分散式鎖）
- RabbitMQ（事件通知、操作日誌）
- Quartz（定時掃描）
- Elasticsearch（日誌搜尋，MySQL 降級）
- MySQL / JPA

---

## 系統架構與設計說明

### 認證機制

系統採用 JWT 進行身分驗證。  
使用者登入後取得 token，後續請求由 Security Filter 解析並注入安全上下文。

### RBAC 權限控制

透過自訂的 `@RequirePermission` 註解與 AOP 切面進行權限校驗：

- 未登入請求回傳 UNAUTHORIZED
- 已登入但權限不足回傳 FORBIDDEN

權限資料由 SecurityContext 提供，避免重複查詢資料庫。

### 操作日誌

所有重要寫操作皆透過 `@OperationLog` AOP 進行攔截：

- 同步寫入 MySQL，確保審計完整性
- 非同步透過 MQ 發送事件
- 查詢時優先使用 Elasticsearch
- ES 發生異常或查無結果時自動降級至 MySQL

### 快取策略

User 模組屬於讀多寫少場景，採用 Cache Aside 模式：

- 查詢未命中時回 DB 並回寫快取
- 使用空值快取防止快取穿透
- 隨機 TTL 降低快取雪崩風險

### 併發控制

使用 Redisson 分散式鎖處理併發敏感的寫操作，例如使用者建立。  
鎖僅存在於必要寫路徑，不影響一般讀取效能。

### MQ 與 Quartz 邊界

- RabbitMQ 僅用於後置通知與事件處理
- Quartz 僅負責定期只讀掃描（如日誌檢查）
- 兩者皆不參與任何核心寫入流程，避免影響系統一致性

---

## 功能模組一覽

- Auth：登入、Token 驗證
- User：CRUD、快取、分頁查詢
- RBAC：角色、權限與關聯管理
- OperationLog：AOP 審計、日誌查詢、搜尋與降級
- Cache / Lock：內部基礎模組（無對外 API）
- MQ / Quartz：後置事件處理與定時掃描

---

## API 使用方式

本專案提供完整的 Postman Collection 供測試與展示。

使用方式如下：

1. 將 Postman Collection 匯入 Postman
2. 設定環境變數 `baseUrl`（例如 http://localhost:8080）
3. 呼叫 Auth / Login
   - Tests 會自動將 token 寫入環境變數
4. 後續受保護 API 會自動帶上 Authorization Header

典型操作流程為：

Login → User 操作 → RBAC 綁定 → OperationLog 查詢

---

## 快速啟動

### 環境需求

- JDK 17 以上
- MySQL
- Redis
- RabbitMQ
- Elasticsearch

### 設定說明（Profiles）

- 共用設定：`src/main/resources/application.yml`
- 開發：`application-dev.yml`（預設啟用，可用 `--spring.profiles.active=dev` 覆寫）
- 生產：`application-prod.yml`
- 需透過環境變數或 `.env` 提供：
  - `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`
  - `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`（可選）
  - `RABBIT_HOST` / `RABBIT_PORT` / `RABBIT_USER` / `RABBIT_PASSWORD`
  - `ELASTICSEARCH_HOST` / `ELASTICSEARCH_PORT`
  - `APP_JWT_SECRET`（至少 32 字元，與 `APP_JWT_EXPIRATION` 可共同控制 token）

### 啟動方式

使用 Maven：

- 開發：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- 生產：`java -jar app.jar --spring.profiles.active=prod`

或於 IDE 中載入 Maven 專案後直接啟動 `JinkOpsApplication`，並在 VM options 指定 `-Dspring.profiles.active=dev` / `prod`。

---

## 專案設計原則

- Controller 輕量化，只負責接參與回應
- 業務邏輯集中於 Service 層
- 錯誤統一由 ErrorCode、BizException 與 GlobalExceptionHandler 處理
- 快取採用 Cache Aside 策略
- 分散式鎖僅用於必要寫路徑
- MQ 與 Quartz 僅處理後置與非一致性流程
- 搜尋主用 Elasticsearch，失效時自動降級 MySQL

---

## 專案狀態說明

本專案用於學習與展示後端系統設計能力，  
整體架構與實作方式以「可上線標準」為目標，
