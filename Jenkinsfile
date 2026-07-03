pipeline {
    agent any

    environment {
        TOMCAT_WEBAPPS_WINDOWS = 'C:\\apache-tomcat-11.0.11\\webapps'
        TOMCAT_WEBAPPS_UNIX    = '/opt/homebrew/opt/tomcat/libexec/webapps'
        JAVA_HOME_WINDOWS      = 'C:\\Program Files\\Java\\jdk-21.0.10'
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

                            echo "JAVA_HOME=$JAVA_HOME"
                            java -version
                            ./gradlew -version

                            chmod +x gradlew
                            ./gradlew clean build -x test
                        '''

                    } else {

                        bat '''
                            @echo off

                            set "JAVA_HOME=%JAVA_HOME_WINDOWS%"
                            set "PATH=%JAVA_HOME%\\bin;%PATH%"

                            echo ==============================
                            echo JAVA_HOME=%JAVA_HOME%
                            where java
                            java -version
                            call gradlew -version
                            echo ==============================

                            call gradlew clean build -x test

                            if errorlevel 1 exit /b %errorlevel%
                        '''
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
                            @echo off

                            echo Procurando arquivo WAR...

                            if not exist "%WORKSPACE%\\build\\libs" (
                                echo Pasta build\\libs nao encontrada.
                                exit /b 1
                            )

                            dir "%WORKSPACE%\\build\\libs"

                            for %%F in ("%WORKSPACE%\\build\\libs\\*.war") do (
                                echo WAR encontrado: %%~nxF

                                copy /Y "%%~fF" "%TOMCAT_WEBAPPS_WINDOWS%\\%%~nxF"

                                if errorlevel 1 exit /b 1

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