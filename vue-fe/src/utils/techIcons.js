/**
 * 기술 스택 소문자 키 → 정규 표기 이름 매핑
 * 입력 시 대소문자 무관하게 올바른 표기로 정규화합니다.
 */
const DISPLAY_NAME_MAP = {
  // ── Languages ──
  javascript:            'JavaScript',
  js:                    'JavaScript',
  typescript:            'TypeScript',
  ts:                    'TypeScript',
  python:                'Python',
  java:                  'Java',
  'c++':                 'C++',
  cpp:                   'C++',
  'c#':                  'C#',
  csharp:                'C#',
  go:                    'Go',
  golang:                'Go',
  rust:                  'Rust',
  ruby:                  'Ruby',
  php:                   'PHP',
  swift:                 'Swift',
  kotlin:                'Kotlin',
  dart:                  'Dart',
  'objective-c':         'Objective-C',
  objc:                  'Objective-C',
  scala:                 'Scala',
  elixir:                'Elixir',
  haskell:               'Haskell',
  c:                     'C',

  // ── Frontend ──
  react:                 'React',
  'vue.js':              'Vue.js',
  vuejs:                 'Vue.js',
  vue:                   'Vue.js',
  angular:               'Angular',
  svelte:                'Svelte',
  'next.js':             'Next.js',
  nextjs:                'Next.js',
  next:                  'Next.js',
  'nuxt.js':             'Nuxt.js',
  nuxtjs:                'Nuxt.js',
  nuxt:                  'Nuxt.js',
  html5:                 'HTML5',
  html:                  'HTML5',
  css3:                  'CSS3',
  css:                   'CSS3',
  'tailwind css':        'Tailwind CSS',
  tailwind:              'Tailwind CSS',
  tailwindcss:           'Tailwind CSS',
  bootstrap:             'Bootstrap',
  jquery:                'jQuery',
  redux:                 'Redux',
  zustand:               'Zustand',
  remix:                 'Remix',
  gatsby:                'Gatsby',
  sass:                  'Sass',
  scss:                  'Sass',

  // ── Backend ──
  'node.js':             'Node.js',
  nodejs:                'Node.js',
  node:                  'Node.js',
  'express.js':          'Express',
  express:               'Express',
  nestjs:                'NestJS',
  'nest.js':             'NestJS',
  nest:                  'NestJS',
  'spring boot':         'Spring Boot',
  spring:                'Spring Boot',
  django:                'Django',
  flask:                 'Flask',
  'ruby on rails':       'Ruby on Rails',
  rails:                 'Ruby on Rails',
  ror:                   'Ruby on Rails',
  laravel:               'Laravel',
  fastapi:               'FastAPI',

  // ── Database ──
  mysql:                 'MySQL',
  postgresql:            'PostgreSQL',
  postgres:              'PostgreSQL',
  mongodb:               'MongoDB',
  redis:                 'Redis',
  sqlite:                'SQLite',
  mariadb:               'MariaDB',
  oracle:                'Oracle',
  graphql:               'GraphQL',
  elasticsearch:         'Elasticsearch',
  cassandra:             'Cassandra',

  // ── DevOps / Tools ──
  devops:                'DevOps',
  git:                   'Git',
  github:                'GitHub',
  gitlab:                'GitLab',
  docker:                'Docker',
  kubernetes:            'Kubernetes',
  k8s:                   'Kubernetes',
  aws:                   'AWS',
  'amazon web services': 'AWS',
  gcp:                   'GCP',
  'google cloud':        'GCP',
  azure:                 'Azure',
  figma:                 'Figma',
  linux:                 'Linux',
  ubuntu:                'Ubuntu',
  nginx:                 'NGINX',
  apache:                'Apache',
  'haproxy':             'HAProxy',
  traefik:               'Traefik',

  // ── Mobile ──
  'react native':        'React Native',
  flutter:               'Flutter',
  ios:                   'iOS',
  android:               'Android',

  // ── CI/CD ──
  jenkins:               'Jenkins',
  'github actions':      'GitHub Actions',
  'gitlab ci':           'GitLab CI',
  'gitlab ci/cd':        'GitLab CI/CD',
  circleci:              'CircleCI',
  'travis ci':           'Travis CI',
  'argo cd':             'Argo CD',
  argocd:                'Argo CD',
  'bitbucket pipelines': 'Bitbucket Pipelines',

  // ── IaC ──
  terraform:             'Terraform',
  ansible:               'Ansible',
  chef:                  'Chef',
  puppet:                'Puppet',
  pulumi:                'Pulumi',
  cloudformation:        'CloudFormation',

  // ── Monitoring & Logging ──
  prometheus:            'Prometheus',
  grafana:               'Grafana',
  datadog:               'Datadog',
  logstash:              'Logstash',
  kibana:                'Kibana',
  'elk stack':           'ELK Stack',
  elk:                   'ELK Stack',
  splunk:                'Splunk',
  'new relic':           'New Relic',

  // ── Tools ──
  webpack:               'Webpack',
  babel:                 'Babel',
  jest:                  'Jest',
  vscode:                'VS Code',
  'vs code':             'VS Code',
  vim:                   'Vim',
  bitbucket:             'Bitbucket',
  xd:                    'XD',
}

/**
 * 태그 이름을 올바른 표기로 정규화합니다. (대소문자 무관)
 * 매핑에 없으면 원본 그대로 반환합니다.
 */
export function normalizeTechName(name) {
  if (!name) return name
  const key = name.trim().toLowerCase()
  return DISPLAY_NAME_MAP[key] ?? name.trim()
}

/**
 * 기술 스택 이름 → Devicon CSS 클래스 매핑
 * 매핑 없는 경우 Font Awesome 'fas fa-code' fallback
 * ※ 키는 정규화된 표기 기준 소문자로 통일
 */
const TECH_ICON_MAP = {
  // ── Languages ──
  javascript:            'devicon-javascript-plain colored',
  typescript:            'devicon-typescript-plain colored',
  python:                'devicon-python-plain colored',
  java:                  'devicon-java-plain colored',
  kotlin:                'devicon-kotlin-plain colored',
  swift:                 'devicon-swift-plain colored',
  go:                    'devicon-go-plain colored',
  rust:                  'devicon-rust-plain',
  'c++':                 'devicon-cplusplus-plain colored',
  cpp:                   'devicon-cplusplus-plain colored',
  'c#':                  'devicon-csharp-plain colored',
  csharp:                'devicon-csharp-plain colored',
  c:                     'devicon-c-plain colored',
  php:                   'devicon-php-plain colored',
  ruby:                  'devicon-ruby-plain colored',
  scala:                 'devicon-scala-plain colored',
  dart:                  'devicon-dart-plain colored',
  elixir:                'devicon-elixir-plain colored',
  haskell:               'devicon-haskell-plain colored',

  // ── Frontend ──
  react:                 'devicon-react-original colored',
  'vue.js':              'devicon-vuejs-plain colored',
  vuejs:                 'devicon-vuejs-plain colored',
  vue:                   'devicon-vuejs-plain colored',
  angular:               'devicon-angularjs-plain colored',
  svelte:                'devicon-svelte-plain colored',
  'next.js':             'devicon-nextjs-plain',
  nextjs:                'devicon-nextjs-plain',
  'nuxt.js':             'devicon-nuxtjs-plain colored',
  nuxtjs:                'devicon-nuxtjs-plain colored',
  nuxt:                  'devicon-nuxtjs-plain colored',
  html5:                 'devicon-html5-plain colored',
  html:                  'devicon-html5-plain colored',
  css3:                  'devicon-css3-plain colored',
  css:                   'devicon-css3-plain colored',
  tailwind:              'devicon-tailwindcss-plain colored',
  tailwindcss:           'devicon-tailwindcss-plain colored',
  'tailwind css':        'devicon-tailwindcss-plain colored',
  bootstrap:             'devicon-bootstrap-plain colored',
  jquery:                'devicon-jquery-plain colored',
  redux:                 'devicon-redux-original colored',
  sass:                  'devicon-sass-original colored',
  scss:                  'devicon-sass-original colored',
  remix:                 'devicon-remix-plain',
  gatsby:                'devicon-gatsby-plain colored',

  // ── Backend ──
  'node.js':             'devicon-nodejs-plain colored',
  nodejs:                'devicon-nodejs-plain colored',
  node:                  'devicon-nodejs-plain colored',
  express:               'devicon-express-original',
  'express.js':          'devicon-express-original',
  nestjs:                'devicon-nestjs-plain colored',
  'nest.js':             'devicon-nestjs-plain colored',
  'spring boot':         'devicon-spring-plain colored',
  spring:                'devicon-spring-plain colored',
  django:                'devicon-django-plain colored',
  flask:                 'devicon-flask-original',
  laravel:               'devicon-laravel-plain colored',
  rails:                 'devicon-rails-plain colored',
  'ruby on rails':       'devicon-rails-plain colored',
  fastapi:               'devicon-fastapi-plain colored',

  // ── Database ──
  mysql:                 'devicon-mysql-plain colored',
  postgresql:            'devicon-postgresql-plain colored',
  postgres:              'devicon-postgresql-plain colored',
  mongodb:               'devicon-mongodb-plain colored',
  redis:                 'devicon-redis-plain colored',
  sqlite:                'devicon-sqlite-plain colored',
  elasticsearch:         'devicon-elasticsearch-plain colored',
  cassandra:             'devicon-cassandra-plain colored',
  graphql:               'devicon-graphql-plain colored',

  // ── DevOps / Cloud ──
  devops:                'fas fa-infinity',
  docker:                'devicon-docker-plain colored',
  kubernetes:            'devicon-kubernetes-plain colored',
  k8s:                   'devicon-kubernetes-plain colored',
  aws:                   'devicon-amazonwebservices-plain-wordmark colored',
  gcp:                   'devicon-googlecloud-plain colored',
  azure:                 'devicon-azure-plain colored',
  terraform:             'devicon-terraform-plain colored',
  ansible:               'devicon-ansible-plain colored',
  jenkins:               'devicon-jenkins-plain colored',
  nginx:                 'devicon-nginx-original colored',
  linux:                 'devicon-linux-plain colored',
  ubuntu:                'devicon-ubuntu-plain colored',

  // ── Version Control ──
  git:                   'devicon-git-plain colored',
  github:                'devicon-github-original',
  gitlab:                'devicon-gitlab-plain colored',
  bitbucket:             'devicon-bitbucket-original colored',

  // ── Mobile ──
  'react native':        'devicon-react-original colored',
  flutter:               'devicon-flutter-plain colored',
  android:               'devicon-android-plain colored',

  // ── CI/CD ──
  circleci:              'devicon-circleci-plain colored',

  // ── Monitoring ──
  grafana:               'devicon-grafana-plain colored',
  prometheus:            'devicon-prometheus-plain colored',

  // ── Tools ──
  figma:                 'devicon-figma-plain colored',
  webpack:               'devicon-webpack-plain colored',
  babel:                 'devicon-babel-plain colored',
  jest:                  'devicon-jest-plain colored',
  vscode:                'devicon-vscode-plain colored',
  vim:                   'devicon-vim-plain colored',
}

const FALLBACK = 'fas fa-code'

/**
 * 기술 스택 이름으로 아이콘 클래스를 반환합니다.
 * @param {string} name 기술 스택 이름 (대소문자 무관)
 * @returns {string} Devicon 또는 Font Awesome CSS 클래스
 */
export function getTechIcon(name) {
  if (!name) return FALLBACK
  const key = name.toLowerCase().trim()
  return TECH_ICON_MAP[key] ?? FALLBACK
}

/**
 * Devicon 아이콘이 있는 기술인지 여부
 */
export function hasTechIcon(name) {
  if (!name) return false
  return TECH_ICON_MAP[name.toLowerCase().trim()] !== undefined
}

/**
 * 입력 문자열로 자동완성 후보 목록 반환 (최대 limit개)
 * 키(alias) 또는 표기명 모두 매칭합니다.
 */
export function getSuggestions(input, limit = 8) {
  if (!input || input.trim().length < 1) return []
  const q = input.trim().toLowerCase()
  const seen = new Set()
  const results = []
  for (const [key, displayName] of Object.entries(DISPLAY_NAME_MAP)) {
    if (seen.has(displayName)) continue
    if (key.includes(q) || displayName.toLowerCase().includes(q)) {
      seen.add(displayName)
      results.push(displayName)
      if (results.length >= limit) break
    }
  }
  return results
}
