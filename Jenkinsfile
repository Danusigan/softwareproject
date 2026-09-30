// CI/CD for LO-PO Analytics: test -> build images -> push to Docker Hub -> deploy to EC2.
// Triggered by the GitHub push webhook. Server setup: infra/README.md.
//
// Jenkins needs (all created by infra/ansible):
//   - Docker on the agent, jenkins user in the docker group
//   - Ansible on the agent + an SSH key the app server trusts
//   - APP_HOST env var = app server's private IP
// and one credential added by hand in the UI:
//   - 'dockerhub' (Username with password: Docker Hub user + access token)

pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
    }

    environment {
        DOCKERHUB_NAMESPACE = 'danusigan'
        BACKEND_IMAGE  = "${DOCKERHUB_NAMESPACE}/lopo-backend"
        FRONTEND_IMAGE = "${DOCKERHUB_NAMESPACE}/lopo-frontend"
        CI_NAME = "lopo-ci-${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                }
            }
        }

        stage('Tests') {
            parallel {
                // Integration tests need a real MySQL - a throwaway container, deleted afterwards.
                // It never touches production data (that's on RDS).
                stage('Backend') {
                    steps {
                        sh '''
                            docker network create "$CI_NAME"
                            docker run -d --name "$CI_NAME-mysql" --network "$CI_NAME" \
                                -e MYSQL_ROOT_PASSWORD=ci_password -e MYSQL_DATABASE=test_db mysql:8.0

                            for i in $(seq 1 60); do
                                docker exec "$CI_NAME-mysql" mysqladmin ping -h 127.0.0.1 -pci_password --silent && break
                                sleep 2
                            done

                            mkdir -p "$HOME/.m2"
                            docker run --rm --network "$CI_NAME" \
                                -u "$(id -u):$(id -g)" \
                                -v "$PWD/Software-project-Backend:/app" -w /app \
                                -v "$HOME/.m2:/var/maven/.m2" -e MAVEN_CONFIG=/var/maven/.m2 \
                                -e DB_HOST="$CI_NAME-mysql" -e DB_PORT=3306 -e DB_NAME=test_db \
                                -e DB_USERNAME=root -e DB_PASSWORD=ci_password \
                                maven:3.9.9-eclipse-temurin-17 \
                                mvn -B -Duser.home=/var/maven verify
                        '''
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'Software-project-Backend/target/surefire-reports/*.xml'
                            sh 'docker rm -f "$CI_NAME-mysql" || true; docker network rm "$CI_NAME" || true'
                        }
                    }
                }

                stage('Frontend') {
                    steps {
                        sh '''
                            docker run --rm -u "$(id -u):$(id -g)" \
                                -v "$PWD/softwareproject_frontend:/app" -w /app \
                                -e HOME=/tmp -e npm_config_cache=/tmp/.npm \
                                node:22-alpine \
                                sh -c "npm ci && npm test && npm run build"
                        '''
                    }
                }
            }
        }

        stage('Build & push images') {
            when { expression { isMain() } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_TOKEN')]) {
                    sh '''
                        echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin

                        docker build -t "$BACKEND_IMAGE:$IMAGE_TAG" -t "$BACKEND_IMAGE:latest" Software-project-Backend
                        docker build -t "$FRONTEND_IMAGE:$IMAGE_TAG" -t "$FRONTEND_IMAGE:latest" softwareproject_frontend

                        docker push "$BACKEND_IMAGE:$IMAGE_TAG"
                        docker push "$BACKEND_IMAGE:latest"
                        docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
                        docker push "$FRONTEND_IMAGE:latest"
                    '''
                }
            }
            post {
                always { sh 'docker logout || true' }
            }
        }

        stage('Deploy') {
            when { expression { isMain() } }
            steps {
                sh '''
                    if [ -z "$APP_HOST" ]; then
                        echo "APP_HOST is not set - run infra/ansible/site.yml against the Jenkins server" >&2
                        exit 1
                    fi
                    cd infra/ansible
                    ansible-playbook -i "$APP_HOST," -u ubuntu deploy.yml -e image_tag="$IMAGE_TAG"
                '''
            }
        }
    }

    post {
        success { echo "Deployed ${env.IMAGE_TAG}" }
        always {
            // Build-host housekeeping: dangling layers from image builds.
            sh 'docker image prune -f || true'
        }
    }
}

// Plain Pipeline jobs expose the branch as GIT_BRANCH ("origin/main"); multibranch jobs as BRANCH_NAME.
def isMain() {
    return env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main'
}
