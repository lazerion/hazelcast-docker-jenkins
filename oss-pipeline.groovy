pipeline {
    agent {
        label "docker"
    }

    parameters {
        string(name: 'HOST', defaultValue: 'localhost', description: 'Should be changed when agent have docker')
        string(name: 'NAME', defaultValue: 'hz-oss-docker', description: 'Image name')
        string(name: 'SLEEP', defaultValue: '10', description: 'Wait time for Hazelcast STARTED')
    }

    options {
        timestamps()
    }

    stages {

        stage('Build') {
            steps {
                git changelog: false, poll: false, url: 'https://github.com/hazelcast/hazelcast-docker.git'
                dir('hazelcast-oss') {
                    script {
                        oss = docker.build("${params.NAME}:${env.BUILD_ID}")
                    }
                }
            }
        }

        stage('Run') {
            steps {
                script {
                    oss.withRun("-p 5701:5701") { container ->
                        sleep params.SLEEP as Integer
                        sh "docker logs ${container.id} --tail 1 2>&1 | grep STARTED"
                    }
                }
            }
        }

        stage('Liveliness') {
            steps {
                script {
                    oss.withRun("-p 5701:5701 -e JAVA_OPTS=\"-Dhazelcast.rest.enabled=true\"") { container ->
                        git changelog: false, poll: false, url: 'https://github.com/lazerion/hazelcast-docker-jenkins.git'
                        sh "chmod 777 wait.sh liveliness.sh"
                        sh "./wait.sh ${params.HOST}:5701 -t 10"
                        sh "./liveliness.sh ${params.HOST}"
                    }
                }
            }
        }

        stage('Acceptance') {
            steps {
                script {
                    oss.withRun("-p 5701:5701") { container ->
                        sh "./wait.sh ${params.HOST}:5701 -t 10"
                        sh "mvn test"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs deleteDirs: true
            script {
                sh "docker rmi ${oss.id}"
            }
        }
        failure {
            mail to: 'baris@hazelcast.com',
                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
