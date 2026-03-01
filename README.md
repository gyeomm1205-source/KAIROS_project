# S14P21A506 Test Starter (Vue + Spring Boot + FastAPI)

## Modules
- `vue-fe`: `/test` integration console (ping checks + mockpost CRUD)
- `springboot-be`: test APIs, mockpost API, Swagger
- `fastapi-be`: ping APIs, Spring bridge ping

## Default ports
- Vue: `5173`
- Spring Boot: `8080`
- FastAPI: `8000`

## 1) Run FastAPI
```bash
cd fastapi-be
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export SPRING_BASE_URL=http://localhost:8080
export FASTAPI_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://kairos.cloud-ip.cc,http://kairos.cloud-ip.cc
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## 2) Run Spring Boot
```bash
cd springboot-be
chmod +x gradlew
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://127.0.0.1:5173,https://kairos.cloud-ip.cc,http://kairos.cloud-ip.cc
export APP_FASTAPI_BASE_URL=http://localhost:8000
./gradlew bootRun
```

## 3) Run Vue
```bash
cd vue-fe
cp .env.example .env
npm install
npm run dev
```

## Domain-ready config (`kairos.cloud-ip.cc`)
- Spring CORS default allows `https://kairos.cloud-ip.cc` and `http://kairos.cloud-ip.cc`.
- FastAPI CORS default allows `https://kairos.cloud-ip.cc` and `http://kairos.cloud-ip.cc`.
- Vue production example file: `vue-fe/.env.production.example`
- If reverse proxy routes differ by path, update:
  - `VITE_SPRING_BASE_URL`
  - `VITE_FASTAPI_BASE_URL`
  - `APP_FASTAPI_BASE_URL`
  - `SPRING_BASE_URL`

## Docs
- Spring Swagger: http://localhost:8080/swagger-ui/index.html
- FastAPI Swagger: http://localhost:8000/docs
- Vue Test Screen: http://localhost:5173/test

## Main APIs
- Spring Ping: `GET /api/test/ping`
- Spring -> FastAPI Ping: `GET /api/test/ping/fastapi`
- FastAPI Ping: `GET /api/test/ping`
- FastAPI -> Spring Ping: `GET /api/test/ping/spring`
- Spring MockPost CRUD: `/api/test/mock-posts`
