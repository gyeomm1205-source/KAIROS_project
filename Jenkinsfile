pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  parameters {
    string(name: 'BASE_DOMAIN', defaultValue: '221.138.211.197.nip.io', description: 'Ingress base domain (app/spring/fastapi host suffix)')
    string(name: 'TARGET_BRANCH', defaultValue: 'build-test', description: 'GitOps target branch')
  }

  environment {
    DOCKERHUB_NAMESPACE = 'gjaku1031'
    DOCKERHUB_REPOSITORY = 'ken-ssafy'
    VALUES_FILE = 'infra/helm/s14-app/values.yaml'

    // Jenkins Credentials IDs
    DOCKERHUB_CREDENTIALS_ID = 'dockerhub-cred'   // usernamePassword
    GITLAB_CREDENTIALS_ID    = 'gitlab-http-cred' // usernamePassword
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Resolve Variables') {
      steps {
        script {
          env.IMAGE_TAG = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
          env.IMAGE_REPO = "docker.io/${env.DOCKERHUB_NAMESPACE}/${env.DOCKERHUB_REPOSITORY}"
          env.GITOPS_BRANCH = env.BRANCH_NAME ?: params.TARGET_BRANCH

          echo "IMAGE_TAG=${env.IMAGE_TAG}"
          echo "IMAGE_REPO=${env.IMAGE_REPO}"
          echo "GITOPS_BRANCH=${env.GITOPS_BRANCH}"
        }
      }
    }

    stage('Checkout Target Branch') {
      steps {
        sh '''
          set -e
          git fetch origin --prune
          if git show-ref --verify --quiet "refs/remotes/origin/${GITOPS_BRANCH}"; then
            git checkout -B "${GITOPS_BRANCH}" "origin/${GITOPS_BRANCH}"
          else
            git checkout -B "${GITOPS_BRANCH}"
          fi
        '''
      }
    }

    stage('Docker Login') {
      steps {
        withCredentials([usernamePassword(credentialsId: "${env.DOCKERHUB_CREDENTIALS_ID}", usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
          sh '''
            set -e
            echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin
          '''
        }
      }
    }

    stage('Build & Push vue-fe') {
      steps {
        sh '''
          set -e
          docker build --pull \
            -t "${IMAGE_REPO}:vue-fe-${IMAGE_TAG}" \
            --build-arg VITE_SPRING_BASE_URL="http://spring.${BASE_DOMAIN}" \
            --build-arg VITE_FASTAPI_BASE_URL="http://fastapi.${BASE_DOMAIN}" \
            -f vue-fe/Dockerfile vue-fe
          docker push "${IMAGE_REPO}:vue-fe-${IMAGE_TAG}"
        '''
      }
    }

    stage('Build & Push springboot-be') {
      steps {
        sh '''
          set -e
          if ! command -v java >/dev/null 2>&1; then
            echo "ERROR: java not found. Install JDK 21 on Jenkins agent."
            exit 1
          fi
          java -version
          chmod +x springboot-be/gradlew
          ./springboot-be/gradlew -p springboot-be --no-daemon \
            bootBuildImage --imageName="${IMAGE_REPO}:springboot-be-${IMAGE_TAG}"
          docker push "${IMAGE_REPO}:springboot-be-${IMAGE_TAG}"
        '''
      }
    }

    stage('Build & Push fastapi-be') {
      steps {
        sh '''
          set -e
          docker build --pull \
            -t "${IMAGE_REPO}:fastapi-be-${IMAGE_TAG}" \
            -f fastapi-be/Dockerfile fastapi-be
          docker push "${IMAGE_REPO}:fastapi-be-${IMAGE_TAG}"
        '''
      }
    }

    stage('Update GitOps values.yaml & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: "${env.GITLAB_CREDENTIALS_ID}", usernameVariable: 'GITLAB_USERNAME', passwordVariable: 'GITLAB_TOKEN')]) {
          sh '''
            set -e

            sed -i "s|^baseDomain: .*|baseDomain: ${BASE_DOMAIN}|g" "$VALUES_FILE"
            sed -i "/vueFe:/,/springbootBe:/ s|repository: .*|repository: ${IMAGE_REPO}|g" "$VALUES_FILE"
            sed -i "/springbootBe:/,/fastapiBe:/ s|repository: .*|repository: ${IMAGE_REPO}|g" "$VALUES_FILE"
            sed -i "/fastapiBe:/,\$ s|repository: .*|repository: ${IMAGE_REPO}|g" "$VALUES_FILE"

            sed -i "/vueFe:/,/springbootBe:/ s|tag: .*|tag: vue-fe-${IMAGE_TAG}|g" "$VALUES_FILE"
            sed -i "/springbootBe:/,/fastapiBe:/ s|tag: .*|tag: springboot-be-${IMAGE_TAG}|g" "$VALUES_FILE"
            sed -i "/fastapiBe:/,\$ s|tag: .*|tag: fastapi-be-${IMAGE_TAG}|g" "$VALUES_FILE"

            if git diff --quiet -- "$VALUES_FILE"; then
              echo "No GitOps changes detected."
              exit 0
            fi

            git config user.name "jenkins"
            git config user.email "jenkins@local"
            git add "$VALUES_FILE"
            git commit -m "chore(helm): update image tags to ${IMAGE_TAG} [skip ci]"

            REMOTE_URL=$(git config --get remote.origin.url)
            REMOTE_URL="${REMOTE_URL%.git}"
            REMOTE_URL="${REMOTE_URL#https://}"
            REMOTE_URL="${REMOTE_URL#http://}"
            GIT_HOST="${REMOTE_URL%%/*}"
            GIT_PATH="${REMOTE_URL#*/}"

            git remote set-url origin "https://${GITLAB_USERNAME}:${GITLAB_TOKEN}@${GIT_HOST}/${GIT_PATH}.git"
            git push origin "HEAD:${GITOPS_BRANCH}"
          '''
        }
      }
    }
  }

  post {
    always {
      sh 'docker logout || true'
    }
  }
}
