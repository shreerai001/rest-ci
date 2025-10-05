pipeline {
	agent any

    environment {
        DOCKER_HUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKER_IMAGE = 'shreerai1/springboot-app'
        DOCKER_TAG = "${BUILD_NUMBER}"

        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonarqube-token')

        APP_NAME = 'rest-ci-app'
        TEST_PORT = '8081'
        PROD_PORT = '8082'
    }

    tools {
		maven 'Maven-3.9.6'
        jdk 'JDK-17'
    }

    stages {
		stage('1. Checkout') {
			steps {
				script {
					echo '========== Stage 1: Checking out code from Git =========='
                    checkout scm
                }
            }
        }

        stage('2. Build') {
			steps {
				script {
					echo '========== Stage 2: Building Application =========='
                    
                    // Explicitly set JAVA_HOME and PATH
                    def javaHome = tool name: 'JDK-17'
                    env.JAVA_HOME = javaHome
                    env.PATH = "${javaHome}/bin:${env.PATH}"
                    
                    sh '''
                        echo "JAVA_HOME is: $JAVA_HOME"
                        echo "Java version:"
                        java -version
                        
                        mvn clean compile
                        mvn package -DskipTests
                    '''

                    echo 'Build artifacts created successfully'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('3. Test') {
			steps {
				script {
					echo '========== Stage 3: Running Unit and Integration Tests =========='
                    sh '''
                        mvn test
                        mvn verify
                    '''
                }
            }
            post {
				always {
					junit '**/target/surefire-reports/*.xml'
                    junit '**/target/failsafe-reports/*.xml'

                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/surefire-reports',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }

        stage('4. Code Quality Analysis') {
			steps {
				script {
					echo '========== Stage 4: Running Code Quality Analysis with SonarQube =========='
                    
                    def javaHome = tool name: 'JDK-17'
                    env.JAVA_HOME = javaHome
                    env.PATH = "${javaHome}/bin:${env.PATH}"
                    
                    withSonarQubeEnv('SonarQube') {
						sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=rest-ci-app \
                                -Dsonar.projectName='REST CI Application' \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.token=${SONAR_TOKEN} \
                                -Dsonar.java.binaries=target/classes \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('5. Quality Gate') {
			steps {
				script {
					echo '========== Stage 5: Checking SonarQube Quality Gate =========='
                    timeout(time: 5, unit: 'MINUTES') {
						def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
							error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        } else {
							echo "Quality Gate passed with status: ${qg.status}"
                        }
                    }
                }
            }
        }

        stage('6. Security Scan') {
			steps {
				script {
					echo '========== Stage 6: Running Security Analysis =========='

                    def javaHome = tool name: 'JDK-17'
                    env.JAVA_HOME = javaHome
                    env.PATH = "${javaHome}/bin:${env.PATH}"

                    echo 'Running OWASP Dependency Check...'
                    sh '''
                        ./mvnw org.owasp:dependency-check-maven:check \
                            -DfailBuildOnCVSS=7 \
                            -DsuppressionFiles=dependency-check-suppressions.xml || true
                    '''

                    echo 'Running Trivy security scan...'
                    sh '''
                        # Scan filesystem
                        if command -v trivy &> /dev/null; then
                            trivy fs --severity HIGH,CRITICAL --format json --output trivy-report.json . || true
                            trivy fs --severity HIGH,CRITICAL . || true
                        else
                            echo "Trivy not installed - skipping scan"
                        fi
                    '''

                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target',
                        reportFiles: 'dependency-check-report.html',
                        reportName: 'OWASP Dependency Check Report'
                    ])
                }
            }
            post {
				always {
					archiveArtifacts artifacts: '**/dependency-check-report.html, **/trivy-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('7. Build Docker Image') {
			steps {
				script {
					echo '========== Stage 7: Building Docker Image =========='
                    sh """
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                    """

                    echo 'Scanning Docker image with Trivy...'
                    sh """
                        trivy image --severity HIGH,CRITICAL ${DOCKER_IMAGE}:${DOCKER_TAG} || true
                    """
                }
            }
        }

        stage('8. Push to Registry') {
			steps {
				script {
					echo '========== Stage 8: Pushing Docker Image to Registry =========='
                    sh '''
                        echo $DOCKER_HUB_CREDENTIALS_PSW | docker login -u $DOCKER_HUB_CREDENTIALS_USR --password-stdin
                    '''
                    sh """
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                    """
                }
            }
            post {
				always {
					sh 'docker logout'
                }
            }
        }

        stage('9. Deploy to Test Environment') {
			steps {
				script {
					echo '========== Stage 9: Deploying to Test/Staging Environment =========='

                    // Stop and remove existing test container
                    sh """
                        docker stop ${APP_NAME}-test || true
                        docker rm ${APP_NAME}-test || true
                    """

                    sh """
                        docker run -d \
                            --name ${APP_NAME}-test \
                            -p ${TEST_PORT}:8080 \
                            -e SPRING_PROFILES_ACTIVE=test \
                            -e SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb \
                            ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """

                    echo 'Waiting for application to start...'
                    sleep(time: 30, unit: 'SECONDS')

                    sh """
                        curl -f http://localhost:${TEST_PORT}/actuator/health || exit 1
                    """

                    echo 'Application deployed successfully to test environment'
                }
            }
        }

        stage('10. Integration Tests on Test Environment') {
			steps {
				script {
					echo '========== Stage 10: Running Integration Tests =========='

                    // Run API tests using curl or newman (Postman CLI)
                    sh """
                        # Test GET endpoint
                        curl -f http://localhost:${TEST_PORT}/api/students || exit 1

                        # Add more API tests as needed
                    """

                    echo 'Integration tests passed'
                }
            }
        }

        stage('11. Approval for Production') {
			steps {
				script {
					echo '========== Stage 11: Waiting for Manual Approval =========='

                    timeout(time: 1, unit: 'HOURS') {
						input message: 'Deploy to Production?',
                              ok: 'Deploy',
                              submitter: 'admin,deployer'
                    }
                }
            }
        }

        stage('12. Release to Production') {
			steps {
				script {
					echo '========== Stage 12: Deploying to Production =========='

                    sh """
                        git tag -a v1.0.${BUILD_NUMBER} -m "Release version 1.0.${BUILD_NUMBER}"
                        git push origin v1.0.${BUILD_NUMBER} || true
                    """

                    sh """
                        # Check if production container exists
                        if docker ps -a | grep -q ${APP_NAME}-prod; then
                            # Backup current production
                            docker rename ${APP_NAME}-prod ${APP_NAME}-prod-backup || true
                        fi

                        # Deploy new production version
                        docker run -d \
                            --name ${APP_NAME}-prod \
                            -p ${PROD_PORT}:8080 \
                            -e SPRING_PROFILES_ACTIVE=prod \
                            -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/proddb \
                            --restart unless-stopped \
                            ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """

                    sleep(time: 30, unit: 'SECONDS')
                    sh """
                        curl -f http://localhost:${PROD_PORT}/actuator/health || exit 1
                    """

                    sh """
                        docker stop ${APP_NAME}-prod-backup || true
                        docker rm ${APP_NAME}-prod-backup || true
                    """

                    echo 'Production deployment successful'
                }
            }
        }

        stage('13. Monitoring & Alerting') {
			steps {
				script {
					echo '========== Stage 13: Setting Up Monitoring =========='

                    sh """
                        # Verify Prometheus can scrape metrics
                        curl -f http://localhost:${PROD_PORT}/actuator/prometheus || true
                    """

                    echo """
                        Deployment Summary:
                        - Build Number: ${BUILD_NUMBER}
                        - Docker Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                        - Test Environment: http://localhost:${TEST_PORT}
                        - Production Environment: http://localhost:${PROD_PORT}
                        - Monitoring Dashboard: http://localhost:3000
                    """

                }
            }
        }
    }

    post {
		always {
			script {
				echo '========== Pipeline Execution Completed =========='
				
				// Archive important files if they exist
				try {
					archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true, fingerprint: true
				} catch (Exception e) {
					echo "Artifact archiving skipped: ${e.message}"
				}
				
				// Clean Docker images to save space
				sh '''
					echo "Cleaning up Docker resources..."
					docker image prune -f || echo "Docker cleanup skipped"
				'''
			}
        }

        success {
			script {
				echo '========== ✅ Pipeline SUCCESS =========='
				echo """
					Build Summary:
					- Build Number: ${BUILD_NUMBER}
					- Build Time: ${new Date()}
					- Status: SUCCESS
				"""
			}
        }

        failure {
			script {
				echo '========== ❌ Pipeline FAILED =========='
				echo """
					Build Failed:
					- Build Number: ${BUILD_NUMBER}
					- Console Output: ${BUILD_URL}console
					- Please check logs for errors
				"""
			}
        }

        unstable {
			script {
				echo '========== ⚠️ Pipeline UNSTABLE =========='
				echo "Some tests may have failed. Check test reports."
			}
        }
    }
}