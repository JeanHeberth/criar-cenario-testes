pipeline {
    agent any

    environment {
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

                            chmod +x gradlew
                            ./gradlew clean build -x test
                        '''
                    } else {
                        bat 'gradlew clean build -x test'
                    }
                }
            }
        }

        stage('Deploy WAR to Tomcat') {

            when {
                expression {
                    def gitBranch = (env.GIT_BRANCH ?: '').toLowerCase()
                    return gitBranch == 'origin/main' ||
                           gitBranch == 'origin/master'
                }
            }

            steps {
                script {

                    if (isUnix()) {

                        sh '''
                            export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

                            echo "Procurando WAR..."

                            WAR_FILE=$(find build/libs -name "*.war" | head -n 1)

                            if [ -z "$WAR_FILE" ]; then
                                echo "ERRO: Nenhum WAR encontrado."
                                exit 1
                            fi

                            WAR_NAME=$(basename "$WAR_FILE")

                            echo "WAR encontrado: $WAR_NAME"

                            mkdir -p "$TOMCAT_WEBAPPS_UNIX"

                            cp -f "$WAR_FILE" "$TOMCAT_WEBAPPS_UNIX/$WAR_NAME"

                            echo "Deploy realizado com sucesso."
                        '''

                    } else {

                        bat '''
                            echo Procurando arquivo WAR...

                            for %%F in ("%WORKSPACE%\\build\\libs\\*.war") do (

                                echo WAR encontrado: %%~nxF

                                copy /Y "%%~fF" "%TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF"

                                echo Deploy concluido.

                                exit /b 0
                            )

                            echo ERRO: Nenhum WAR encontrado.
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
            echo 'Build e Deploy executados com sucesso!'
        }

        failure {
            echo 'Falha detectada no pipeline.'
        }
    }
}