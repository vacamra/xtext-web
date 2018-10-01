pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr:'15'))
  }

  tools { 
    maven 'apache-maven-latest'
    jdk 'oracle-jdk8-latest'
  }
  
  // https://jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers {
    pollSCM('H/5 * * * *')
  }
  
  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Gradle Build') {
      steps {
        sh '''
          ./gradlew \
            clean build createLocalMavenRepo \
            -PuseJenkinsSnapshots=true \
            -PJENKINS_URL=$JENKINS_URL \
            -PignoreTestFailures=true \
            --refresh-dependencies \
            --continue
        '''
        step([$class: 'JUnitResultArchiver', testResults: '**/build/test-results/test/*.xml'])
      }
    }
  }

  post {
    success {
      archiveArtifacts artifacts: 'build/maven-repository/**'
    }
  }
}