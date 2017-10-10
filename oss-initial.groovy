node{
  def host_ip = '192.168.99.102';
  def image_name = 'hz-oss-docker'
  def hz_img
  def hz_container

  stage('build'){
    git changelog: false, poll: false, url: 'https://github.com/hazelcast/hazelcast-docker.git'
    dir('hazelcast-oss') {
      hz_img = docker.build("${image_name}:${env.BUILD_ID}")
    }
  }

  stage('run'){
    hz_img.withRun{ container ->
      dir('/var/jenkins_home/jobs/hz/'){
        sh "./wait.sh ${host_ip}:5701 -t 10"
        sh "docker logs ${container.id} --tail 1 2>&1 | grep STARTED"
      }
    }
  }

  stage('liveliness'){
    hz_img.withRun("-p 5701:5701 -e JAVA_OPTS=\"-Dhazelcast.rest.enabled=true\""){container ->
      dir('/var/jenkins_home/jobs/hz/'){
        sh "./wait.sh ${host_ip}:5701 -t 10"
        sh "./liveliness.sh ${host_ip}"
      }
    }
  }

  stage('clean-up'){
    cleanWs deleteDirs: true
  }
}
