pipeline {
    agent any

    environment {
        CODECOV_TOKEN = credentials('CODECOV_TOKEN_GASTOS_MENSAIS')
        GITHUB_TOKEN  = credentials('GITHUB_TOKEN')

        TOMCAT_WEBAPPS_WINDOWS = 'C:\\apache-tomcat-11.0.11\\webapps'
        TOMCAT_WEBAPPS_UNIX    = '/opt/homebrew/opt/tomcat/libexec/webapps'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Clonando repositório...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export JAVA_HOME=$(/usr/libexec/java_home -v 21)
                            export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

                            chmod +x ./gradlew
                            ./gradlew clean build -x test
                        '''
                    } else {
                        bat 'gradlew clean build -x test'
                    }
                }
            }
        }

        stage('Unit Tests - Service') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export JAVA_HOME=$(/usr/libexec/java_home -v 21)
                            export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

                            chmod +x ./gradlew
                            ./gradlew test --tests "br.com.gastosmensais.service.*"
                        '''
                    } else {
                        bat 'gradlew test --tests "br.com.gastosmensais.service.*"'
                    }
                }
            }
            post {
                always {
                    junit '**/build/test-results/test/TEST-*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export JAVA_HOME=$(/usr/libexec/java_home -v 21)
                            export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

                            chmod +x ./gradlew
                            ./gradlew test --tests "br.com.gastosmensais.controller.*"
                        '''
                    } else {
                        bat 'gradlew test --tests "br.com.gastosmensais.controller.*"'
                    }
                }
            }
            post {
                always {
                    junit '**/build/test-results/test/TEST-*.xml'
                }
            }
        }

        stage('Reports & Coverage') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export JAVA_HOME=$(/usr/libexec/java_home -v 21)
                            export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

                            chmod +x ./gradlew
                            ./gradlew jacocoTestReport -x jacocoTestCoverageVerification
                        '''
                    } else {
                        bat 'gradlew jacocoTestReport -x jacocoTestCoverageVerification'
                    }
                }
            }
            post {
                always {
                    junit '**/build/test-results/test/TEST-*.xml'
                    publishHTML(target: [
                        reportDir: 'build/reports/jacoco/test/html',
                        reportFiles: 'index.html',
                        reportName: 'Jacoco Coverage Report'
                    ])
                }
            }
        }

        stage('Upload Coverage to Codecov') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

                            echo "Baixando Codecov para Mac/Linux..."
                            curl -Os https://uploader.codecov.io/latest/linux/codecov

                            chmod +x codecov

                            echo "Enviando cobertura..."
                            ./codecov -t "$CODECOV_TOKEN" -f build/reports/jacoco/test/jacocoTestReport.xml
                        '''
                    } else {
                        bat '''
                            echo Baixando Codecov para Windows...
                            curl -L -o codecov.exe https://uploader.codecov.io/latest/windows/codecov.exe

                            echo Enviando cobertura...
                            codecov.exe -t %CODECOV_TOKEN% -f build\\reports\\jacoco\\test\\jacocoTestReport.xml
                        '''
                    }
                }
            }
        }

        stage('Deploy WAR to Tomcat') {
            when {
                expression {
                    def gitBranch = (env.GIT_BRANCH ?: '').toLowerCase()
                    return gitBranch == 'origin/main' || gitBranch == 'origin/master'
                }
            }

            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

                            echo "Procurando arquivo WAR em $WORKSPACE/build/libs"

                            WAR_FILE=$(find "$WORKSPACE/build/libs" -name "*.war" | head -n 1)

                            if [ -z "$WAR_FILE" ]; then
                                echo "ERRO: Nenhum WAR encontrado em $WORKSPACE/build/libs"
                                exit 1
                            fi

                            WAR_NAME=$(basename "$WAR_FILE")

                            echo "WAR encontrado: $WAR_NAME"

                            mkdir -p "$TOMCAT_WEBAPPS_UNIX"

                            echo "Copiando para Tomcat..."
                            cp -f "$WAR_FILE" "$TOMCAT_WEBAPPS_UNIX/$WAR_NAME"

                            echo "Deploy concluído em $TOMCAT_WEBAPPS_UNIX/$WAR_NAME"
                        '''
                    } else {
                        bat '''
                            echo Procurando arquivo WAR em %WORKSPACE%\\build\\libs

                            for %%F in ("%WORKSPACE%\\build\\libs\\*.war") do (
                                echo WAR encontrado: %%~nxF

                                echo Copiando para Tomcat...
                                copy /Y "%%~fF" "%TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF"

                                echo Deploy concluido em %TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF
                                exit /b 0
                            )

                            echo ERRO: Nenhum WAR encontrado em %WORKSPACE%\\build\\libs
                            exit /b 1
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline concluído.'
        }

        success {
            echo 'Todos os stages executados com sucesso!'
        }

        failure {
            echo 'Falha detectada no pipeline. Verifique os logs.'
        }
    }
}