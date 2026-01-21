def call(Map config = [:]) {

def imageName = config.imageName ?: error("Images is required")
def imageTag = config.imageTag ?: 'latest'
def dockerfile = config.dockerfile ?: 'Dockerfile'
def context = config.context ?: '.'

echo "Building docker image: ${imageName}:${imageTag} using ${dockerfile}"

sh """

docker build -t ${imageName}:${imageTag} -t ${imageName}:latest -f ${dockerfile} ${context}

"""

}
