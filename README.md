# S14P21A506 Starter (Vue + Spring Boot + FastAPI)

## Modules
- `vue-fe`: 프론트엔드
- `springboot-be`: MySQL + Redis 연동 Spring Boot API
- `fastapi-be`: Spring 내부 AI bridge + Qdrant 연동 FastAPI API
  FastAPI는 MySQL/Redis를 직접 조회하지 않고, 외부 ingress로 직접 노출하지 않는다.
  Qdrant Cloud endpoint와 API key는 코드에 하드코딩되어 있다.
- `infra`: 로컬 인프라 compose, Helm chart, ArgoCD manifest

## Env policy
- Spring Boot와 Vue는 로컬용 `.env`와 템플릿용 `.env.example`를 사용한다.
- 배포용 실제값은 `infra/env/.env.spring.prod`를 원본으로 사용한다.
- Spring Boot는 `.env`, `infra/env/.env.spring.prod`, `.env.example` 키 구조를 같게 유지한다.
- 저장소에는 `.env.example`만 커밋한다.
- 배포용 prod 파일은 GitLab CI/CD file variable이나 bastion 서버의 비밀 경로에서 관리한다.
- FastAPI는 별도 env 없이 코드에 고정된 Qdrant Cloud 설정을 사용한다.

초기 파일 준비:

```bash
cd /home/ken/project/ssafy/a506
cp springboot-be/.env.example springboot-be/.env
cp vue-fe/.env.example vue-fe/.env
```

배포용 파일 예시:

```bash
mkdir -p infra/env
cp springboot-be/.env.example infra/env/.env.spring.prod
```

## Default ports
- Vue: `5173`
- Spring Boot: `8080`
- FastAPI: `8000`
- MySQL: `3306`
- Redis: `6379`

## 1) Start local infra

```bash
cd /home/ken/project/ssafy/a506/infra
docker compose up -d
docker compose ps
```

이 compose는 MySQL, Redis만 띄운다.
값은 compose 파일에 고정되어 있다.
Spring Boot는 호스트에서 `.env`를 읽어 실행한다.
FastAPI는 별도 env 없이 코드에 하드코딩된 Qdrant Cloud를 사용한다.
기준 로컬 환경은 WSL Ubuntu 셸이다.
즉 `docker compose`, 백엔드 실행 명령, `npm run dev`를 모두 같은 WSL Ubuntu 안에서 실행하는 것을 전제로 한다.
이 경우 백엔드 `.env`의 `127.0.0.1`/`localhost` 값은 그대로 사용하면 된다.

## 2) Run Spring Boot

```bash
cd /home/ken/project/ssafy/a506/springboot-be
./gradlew bootRun
```

`springboot-be/.env`는 Spring Boot가 직접 읽는다.
IDE에서 실행할 때도 `springboot-be`를 working directory로 잡아두면 `.env`를 자동으로 읽는다.

## 3) Run FastAPI

```bash
cd /home/ken/project/ssafy/a506/fastapi-be
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## 4) Run Vue

```bash
cd /home/ken/project/ssafy/a506/vue-fe
npm install
npm run dev
```

## K8s env workflow
- Helm chart path: `infra/helm/s14-app`
- ArgoCD application manifest: `infra/argocd/app-develop.yaml`
- Spring Deployment만 `springboot-be-env` Secret을 읽는다.
- Helm은 Spring env 값을 갖지 않고 고정된 이름의 Secret만 참조한다.
- 실제 값은 GitLab file variable의 Spring prod env 파일에서 CI job이 바로 Secret으로 만든다.
- ingress는 `kairos.<domain>`, `spring.<domain>`만 외부에 열고 FastAPI는 `ClusterIP` 내부 서비스로만 둔다.
- FastAPI의 Qdrant Cloud endpoint와 API key는 코드에 하드코딩되어 있으므로 별도 Secret을 만들지 않는다.
- GitLab에서는 file variable을 권장한다.
  - `SPRING_ENV_PROD_FILE`: 업로드 원본 파일 `infra/env/.env.spring.prod`
  - `KUBE_CONFIG_FILE`: kubeconfig 파일 변수

수동 적용 예시:

```bash
cd /home/ken/project/ssafy/a506

kubectl create secret generic springboot-be-env -n ssafy \
  --from-env-file=infra/env/.env.spring.prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl rollout restart deployment/springboot-be -n ssafy
```

GitLab CI job 안에서 file variable을 쓰는 예시:

```bash
export KUBECONFIG="$KUBE_CONFIG_FILE"

kubectl create namespace ssafy --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic springboot-be-env -n ssafy \
  --from-env-file="$SPRING_ENV_PROD_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Docs
- Spring Swagger: http://localhost:8080/swagger-ui/index.html
- FastAPI Swagger: http://localhost:8000/docs
  로컬 실행 또는 `kubectl port-forward svc/fastapi-be 8000:8000 -n ssafy`로만 확인
- Vue: http://localhost:5173
- K3s v3 tutorial: `/home/ken/project/ssafy/ken-infra/k3s/v3`

## Main APIs
- Spring Ping: `GET /api/test/ping`
- Spring -> FastAPI Ping: `GET /api/test/ping/fastapi`
- Spring Infrastructure Check: `GET /api/test/ping/infrastructure`
  MySQL, Redis, FastAPI, Qdrant 상태를 한 번에 확인한다.
- Spring MockPost CRUD: `/api/test/mock-posts`
- FastAPI Ping: `GET /api/test/ping`
  내부 확인용이다. 로컬 또는 port-forward에서만 직접 호출한다.
- FastAPI Qdrant Ping: `GET /api/test/qdrant`
  내부 확인용이다. 로컬 또는 port-forward에서만 직접 호출한다.
