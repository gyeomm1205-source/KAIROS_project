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

## Kubernetes GitOps (Helm)

- Helm chart path: `infra/helm/s14-app`
- ArgoCD application manifest: `infra/argocd/app-build-test.yaml`
- CI/CD pipeline: `.gitlab-ci.yml` (self-hosted runner: other-vm)
- 배포 기준: Helm chart + ArgoCD (kustomize 미사용, `infra/k8s` 제거)
- Spring Boot 이미지 빌드: Dockerfile 대신 `bootBuildImage` 사용 (CI에서 실행)
- Docker Hub private 저장소 기준: `ssafy`의 `dockerhub-regcred` + values `imagePullSecrets` 설정 필수

Quick check:

```bash
helm template s14-app infra/helm/s14-app --namespace ssafy | head -n 40
```
