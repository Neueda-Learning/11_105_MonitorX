pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        APP_URL = 'http://localhost:8081'
        COMPOSE_PROJECT_NAME = 'monitorx'
        COMPOSE_FILES = '-f docker-compose.yml -f docker-compose.jenkins.yml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Preflight') {
            steps {
                script {
                    if (sh(returnStatus: true, script: 'docker compose version > /dev/null 2>&1') == 0) {
                        env.COMPOSE_CMD = 'docker compose'
                    } else if (sh(returnStatus: true, script: 'docker-compose version > /dev/null 2>&1') == 0) {
                        env.COMPOSE_CMD = 'docker-compose'
                    } else {
                        error('Docker Compose is not installed. Install docker compose plugin or docker-compose binary.')
                    }
                }

                sh '''#!/bin/bash
                    set -euo pipefail
                    chmod +x mvnw
                    docker --version
                    ${COMPOSE_CMD} version
                    ./mvnw -v
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh '''#!/bin/bash
                    set -euo pipefail
                    ./mvnw -B clean verify
                '''
            }
        }

        stage('Prepare Compose Override') {
            steps {
                writeFile file: 'docker-compose.jenkins.yml', text: '''services:
  db:
    ports: []
'''
            }
        }

        stage('Docker Build') {
            steps {
                sh '''#!/bin/bash
                    set -euo pipefail
                    ${COMPOSE_CMD} ${COMPOSE_FILES} build --pull
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''#!/bin/bash
                    set -euo pipefail
                    ${COMPOSE_CMD} ${COMPOSE_FILES} up -d --remove-orphans
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''#!/bin/bash
                    set -euo pipefail

                    for i in {1..30}; do
                      if command -v curl >/dev/null 2>&1; then
                        if curl -fsS ${APP_URL}/ > /dev/null && curl -fsS ${APP_URL}/api/summary > /dev/null; then
                          echo "Application is healthy"
                          exit 0
                        fi
                      elif command -v wget >/dev/null 2>&1; then
                        if wget -qO- ${APP_URL}/ > /dev/null && wget -qO- ${APP_URL}/api/summary > /dev/null; then
                          echo "Application is healthy"
                          exit 0
                        fi
                      else
                        echo "Neither curl nor wget is available for health checks"
                        exit 1
                      fi

                      echo "Waiting for app startup... attempt $i/30"
                      sleep 5
                    done

                    echo "Health check failed"
                    ${COMPOSE_CMD} ${COMPOSE_FILES} ps
                    ${COMPOSE_CMD} ${COMPOSE_FILES} logs --tail=200
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh '''#!/bin/bash
                set +e
                ${COMPOSE_CMD} ${COMPOSE_FILES} ps || true
                ${COMPOSE_CMD} ${COMPOSE_FILES} logs --tail=200 > compose-last.log || true
            '''
            archiveArtifacts artifacts: 'compose-last.log', onlyIfSuccessful: false
        }

        failure {
            echo 'Build or deployment failed. Check compose-last.log artifact for details.'
        }

        success {
            echo 'MonitorX CI/CD completed successfully with Docker deployment.'
        }
    }
}
