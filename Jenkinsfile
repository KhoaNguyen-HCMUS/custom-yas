pipeline {
    agent any

    stages {
        stage('Security Scan - Gitleaks') {
            steps {
                sh '''
                    docker run --rm \
                      -v "$PWD":/work \
                      -w /work \
                      zricethezav/gitleaks:v8.18.4 \
                      detect --source="." --config="/work/gitleaks.toml" --verbose --no-git
                '''
            }
        }

        stage('CI - Product Service') {
            when {
                changeset "product/**"
            }
            stages {
                stage('Test Product') {
                    steps {
                        dir('product') {
                            sh 'chmod +x ./mvnw'
                            sh './mvnw -f ../pom.xml clean test -pl product -am'
                        }
                    }
                    post {
                        always {
                            junit 'product/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Build Product') {
                    steps {
                        dir('product') {
                            sh 'chmod +x ./mvnw'
                            sh './mvnw -f ../pom.xml clean package -pl product -am -DskipTests'
                        }
                    }
                }
            }
        }

        stage('CI - Customer Service') {
            when {
                changeset "customer/**"
            }
            stages {
                stage('Test Customer') {
                    steps {
                        dir('customer') {
                            sh 'chmod +x ./mvnw'
                            sh './mvnw -f ../pom.xml clean test -pl customer -am'
                        }
                    }
                    post {
                        always {
                            junit 'customer/target/surefire-reports/*.xml'
                            jacoco(
                                execPattern: 'customer/target/jacoco.exec',
                                classPattern: 'customer/target/classes',
                                sourcePattern: 'customer/src/main/java',
                                inclusionPattern: '**/*.class'
                            )
                        }
                    }
                }
                stage('Build Customer') {
                    steps {
                        dir('customer') {
                            sh 'chmod +x ./mvnw'
                            sh './mvnw -f ../pom.xml clean package -pl customer -am -DskipTests'
                        }
                    }
                }
            }
        }

        stage('CI - Storefront Frontend') {
            when {
                changeset "storefront/**"
            }
            stages {
                stage('Test & Build Storefront') {
                    steps {
                        dir('storefront') {
                            sh 'npm ci'
                            sh 'npm run lint'
                            sh 'npx prettier --check .'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }
    }
}