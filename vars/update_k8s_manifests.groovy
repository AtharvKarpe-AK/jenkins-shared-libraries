#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """
        
        // Update deployment manifests with new image tags - using proper Linux sed syntax
        sh """
            # Update main application deployment - note the correct image name is atharvkarpe/easyshop-app
            sed -i "s|image: atharvkarpe/easyshop-app:.*|image: atharvkarpe/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: atharvkarpe/easyshop-app-migration:.*|image: atharvkarpe/easyshop-app-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # Ensure ingress is using the correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: "correct_name_after_installing_EKS_ingress_controller"|g" ${manifestsPath}/10-ingress.yaml
            fi

            # replace the correct IP in configmap and ingress file
            sed -i "s|NEXT_PUBLIC_API_URL: .*|NEXT_PUBLIC_API_URL: "http://3.249.24.205.nip.io:30000/api"|g" ${manifestsPath}/04-configmap.yaml
            sed -i "s|NEXTAUTH_URL: .*|NEXTAUTH_URL: "http://3.249.24.205.nip.io:30000"|g" ${manifestsPath}/04-configmap.yaml
            
            
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit and push changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                # Set up credentials for push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/AtharvKarpe-AK/tws-e-commerce-app.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
